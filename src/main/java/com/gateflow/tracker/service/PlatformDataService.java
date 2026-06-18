package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.platform.PlatformDto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台数据 overview:核心指标 / 渠道 / 页面 / 实时 / 分析 / 留存 / 异常,全部直查 ClickHouse(全局口径)。
 * 用户口径采用身份缝合后的 userKey。率字段单位见各方法说明。CH 不可达时返回空/0。
 */
@Slf4j
@Service
public class PlatformDataService {

    private static final String EVENTS = "gateflow_tracker.events";
    private static final String SESSIONS = "gateflow_tracker.sessions";
    private static final String UK = "if(user_id != '', user_id, anonymous_id)";

    private final DataSource ch;

    public PlatformDataService(ClickHouseProperties chProps) {
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource for platform data: {}", e.getMessage());
            ds = null;
        }
        this.ch = ds;
    }

    // 事件/会话的日期范围过滤(包含端点)
    private static String tf(String col, String start, String end) {
        return "toDate(" + col + ") >= toDate('" + AdvancedAnalysisService.toChTime(start) + "') AND toDate("
                + col + ") <= toDate('" + AdvancedAnalysisService.toChTime(end) + "')";
    }

    // ── core metrics ──（率为百分数,时长为秒）
    public CoreMetrics coreMetrics(String start, String end) {
        CoreMetrics m = new CoreMetrics(0, 0, 0, 0, 0, 0, 0, 0);
        if (ch == null) return m;
        String ef = tf("timestamp", start, end);
        forEachRow("SELECT uniqExact(" + UK + ") uv, uniqExactIf(session_id, session_id!='') ses, " +
                "countIf(event_type='page_view') pv FROM " + EVENTS + " WHERE " + ef, rs -> {
            m.setUv(rs.getLong("uv"));
            m.setSessions(rs.getLong("ses"));
            m.setPv(rs.getLong("pv"));
        });
        m.setNewUsers(newUsers(start, end));
        forEachRow("SELECT avg(duration)/1000 ad, avg(page_views) dep, " +
                "if(count()=0,0,sum(is_bounce)/count()) bo FROM " + SESSIONS + " WHERE " + tf("start_time", start, end), rs -> {
            m.setAvgDuration(round1(rs.getDouble("ad")));
            m.setAvgDepth(round1(rs.getDouble("dep")));
            m.setBounceRate(pct(rs.getDouble("bo")));
        });
        forEachRow("SELECT if(uniqExact(session_id)=0,0, uniqExactIf(session_id, event_type='purchase')/uniqExact(session_id)) cv " +
                "FROM " + EVENTS + " WHERE " + ef + " AND session_id!=''", rs -> m.setConversionRate(pct(rs.getDouble("cv"))));
        return m;
    }

    private long newUsers(String start, String end) {
        long[] n = {0};
        forEachRow("SELECT uniqExact(uk) c FROM (SELECT " + UK + " uk, min(timestamp) fs FROM " + EVENTS +
                " GROUP BY uk HAVING toDate(fs) >= toDate('" + AdvancedAnalysisService.toChTime(start) +
                "') AND toDate(fs) <= toDate('" + AdvancedAnalysisService.toChTime(end) + "'))",
                rs -> n[0] = rs.getLong("c"));
        return n[0];
    }

    // ── channels (top by uv) ──
    public List<ChannelBreakdown> channels(String start, String end) {
        List<ChannelBreakdown> out = new ArrayList<>();
        forEachRow("SELECT if(utm_source='','direct',utm_source) name, uniqExact(" + UK + ") uv FROM " + EVENTS +
                " WHERE " + tf("timestamp", start, end) + " GROUP BY name ORDER BY uv DESC LIMIT 10",
                rs -> out.add(new ChannelBreakdown(rs.getString("name"), rs.getLong("uv"), 0)));
        for (int i = 0; i < out.size(); i++) out.get(i).setRank(i + 1);
        return out;
    }

    // ── pages (top by pv) ──
    public List<PageBreakdown> pages(String start, String end) {
        List<PageBreakdown> out = new ArrayList<>();
        forEachRow("SELECT page_url name, count() pv FROM " + EVENTS + " WHERE " + tf("timestamp", start, end) +
                " AND event_type='page_view' AND page_url!='' GROUP BY name ORDER BY pv DESC LIMIT 10",
                rs -> out.add(new PageBreakdown(rs.getString("name"), rs.getLong("pv"), 0)));
        for (int i = 0; i < out.size(); i++) out.get(i).setRank(i + 1);
        return out;
    }

    // ── realtime ──
    public RealtimeSnapshot realtime() {
        RealtimeSnapshot r = new RealtimeSnapshot(0, 0, 0, 0, new ArrayList<>());
        if (ch == null) return r;
        forEachRow("SELECT uniqExact(" + UK + ") c FROM " + EVENTS + " WHERE timestamp >= now() - INTERVAL 5 MINUTE",
                rs -> r.setOnline(rs.getLong("c")));
        forEachRow("SELECT uniqExact(" + UK + ") uv, uniqExactIf(session_id, session_id!='') ses FROM " + EVENTS +
                " WHERE timestamp >= toStartOfDay(now())", rs -> {
            r.setTodayUv(rs.getLong("uv"));
            r.setTodaySessions(rs.getLong("ses"));
        });
        forEachRow("SELECT uniqExact(uk) c FROM (SELECT " + UK + " uk, min(timestamp) fs FROM " + EVENTS +
                " GROUP BY uk HAVING fs >= toStartOfDay(now()))", rs -> r.setTodayNewUsers(rs.getLong("c")));
        List<TopPage> tops = new ArrayList<>();
        forEachRow("SELECT page_url name, count() c FROM " + EVENTS + " WHERE timestamp >= now() - INTERVAL 1 HOUR " +
                "AND event_type='page_view' AND page_url!='' GROUP BY name ORDER BY c DESC LIMIT 10",
                rs -> tops.add(new TopPage(rs.getString("name"), rs.getLong("c"))));
        r.setTopPages(tops);
        return r;
    }

    // ── analysis overview ──
    public AnalysisOverview analysis(String start, String end) {
        AnalysisOverview a = new AnalysisOverview(0, 0, 0, 0, 0, 0, new ArrayList<>(), new ArrayList<>());
        if (ch == null) return a;
        forEachRow("SELECT uniqExact(" + UK + ") c FROM " + EVENTS + " WHERE timestamp >= toStartOfDay(now())",
                rs -> a.setDau(rs.getLong("c")));
        forEachRow("SELECT uniqExact(" + UK + ") c FROM " + EVENTS + " WHERE timestamp >= now() - INTERVAL 30 DAY",
                rs -> a.setMau(rs.getLong("c")));
        String ef = tf("timestamp", start, end);
        long[] uv = {0}, ses = {0};
        forEachRow("SELECT uniqExact(" + UK + ") uv, uniqExact(session_id) ses FROM " + EVENTS + " WHERE " + ef,
                rs -> { uv[0] = rs.getLong("uv"); ses[0] = rs.getLong("ses"); });
        a.setAvgSessionsPerUser(uv[0] > 0 ? round1((double) ses[0] / uv[0]) : 0);
        forEachRow("SELECT avg(duration)/1000 ad, avg(page_views) ap FROM " + SESSIONS + " WHERE " + tf("start_time", start, end),
                rs -> { a.setAvgDuration(round1(rs.getDouble("ad"))); a.setAvgPagesPerSession(round1(rs.getDouble("ap"))); });
        a.setDay7Retention(pct(retentionFraction(start, end, 7)));

        forEachRow("SELECT if(utm_source='','direct',utm_source) ch, uniqExact(user_id) uv, count() ses, " +
                "avg(duration)/1000 ad, if(count()=0,0,sum(is_bounce)/count()) bo FROM " + SESSIONS +
                " WHERE " + tf("start_time", start, end) + " GROUP BY ch ORDER BY uv DESC LIMIT 10",
                rs -> a.getChannels().add(new ChannelDetail(rs.getString("ch"), rs.getLong("uv"), 0,
                        rs.getLong("ses"), round1(rs.getDouble("ad")), pct(rs.getDouble("bo")))));
        forEachRow("SELECT page_url path, uniqExact(" + UK + ") uv, count() pv, avg(stay_duration)/1000 st FROM " + EVENTS +
                " WHERE " + ef + " AND event_type='page_view' AND page_url!='' GROUP BY path ORDER BY pv DESC LIMIT 10",
                rs -> a.getPages().add(new PageDetail(rs.getString("path"), rs.getLong("uv"), rs.getLong("pv"),
                        0, 0, 0, round1(rs.getDouble("st")))));
        return a;
    }

    // ── retention (rates as 0..1 fractions) ──
    public RetentionResult retention(String start, String end) {
        List<Integer> days = List.of(1, 3, 7, 14, 30);
        List<CohortRow> cohorts = new ArrayList<>();
        long[] totalInit = {0};
        double[] sumByDay = new double[days.size()];
        if (ch != null) {
            StringBuilder cols = new StringBuilder();
            for (int d : days) cols.append(", uniqExactIf(c.uk, a.ad = c.cd + ").append(d).append(") d").append(d);
            String sql = "SELECT cohortDate, users" + retAlias(days) + " FROM (" +
                    "WITH cohorts AS (SELECT " + UK + " uk, min(toDate(timestamp)) cd FROM " + EVENTS +
                    " WHERE " + tf("timestamp", start, end) + " GROUP BY uk), " +
                    "activity AS (SELECT " + UK + " uk, toDate(timestamp) ad FROM " + EVENTS +
                    " WHERE " + tf("timestamp", start, end) + " GROUP BY uk, ad) " +
                    "SELECT c.cd cohortDate, uniqExact(c.uk) users" + cols +
                    " FROM cohorts c LEFT JOIN activity a ON c.uk=a.uk GROUP BY c.cd ORDER BY c.cd)";
            forEachRow(sql, rs -> {
                long users = rs.getLong("users");
                totalInit[0] += users;
                List<Double> rates = new ArrayList<>();
                for (int i = 0; i < days.size(); i++) {
                    long c = rs.getLong("d" + days.get(i));
                    sumByDay[i] += c;
                    rates.add(users > 0 ? round4((double) c / users) : 0d);
                }
                cohorts.add(new CohortRow(rs.getString("cohortDate"), users, rates));
            });
        }
        double d1 = frac(sumByDay, days, 1, totalInit[0]);
        double d7 = frac(sumByDay, days, 7, totalInit[0]);
        double d30 = frac(sumByDay, days, 30, totalInit[0]);
        return new RetentionResult(new RetentionSummary(d1, d7, d30, d7), cohorts);
    }

    private double retentionFraction(String start, String end, int day) {
        RetentionResult r = retention(start, end);
        return switch (day) {
            case 1 -> r.getSummary().getDay1Rate();
            case 7 -> r.getSummary().getDay7Rate();
            case 30 -> r.getSummary().getDay30Rate();
            default -> 0;
        };
    }

    // ── anomalies: 当日 vs 前一日 ──
    public List<AnomalyItem> anomalies(String date) {
        List<AnomalyItem> out = new ArrayList<>();
        if (ch == null) return out;
        String d = AdvancedAnalysisService.toChTime(date);
        out.add(dayAnomaly("访问人数 (UV)", "uniqExact(" + UK + ")", d));
        out.add(dayAnomaly("访问量 (PV)", "countIf(event_type='page_view')", d));
        out.add(dayAnomaly("会话数", "uniqExact(session_id)", d));
        out.removeIf(java.util.Objects::isNull);
        return out;
    }

    private AnomalyItem dayAnomaly(String metric, String expr, String date) {
        long[] today = {0}, prev = {0};
        forEachRow("SELECT " + expr + " v FROM " + EVENTS + " WHERE toDate(timestamp) = toDate('" + date + "')",
                rs -> today[0] = rs.getLong("v"));
        forEachRow("SELECT " + expr + " v FROM " + EVENTS + " WHERE toDate(timestamp) = toDate('" + date + "') - 1",
                rs -> prev[0] = rs.getLong("v"));
        return anomaly(metric, today[0], prev[0]);
    }

    /** 计算单指标异常项(纯函数):变化率、方向;变化不足 5% 返回 null(不告警)。 */
    static AnomalyItem anomaly(String metric, long today, long prev) {
        if (prev <= 0) return null;
        double change = (double) (today - prev) / prev;
        if (Math.abs(change) < 0.05) return null;
        String dir = change >= 0 ? "up" : "down";
        String sign = change >= 0 ? "+" : "";
        String pct = sign + round1(change * 100) + "%";
        return new AnomalyItem(metric, pct, dir,
                "当日 " + today + " / 前一日 " + prev);
    }

    // ── helpers ──
    static double pct(double fraction) { return round1(fraction * 100); }
    static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    static double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }

    private static String retAlias(List<Integer> days) {
        StringBuilder sb = new StringBuilder();
        for (int d : days) sb.append(", d").append(d);
        return sb.toString();
    }

    private static double frac(double[] sumByDay, List<Integer> days, int day, long totalInit) {
        int idx = days.indexOf(day);
        if (idx < 0 || totalInit <= 0) return 0;
        return round4(sumByDay[idx] / totalInit);
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
            log.error("Platform data query failed: {} | {}", e.getMessage(), sql);
        }
    }
}
