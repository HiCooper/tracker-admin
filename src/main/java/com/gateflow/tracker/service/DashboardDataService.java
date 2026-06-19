package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.DashboardVO;
import com.gateflow.tracker.domain.dto.dashboard.DashboardDataDto.DashboardDataResult;
import com.gateflow.tracker.domain.dto.dashboard.DashboardDataDto.PanelData;
import com.gateflow.tracker.domain.dto.platform.PlatformDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板取数引擎:解析看板 configJson 中的 widget 配置,逐个从 ClickHouse 计算真实数据。
 * 复用 PlatformDataService 的渠道/页面/核心指标;支持 stat/line/bar/pie/table 五类组件。
 */
@Slf4j
@Service
public class DashboardDataService {

    private static final String EVENTS = "gateflow_tracker.events";
    private static final String SESSIONS = "gateflow_tracker.sessions";
    private static final String UK = "if(user_id != '', user_id, anonymous_id)";

    private final DashboardService dashboardService;
    private final PlatformDataService platformData;
    private final ObjectMapper objectMapper;
    private final DataSource ch;

    public DashboardDataService(DashboardService dashboardService, PlatformDataService platformData,
                                ObjectMapper objectMapper, ClickHouseProperties chProps) {
        this.dashboardService = dashboardService;
        this.platformData = platformData;
        this.objectMapper = objectMapper;
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource for dashboard data: {}", e.getMessage());
            ds = null;
        }
        this.ch = ds;
    }

    public DashboardDataResult data(Long id, String start, String end) {
        DashboardVO dash = dashboardService.getDashboardById(id);
        String s = StringUtils.hasText(start) ? start : java.time.LocalDate.now().minusDays(7).toString();
        String e = StringUtils.hasText(end) ? end : java.time.LocalDate.now().toString();
        List<PanelData> panels = new ArrayList<>();
        for (JsonNode w : parseWidgets(dash.getConfig())) {
            String panelId = text(w, "id", text(w, "panelId", ""));
            String type = text(w, "type", "stat");
            String title = text(w, "title", "");
            String metric = text(w, "metric", "pv");
            try {
                panels.add(new PanelData(panelId, type, title, compute(type, metric, s, e), null));
            } catch (Exception ex) {
                log.warn("panel {} compute failed: {}", panelId, ex.getMessage());
                panels.add(new PanelData(panelId, type, title, null, ex.getMessage()));
            }
        }
        return new DashboardDataResult(dash.getId(), dash.getName(), panels);
    }

    /** 解析 configJson:支持 {"widgets":[...]} / {"panels":[...]} / 顶层数组。 */
    List<JsonNode> parseWidgets(String configJson) {
        List<JsonNode> out = new ArrayList<>();
        if (!StringUtils.hasText(configJson)) return out;
        try {
            JsonNode root = objectMapper.readTree(configJson);
            JsonNode arr = root.isArray() ? root
                    : root.has("widgets") ? root.get("widgets")
                    : root.has("panels") ? root.get("panels") : null;
            if (arr != null && arr.isArray()) arr.forEach(out::add);
        } catch (Exception ex) {
            log.warn("invalid dashboard configJson: {}", ex.getMessage());
        }
        return out;
    }

    private Object compute(String type, String metric, String start, String end) {
        return switch (type) {
            case "stat" -> Map.of("metric", metric, "value", scalarMetric(metric, start, end));
            case "line" -> dailySeries(metric, start, end);
            case "bar", "pie" -> channelSeries(start, end);
            case "table" -> pageRows(start, end);
            default -> Map.of("metric", metric, "value", scalarMetric(metric, start, end));
        };
    }

    // ── 单值指标 ──
    double scalarMetric(String metric, String start, String end) {
        PlatformDto.CoreMetrics m = platformData.coreMetrics(start, end);
        return switch (metric) {
            case "uv" -> m.getUv();
            case "pv" -> m.getPv();
            case "new_users" -> m.getNewUsers();
            case "avg_duration" -> m.getAvgDuration();
            case "bounce_rate" -> m.getBounceRate();
            case "conversion_rate" -> m.getConversionRate();
            case "pay_users" -> scalar("SELECT uniqExactIf(" + UK + ", event_type='purchase') FROM " + EVENTS + " WHERE " + tf(start, end));
            case "shares" -> scalar("SELECT countIf(event_type='share') FROM " + EVENTS + " WHERE " + tf(start, end));
            default -> m.getPv();
        };
    }

    // ── 按天序列 ──
    List<Map<String, Object>> dailySeries(String metric, String start, String end) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (ch == null) return out;
        String sql;
        if ("avg_duration".equals(metric) || "bounce_rate".equals(metric)) {
            String expr = "avg_duration".equals(metric) ? "avg(duration)/1000" : "if(count()=0,0,sum(is_bounce)/count()*100)";
            sql = "SELECT toDate(start_time) d, " + expr + " v FROM " + SESSIONS + " WHERE " + tf2("start_time", start, end) +
                    " GROUP BY d ORDER BY d";
        } else {
            sql = "SELECT toDate(timestamp) d, " + dailyExpr(metric) + " v FROM " + EVENTS + " WHERE " + tf(start, end) +
                    " GROUP BY d ORDER BY d";
        }
        forEachRow(sql, rs -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", rs.getString("d"));
            row.put("value", round1(rs.getDouble("v")));
            out.add(row);
        });
        return out;
    }

    private static String dailyExpr(String metric) {
        return switch (metric) {
            case "uv", "new_users" -> "uniqExact(" + UK + ")";
            case "pay_users" -> "uniqExactIf(" + UK + ", event_type='purchase')";
            case "shares" -> "countIf(event_type='share')";
            case "conversion_rate" -> "if(uniqExact(" + UK + ")=0,0, uniqExactIf(" + UK + ", event_type='purchase')/uniqExact(" + UK + ")*100)";
            default -> "countIf(event_type='page_view')"; // pv
        };
    }

    private List<Map<String, Object>> channelSeries(String start, String end) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PlatformDto.ChannelBreakdown c : platformData.channels(start, end)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", c.getName());
            row.put("value", c.getUv());
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> pageRows(String start, String end) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PlatformDto.PageBreakdown p : platformData.pages(start, end)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("page", p.getName());
            row.put("pv", p.getPv());
            out.add(row);
        }
        return out;
    }

    // ── helpers ──
    private static String tf(String start, String end) {
        return tf2("timestamp", start, end);
    }

    private static String tf2(String col, String start, String end) {
        return "toDate(" + col + ") >= toDate('" + AdvancedAnalysisService.toChTime(start) + "') AND toDate(" + col +
                ") <= toDate('" + AdvancedAnalysisService.toChTime(end) + "')";
    }

    private static String text(JsonNode n, String field, String def) {
        JsonNode v = n.get(field);
        return v != null && !v.isNull() ? v.asText() : def;
    }

    static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private double scalar(String sql) {
        double[] v = {0};
        forEachRow(sql, rs -> v[0] = rs.getDouble(1));
        return v[0];
    }

    @FunctionalInterface private interface RowConsumer { void accept(ResultSet rs) throws Exception; }

    private void forEachRow(String sql, RowConsumer consumer) {
        if (ch == null) return;
        try (Connection conn = ch.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) consumer.accept(rs);
        } catch (Exception e) {
            log.error("Dashboard data query failed: {} | {}", e.getMessage(), sql);
        }
    }
}
