package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.experience.ExperienceDto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 体验分析:热力图 / 用户画像 / 页面 / 会话 / 转化 / 报告,直查 ClickHouse。
 * 用户口径采用身份缝合 userKey。率为百分数,时长格式化为可读字符串。
 */
@Slf4j
@Service
public class ExperienceService {

    private static final String EVENTS = "gateflow_tracker.events";
    private static final String SESSIONS = "gateflow_tracker.sessions";
    private static final String UK = "if(user_id != '', user_id, anonymous_id)";

    private final DataSource ch;

    public ExperienceService(ClickHouseProperties chProps) {
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource for experience: {}", e.getMessage());
            ds = null;
        }
        this.ch = ds;
    }

    private static String tf(String col, String start, String end) {
        return "toDate(" + col + ") >= toDate('" + AdvancedAnalysisService.toChTime(start) + "') AND toDate("
                + col + ") <= toDate('" + AdvancedAnalysisService.toChTime(end) + "')";
    }

    private static String app(String appCode) {
        return StringUtils.hasText(appCode) ? " AND app_code = '" + AdvancedAnalysisService.san(appCode) + "'" : "";
    }

    // ── heatmap ──
    public HeatmapData heatmap(String appCode, String pageUrl, String start, String end, String type, Integer bucketSize) {
        int b = bucketSize != null && bucketSize > 0 ? bucketSize : 20;
        String et = switch (type == null ? "click" : type) {
            case "exposure" -> "exposure";
            case "scroll" -> "scroll";
            default -> "click";
        };
        HeatmapData data = new HeatmapData(pageUrl, 0, 0, 0, new ArrayList<>());
        if (ch == null) return data;
        String filter = tf("timestamp", start, end) + app(appCode) +
                " AND page_url = '" + AdvancedAnalysisService.san(pageUrl) + "' AND event_type = '" + et + "'";
        forEachRow("SELECT max(screen_width) w, max(screen_height) h, count() c FROM " + EVENTS + " WHERE " + filter, rs -> {
            data.setViewportWidth(rs.getInt("w"));
            data.setViewportHeight(rs.getInt("h"));
            data.setTotalClicks(rs.getLong("c"));
        });
        forEachRow("SELECT toInt32(floor(click_x/" + b + "))*" + b + " x, toInt32(floor(click_y/" + b + "))*" + b + " y, " +
                "count() c FROM " + EVENTS + " WHERE " + filter + " AND isNotNull(click_x) AND isNotNull(click_y) " +
                "GROUP BY x, y ORDER BY c DESC LIMIT 2000",
                rs -> data.getPoints().add(new HeatmapPoint(rs.getInt("x"), rs.getInt("y"), rs.getLong("c"))));
        return data;
    }

    // ── user portrait ──
    public UserPortrait portrait(String appCode, String start, String end) {
        UserPortrait p = new UserPortrait(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), new ArrayList<>());
        if (ch == null) return p;
        String filter = tf("timestamp", start, end) + app(appCode);
        long total = scalar("SELECT count() FROM " + EVENTS + " WHERE " + filter);
        p.setDeviceType(dim(filter, "device_type", total));
        p.setOs(dim(filter, "os", total));
        p.setBrowser(dim(filter, "browser", total));
        p.setLanguage(dim(filter, "language", total));
        p.setScreenResolution(dimExpr(filter + " AND screen_width > 0",
                "concat(toString(screen_width),'x',toString(screen_height))", total));
        p.setSource(dimExpr(filter, "if(utm_source='','direct',utm_source)", total));
        List<ActiveHour> hours = new ArrayList<>();
        forEachRow("SELECT toHour(timestamp) h, toDayOfWeek(timestamp) dw, count() c FROM " + EVENTS +
                " WHERE " + filter + " GROUP BY h, dw",
                rs -> hours.add(new ActiveHour(rs.getInt("h"), rs.getInt("dw"), rs.getLong("c"))));
        p.setActiveHours(hours);
        return p;
    }

    private List<PortraitDimension> dim(String filter, String col, long total) {
        return dimExpr(filter + " AND " + col + " != ''", col, total);
    }

    private List<PortraitDimension> dimExpr(String filter, String expr, long total) {
        List<PortraitDimension> out = new ArrayList<>();
        forEachRow("SELECT " + expr + " v, count() c FROM " + EVENTS + " WHERE " + filter +
                " GROUP BY v ORDER BY c DESC LIMIT 12", rs -> {
            String v = rs.getString("v");
            long c = rs.getLong("c");
            out.add(new PortraitDimension(v, v, c, total > 0 ? PlatformDataService.pct((double) c / total) : 0));
        });
        return out;
    }

    // ── pages ──
    public List<PageListItem> pages(String appCode, String start, String end) {
        List<PageListItem> out = new ArrayList<>();
        forEachRow("SELECT page_url, any(page_title) t, count() pv FROM " + EVENTS + " WHERE " +
                tf("timestamp", start, end) + app(appCode) + " AND event_type='page_view' AND page_url!='' " +
                "GROUP BY page_url ORDER BY pv DESC LIMIT 50",
                rs -> out.add(new PageListItem(rs.getString("page_url"), rs.getString("t"), rs.getLong("pv"))));
        return out;
    }

    // ── sessions ──
    public List<SessionRecord> sessions(String start, String end) {
        List<SessionRecord> out = new ArrayList<>();
        forEachRow("SELECT session_id, user_id, device_type, os, page_views, duration, toString(start_time) ts " +
                "FROM " + SESSIONS + " WHERE " + tf("start_time", start, end) + " ORDER BY start_time DESC LIMIT 100",
                rs -> out.add(new SessionRecord(
                        rs.getString("session_id"),
                        StringUtils.hasText(rs.getString("user_id")) ? rs.getString("user_id") : "匿名",
                        rs.getString("device_type"), rs.getString("os"),
                        rs.getLong("page_views"), formatDur(rs.getLong("duration")), rs.getString("ts"))));
        return out;
    }

    // ── conversion (goal funnel) ──
    public List<ConversionStep> conversion(String start, String end, String goal) {
        String g = StringUtils.hasText(goal) ? AdvancedAnalysisService.san(goal) : "purchase";
        long[] total = {0}, conv = {0};
        forEachRow("SELECT uniqExact(" + UK + ") t, uniqExactIf(" + UK + ", event_type='" + g + "') c FROM " + EVENTS +
                " WHERE " + tf("timestamp", start, end), rs -> {
            total[0] = rs.getLong("t");
            conv[0] = rs.getLong("c");
        });
        List<ConversionStep> steps = new ArrayList<>();
        steps.add(new ConversionStep("总访客", total[0], 100));
        steps.add(new ConversionStep(g, conv[0], total[0] > 0 ? PlatformDataService.pct((double) conv[0] / total[0]) : 0));
        return steps;
    }

    // ── reports (报告生成为独立能力,暂返回空列表避免前端 404)──
    public List<AnalysisReport> reports() {
        return List.of();
    }

    // ── helpers ──
    /** 毫秒时长格式化为可读字符串(纯函数)。 */
    static String formatDur(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000;
        long m = s / 60;
        long sec = s % 60;
        return m > 0 ? m + "m " + sec + "s" : sec + "s";
    }

    private long scalar(String sql) {
        long[] v = {0};
        forEachRow(sql, rs -> v[0] = rs.getLong(1));
        return v[0];
    }

    @FunctionalInterface
    private interface RowConsumer { void accept(ResultSet rs) throws Exception; }

    private void forEachRow(String sql, RowConsumer consumer) {
        if (ch == null) return;
        try (Connection conn = ch.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) consumer.accept(rs);
        } catch (Exception e) {
            log.error("Experience query failed: {} | {}", e.getMessage(), sql);
        }
    }
}
