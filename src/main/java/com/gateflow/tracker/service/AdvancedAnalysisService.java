package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.advanced.FunnelDto;
import com.gateflow.tracker.domain.dto.advanced.PathDto;
import com.gateflow.tracker.domain.dto.advanced.RetentionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * 高级行为分析:漏斗 / 留存 / 路径,直查 ClickHouse。
 *
 * <p>用户口径采用身份缝合后的 userKey = {@code if(user_id != '', user_id, anonymous_id)};
 * 支持按 app_code / platform 维度过滤(利用事件 app 维度)。CH 不可达时返回空结果。
 */
@Slf4j
@Service
public class AdvancedAnalysisService {

    private static final String EVENTS = "gateflow_tracker.events";
    private static final String USER_KEY = "if(user_id != '', user_id, anonymous_id)";

    private final DataSource ch;

    public AdvancedAnalysisService(ClickHouseProperties chProps) {
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource for advanced analysis: {}", e.getMessage());
            ds = null;
        }
        this.ch = ds;
    }

    // ───────────────────────── Funnel ─────────────────────────

    public FunnelDto.Result funnel(FunnelDto.Request req) {
        List<FunnelDto.StepDef> steps = req.getSteps() != null ? req.getSteps() : List.of();
        if (ch == null || steps.size() < 2) {
            return new FunnelDto.Result(List.of(), 0, 0, List.of());
        }
        String filter = baseFilter(req.getStartTime(), req.getEndTime(), req.getPlatform(), req.getAppCode());
        long winSec = Math.max(60L, (long) req.getConversionWindowMinutes() * 60);

        // windowFunnel: 每用户达到的最大连续步级
        StringBuilder conds = new StringBuilder();
        Set<String> types = new LinkedHashSet<>();
        for (FunnelDto.StepDef s : steps) {
            conds.append(", event_type = '").append(san(s.getEventType())).append("'");
            types.add(s.getEventType());
        }
        String typeIn = inList(types);
        String funnelSql = "SELECT level, count() AS users FROM (" +
                "SELECT " + USER_KEY + " AS uk, windowFunnel(" + winSec + ")(timestamp" + conds + ") AS level " +
                "FROM " + EVENTS + " WHERE " + filter + " AND event_type IN (" + typeIn + ") GROUP BY uk" +
                ") GROUP BY level ORDER BY level";
        Map<Integer, Long> levelCounts = new HashMap<>();
        forEachRow(funnelSql, rs -> levelCounts.put(rs.getInt("level"), rs.getLong("users")));
        long[] reached = usersReachedFromLevels(levelCounts, steps.size());

        // 每步事件总量
        Map<String, Long> eventCounts = new HashMap<>();
        forEachRow("SELECT event_type, count() AS c FROM " + EVENTS + " WHERE " + filter +
                        " AND event_type IN (" + typeIn + ") GROUP BY event_type",
                rs -> eventCounts.put(rs.getString("event_type"), rs.getLong("c")));

        List<FunnelDto.Step> outSteps = buildFunnelSteps(steps, reached, eventCounts);
        long totalEntrants = reached.length > 0 ? reached[0] : 0;
        double overall = totalEntrants > 0 ? (double) reached[reached.length - 1] / totalEntrants : 0;
        return new FunnelDto.Result(outSteps, overall, totalEntrants, funnelTrend(req, steps, filter, typeIn));
    }

    /** 由 windowFunnel 的 level 分布计算各步到达人数:reached[i] = Σ_{level>=i+1} count。 */
    static long[] usersReachedFromLevels(Map<Integer, Long> levelCounts, int n) {
        long[] reached = new long[n];
        for (int i = 0; i < n; i++) {
            long sum = 0;
            for (Map.Entry<Integer, Long> e : levelCounts.entrySet()) {
                if (e.getKey() >= i + 1) sum += e.getValue();
            }
            reached[i] = sum;
        }
        return reached;
    }

    static List<FunnelDto.Step> buildFunnelSteps(List<FunnelDto.StepDef> defs, long[] reached,
                                                 Map<String, Long> eventCounts) {
        List<FunnelDto.Step> out = new ArrayList<>();
        long first = reached.length > 0 ? reached[0] : 0;
        for (int i = 0; i < defs.size(); i++) {
            FunnelDto.StepDef d = defs.get(i);
            long users = reached[i];
            double conv = first > 0 ? (double) users / first : 0;
            double stepConv = i == 0 ? 1.0 : (reached[i - 1] > 0 ? (double) users / reached[i - 1] : 0);
            out.add(new FunnelDto.Step(i, d.getStepName(), d.getEventType(), d.getEventFilter(),
                    eventCounts.getOrDefault(d.getEventType(), 0L), users, conv, stepConv, 0));
        }
        return out;
    }

    private List<FunnelDto.TrendPoint> funnelTrend(FunnelDto.Request req, List<FunnelDto.StepDef> steps,
                                                   String filter, String typeIn) {
        // 近似趋势:按天的每步事件量 + 相对首步的转化(事件量口径)
        String sql = "SELECT toDate(timestamp) AS d, event_type AS et, count() AS c FROM " + EVENTS +
                " WHERE " + filter + " AND event_type IN (" + typeIn + ") GROUP BY d, et ORDER BY d";
        // date -> (eventType -> count)
        Map<String, Map<String, Long>> byDate = new LinkedHashMap<>();
        forEachRow(sql, rs -> byDate.computeIfAbsent(rs.getString("d"), k -> new HashMap<>())
                .put(rs.getString("et"), rs.getLong("c")));
        List<FunnelDto.TrendPoint> trend = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> e : byDate.entrySet()) {
            long firstCount = e.getValue().getOrDefault(steps.get(0).getEventType(), 0L);
            List<FunnelDto.TrendStep> ts = new ArrayList<>();
            for (int i = 0; i < steps.size(); i++) {
                long c = e.getValue().getOrDefault(steps.get(i).getEventType(), 0L);
                ts.add(new FunnelDto.TrendStep(i, c, firstCount > 0 ? (double) c / firstCount : 0));
            }
            trend.add(new FunnelDto.TrendPoint(e.getKey(), ts));
        }
        return trend;
    }

    // ──────────────────────── Retention ────────────────────────

    public RetentionDto.Result retention(RetentionDto.Request req) {
        List<Integer> days = req.getRetentionDays() != null && !req.getRetentionDays().isEmpty()
                ? req.getRetentionDays() : List.of(1, 7, 30);
        if (ch == null || !StringUtils.hasText(req.getInitialEvent())) {
            return new RetentionDto.Result(List.of(), List.of(),
                    new RetentionDto.Summary(0, 0, 0, 0));
        }
        String filter = baseFilter(req.getStartTime(), req.getEndTime(), req.getPlatform(), req.getAppCode());
        String initial = san(req.getInitialEvent());
        String ret = san(StringUtils.hasText(req.getReturnEvent()) ? req.getReturnEvent() : req.getInitialEvent());

        StringBuilder cols = new StringBuilder();
        for (int d : days) {
            cols.append(", uniqExactIf(c.uk, a.ad = c.cohortDate + ").append(d).append(") AS d_").append(d);
        }
        String sql = "SELECT cohortDate, initialUsers" + retSelectAliases(days) + " FROM (" +
                "WITH cohorts AS (SELECT " + USER_KEY + " AS uk, min(toDate(timestamp)) AS cohortDate " +
                "FROM " + EVENTS + " WHERE " + filter + " AND event_type = '" + initial + "' GROUP BY uk), " +
                "activity AS (SELECT " + USER_KEY + " AS uk, toDate(timestamp) AS ad " +
                "FROM " + EVENTS + " WHERE " + filter + " AND event_type = '" + ret + "' GROUP BY uk, ad) " +
                "SELECT c.cohortDate AS cohortDate, uniqExact(c.uk) AS initialUsers" + cols + " " +
                "FROM cohorts c LEFT JOIN activity a ON c.uk = a.uk GROUP BY c.cohortDate ORDER BY c.cohortDate)";

        List<RetentionDto.Cohort> cohorts = new ArrayList<>();
        forEachRow(sql, rs -> {
            long init = rs.getLong("initialUsers");
            Map<String, Long> counts = new LinkedHashMap<>();
            Map<String, Double> rates = new LinkedHashMap<>();
            for (int d : days) {
                long c = rs.getLong("d_" + d);
                counts.put("d" + d, c);
                rates.put("d" + d, init > 0 ? (double) c / init : 0);
            }
            cohorts.add(new RetentionDto.Cohort(rs.getString("cohortDate"), init, rates, counts));
        });
        return new RetentionDto.Result(cohorts, retentionCurve(cohorts, days), retentionSummary(cohorts, days));
    }

    private static String retSelectAliases(List<Integer> days) {
        StringBuilder sb = new StringBuilder();
        for (int d : days) sb.append(", d_").append(d);
        return sb.toString();
    }

    static List<RetentionDto.CurvePoint> retentionCurve(List<RetentionDto.Cohort> cohorts, List<Integer> days) {
        long totalInit = cohorts.stream().mapToLong(RetentionDto.Cohort::getInitialUsers).sum();
        List<RetentionDto.CurvePoint> curve = new ArrayList<>();
        for (int d : days) {
            long retained = cohorts.stream()
                    .mapToLong(c -> c.getRetentionCounts().getOrDefault("d" + d, 0L)).sum();
            curve.add(new RetentionDto.CurvePoint(d, totalInit > 0 ? (double) retained / totalInit : 0));
        }
        return curve;
    }

    static RetentionDto.Summary retentionSummary(List<RetentionDto.Cohort> cohorts, List<Integer> days) {
        long totalInit = cohorts.stream().mapToLong(RetentionDto.Cohort::getInitialUsers).sum();
        Map<Integer, Double> rateByDay = new HashMap<>();
        for (RetentionDto.CurvePoint p : retentionCurve(cohorts, days)) {
            rateByDay.put(p.getDay(), p.getRate());
        }
        return new RetentionDto.Summary(
                rateByDay.getOrDefault(1, 0d), rateByDay.getOrDefault(7, 0d),
                rateByDay.getOrDefault(30, 0d), totalInit);
    }

    // ────────────────────────── Path ──────────────────────────

    public PathDto.Result path(PathDto.Request req) {
        if (ch == null) {
            return new PathDto.Result(List.of(), List.of(), List.of(), new PathDto.Summary(0, 0));
        }
        String filter = baseFilter(req.getStartTime(), req.getEndTime(), req.getPlatform(), req.getAppCode());
        int depth = req.getDepth() > 0 ? Math.min(req.getDepth(), 10) : 5;
        long minCount = req.getMinTransitionCount() != null ? Math.max(1, req.getMinTransitionCount()) : 1;
        String pvFilter = filter + " AND event_type = 'page_view' AND page_url != ''";

        // 会话内有序页面序列
        String orderedPages = "SELECT session_id, groupArray(page_url) AS pages FROM (" +
                "SELECT session_id, page_url, timestamp FROM " + EVENTS + " WHERE " + pvFilter +
                " AND session_id != '' ORDER BY session_id, timestamp) GROUP BY session_id";

        // 相邻页面转移
        List<PathDto.Transition> transitions = new ArrayList<>();
        long[] totalTrans = {0};
        String transSql = "SELECT source, target, count() AS c FROM (" +
                "SELECT arrayJoin(arrayMap(i -> (pages[i], pages[i+1]), range(1, length(pages)))) AS pair, " +
                "pair.1 AS source, pair.2 AS target FROM (" + orderedPages + ") WHERE length(pages) >= 2" +
                ") GROUP BY source, target HAVING c >= " + minCount + " ORDER BY c DESC LIMIT 200";
        forEachRow(transSql, rs -> {
            long c = rs.getLong("c");
            totalTrans[0] += c;
            transitions.add(new PathDto.Transition(rs.getString("source"), rs.getString("target"), c, 0));
        });
        for (PathDto.Transition t : transitions) {
            t.setRate(totalTrans[0] > 0 ? (double) t.getCount() / totalTrans[0] : 0);
        }

        // 节点(页面访问量)
        List<PathDto.Node> nodes = new ArrayList<>();
        forEachRow("SELECT page_url, count() AS c FROM " + EVENTS + " WHERE " + pvFilter +
                        " GROUP BY page_url ORDER BY c DESC LIMIT 100",
                rs -> nodes.add(new PathDto.Node(rs.getString("page_url"), rs.getLong("c"), 0)));

        // Top 路径(前 depth 个页面拼接)
        List<PathDto.TopPath> topPaths = new ArrayList<>();
        long[] totalSessions = {0};
        String topSql = "SELECT pathStr, count() AS c FROM (" +
                "SELECT arrayStringConcat(arraySlice(pages, 1, " + depth + "), ' > ') AS pathStr FROM (" +
                orderedPages + ") WHERE length(pages) >= 1) GROUP BY pathStr ORDER BY c DESC LIMIT 20";
        forEachRow(topSql, rs -> {
            long c = rs.getLong("c");
            List<String> p = Arrays.asList(rs.getString("pathStr").split(" > "));
            topPaths.add(new PathDto.TopPath(p, c, c, 0));
        });

        // 汇总
        double[] avgDepth = {0};
        forEachRow("SELECT uniqExact(session_id) AS s, avg(cnt) AS ad FROM (" +
                "SELECT session_id, count() AS cnt FROM " + EVENTS + " WHERE " + pvFilter +
                " AND session_id != '' GROUP BY session_id)", rs -> {
            totalSessions[0] = rs.getLong("s");
            avgDepth[0] = rs.getDouble("ad");
        });
        for (PathDto.TopPath tp : topPaths) {
            tp.setRate(totalSessions[0] > 0 ? (double) tp.getCount() / totalSessions[0] : 0);
        }
        return new PathDto.Result(nodes, transitions, topPaths,
                new PathDto.Summary(totalSessions[0], avgDepth[0]));
    }

    // ───────────────────────── helpers ─────────────────────────

    /** 构造时间 + 可选 platform/appCode 过滤(值去引号防注入)。 */
    static String baseFilter(String start, String end, String platform, String appCode) {
        StringBuilder w = new StringBuilder();
        w.append("timestamp >= '").append(toChTime(start)).append("'")
                .append(" AND timestamp <= '").append(toChTime(end)).append("'");
        if (StringUtils.hasText(platform)) {
            w.append(" AND platform = '").append(san(platform)).append("'");
        }
        if (StringUtils.hasText(appCode)) {
            w.append(" AND app_code = '").append(san(appCode)).append("'");
        }
        return w.toString();
    }

    /** ISO/日期字符串归一为 ClickHouse 可解析的 'yyyy-MM-dd[ HH:mm:ss]'。 */
    static String toChTime(String v) {
        if (!StringUtils.hasText(v)) {
            return "1970-01-01";
        }
        String s = san(v).replace('T', ' ');
        int dot = s.indexOf('.');
        if (dot > 0) s = s.substring(0, dot);
        int z = s.indexOf('Z');
        if (z > 0) s = s.substring(0, z);
        return s.trim();
    }

    static String san(String v) {
        return v == null ? "" : v.replace("'", "").replace("\\", "");
    }

    private static String inList(Collection<String> types) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String t : types) {
            if (!first) sb.append(", ");
            sb.append("'").append(san(t)).append("'");
            first = false;
        }
        return sb.toString();
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(ResultSet rs) throws Exception;
    }

    private void forEachRow(String sql, RowConsumer consumer) {
        if (ch == null) {
            return;
        }
        try (Connection conn = ch.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                consumer.accept(rs);
            }
        } catch (Exception e) {
            log.error("Advanced analysis query failed: {} | {}", e.getMessage(), sql);
        }
    }
}
