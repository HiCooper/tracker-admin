package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.advanced.FunnelDto;
import com.gateflow.tracker.domain.dto.advanced.PathDto;
import com.gateflow.tracker.domain.dto.advanced.RetentionDto;
import com.gateflow.tracker.domain.dto.behavior.BehaviorDto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行为分析:概览 / 事件 / 漏斗 / 路径 / 留存。漏斗/路径/留存复用 AdvancedAnalysisService(避免重复 CH 逻辑),
 * 概览/事件直查 ClickHouse(含 trend = 与上一等长周期对比)。
 */
@Slf4j
@Service
public class BehaviorService {

    private static final String EVENTS = "gateflow_tracker.events";
    private static final String UK = "if(user_id != '', user_id, anonymous_id)";

    private final DataSource ch;
    private final AdvancedAnalysisService advanced;

    public BehaviorService(ClickHouseProperties chProps, AdvancedAnalysisService advanced) {
        this.advanced = advanced;
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource for behavior: {}", e.getMessage());
            ds = null;
        }
        this.ch = ds;
    }

    private static String tf(String start, String end) {
        return "toDate(timestamp) >= toDate('" + AdvancedAnalysisService.toChTime(start) + "') AND toDate(timestamp) <= toDate('"
                + AdvancedAnalysisService.toChTime(end) + "')";
    }

    // ── overview ──
    public BehaviorOverview overview(String start, String end) {
        BehaviorOverview o = new BehaviorOverview(0, 0, 0, 0, List.of());
        if (ch == null) return o;
        forEachRow("SELECT count() t, uniqExact(event_type) et, uniqExact(" + UK + ") au FROM " + EVENTS + " WHERE " + tf(start, end), rs -> {
            o.setTotalEvents(rs.getLong("t"));
            o.setEventTypeCount(rs.getLong("et"));
            o.setActiveUsers(rs.getLong("au"));
            o.setAvgEventsPerUser(rs.getLong("au") > 0 ? round1((double) rs.getLong("t") / rs.getLong("au")) : 0);
        });
        List<EventSummary> top = events(start, end, null);
        o.setTopEvents(top.size() > 5 ? top.subList(0, 5) : top);
        return o;
    }

    // ── events (含 trend) ──
    public List<EventSummary> events(String start, String end, List<String> eventTypes) {
        List<EventSummary> out = new ArrayList<>();
        if (ch == null) return out;
        String typeFilter = "";
        if (eventTypes != null && !eventTypes.isEmpty()) {
            StringBuilder in = new StringBuilder();
            for (String t : eventTypes) in.append(in.isEmpty() ? "" : ",").append("'").append(AdvancedAnalysisService.san(t)).append("'");
            typeFilter = " AND event_type IN (" + in + ")";
        }
        Map<String, Long> prev = eventCounts(prevWindow(start, end), typeFilter);
        String sql = "SELECT event_type et, count() c, uniqExact(" + UK + ") u FROM " + EVENTS +
                " WHERE " + tf(start, end) + typeFilter + " GROUP BY et ORDER BY c DESC LIMIT 20";
        forEachRow(sql, rs -> {
            String et = rs.getString("et");
            long c = rs.getLong("c"), u = rs.getLong("u");
            long p = prev.getOrDefault(et, 0L);
            double trend = p > 0 ? round1((double) (c - p) / p * 100) : 0;
            out.add(new EventSummary(et, c, u, u > 0 ? round1((double) c / u) : 0, trend));
        });
        return out;
    }

    private Map<String, Long> eventCounts(String[] window, String typeFilter) {
        Map<String, Long> m = new HashMap<>();
        if (window == null) return m;
        forEachRow("SELECT event_type et, count() c FROM " + EVENTS + " WHERE " + tf(window[0], window[1]) + typeFilter + " GROUP BY et",
                rs -> m.put(rs.getString("et"), rs.getLong("c")));
        return m;
    }

    /** 上一等长周期 [start-span-1, start-1]。 */
    static String[] prevWindow(String start, String end) {
        try {
            LocalDate s = LocalDate.parse(AdvancedAnalysisService.toChTime(start).substring(0, 10));
            LocalDate e = LocalDate.parse(AdvancedAnalysisService.toChTime(end).substring(0, 10));
            long span = ChronoUnit.DAYS.between(s, e);
            LocalDate pe = s.minusDays(1);
            LocalDate ps = pe.minusDays(span);
            return new String[]{ps.toString(), pe.toString()};
        } catch (Exception ex) {
            return null;
        }
    }

    // ── funnel (复用 advanced) ──
    public FunnelData funnel(String start, String end, List<String> steps) {
        List<String> types = (steps != null && steps.size() >= 2) ? steps : List.of("page_view", "click", "purchase");
        FunnelDto.Request req = new FunnelDto.Request();
        List<FunnelDto.StepDef> defs = new ArrayList<>();
        for (String t : types) defs.add(new FunnelDto.StepDef(t, t, null));
        req.setSteps(defs);
        req.setStartTime(start); req.setEndTime(end); req.setConversionWindowMinutes(1440);
        FunnelDto.Result r = advanced.funnel(req);

        List<FunnelStep> outSteps = new ArrayList<>();
        String maxDrop = ""; double worst = 2;
        for (FunnelDto.Step s : r.getSteps()) {
            outSteps.add(new FunnelStep(s.getStepName(), s.getUsers(), s.getConversionRate()));
            if (s.getStepIndex() > 0 && s.getStepConversionRate() < worst) {
                worst = s.getStepConversionRate(); maxDrop = s.getStepName();
            }
        }
        return new FunnelData(outSteps, r.getTotalEntrants(), r.getOverallConversionRate(), maxDrop, 0);
    }

    // ── path (复用 advanced) ──
    public PathData path(String start, String end, String startPage, Integer depth) {
        PathDto.Request req = new PathDto.Request();
        req.setStartPage(startPage); req.setDepth(depth != null ? depth : 5);
        req.setStartTime(start); req.setEndTime(end);
        PathDto.Result r = advanced.path(req);
        long totalVisits = r.getNodes().stream().mapToLong(PathDto.Node::getValue).sum();
        List<PathNode> nodes = new ArrayList<>();
        for (PathDto.Node n : r.getNodes()) {
            nodes.add(new PathNode(n.getName(), n.getValue(), totalVisits > 0 ? round4((double) n.getValue() / totalVisits) : 0));
        }
        List<PathTransition> trans = new ArrayList<>();
        for (PathDto.Transition t : r.getTransitions()) {
            trans.add(new PathTransition(t.getSource(), t.getTarget(), t.getCount()));
        }
        return new PathData(nodes, trans, r.getSummary().getTotalSessions(),
                round1(r.getSummary().getAvgPathDepth()), nodes.size());
    }

    // ── retention (复用 advanced) ──
    public RetentionData retention(String start, String end, String initialEvent, String returnEvent) {
        List<Integer> days = List.of(2, 7, 14, 30);
        RetentionDto.Request req = new RetentionDto.Request();
        req.setInitialEvent(StringUtils.hasText(initialEvent) ? initialEvent : "page_view");
        req.setReturnEvent(StringUtils.hasText(returnEvent) ? returnEvent : req.getInitialEvent());
        req.setStartTime(start); req.setEndTime(end); req.setRetentionDays(days);
        RetentionDto.Result r = advanced.retention(req);

        List<RetentionCohort> cohorts = new ArrayList<>();
        for (RetentionDto.Cohort c : r.getCohorts()) {
            List<Double> rates = new ArrayList<>();
            for (int d : days) rates.add(c.getRetentionRates().getOrDefault("d" + d, 0d));
            cohorts.add(new RetentionCohort(c.getCohortDate(), c.getInitialUsers(), rates));
        }
        Map<Integer, Double> curve = new HashMap<>();
        r.getRetentionCurve().forEach(p -> curve.put(p.getDay(), p.getRate()));
        return new RetentionData(cohorts, curve.getOrDefault(2, 0d), curve.getOrDefault(7, 0d), curve.getOrDefault(30, 0d));
    }

    // ── helpers ──
    static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    static double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }

    @FunctionalInterface private interface RowConsumer { void accept(ResultSet rs) throws Exception; }

    private void forEachRow(String sql, RowConsumer consumer) {
        if (ch == null) return;
        try (Connection conn = ch.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) consumer.accept(rs);
        } catch (Exception e) {
            log.error("Behavior query failed: {} | {}", e.getMessage(), sql);
        }
    }
}
