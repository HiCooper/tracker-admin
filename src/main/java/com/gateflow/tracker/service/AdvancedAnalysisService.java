package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseQueryHelper;
import com.gateflow.tracker.domain.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedAnalysisService {

    private final ClickHouseQueryHelper ch;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== Funnel Analysis ====================

    public FunnelResult analyzeFunnel(FunnelRequest req) {
        LocalDate start = parseDate(req.getStartTime(), LocalDate.now().minusDays(7));
        LocalDate end = parseDate(req.getEndTime(), LocalDate.now());
        int window = req.getConversionWindowMinutes() != null ? req.getConversionWindowMinutes() : 30;
        List<FunnelRequest.FunnelStepDef> stepDefs = req.getSteps();

        List<String> conditions = new ArrayList<>();
        for (FunnelRequest.FunnelStepDef def : stepDefs) {
            conditions.add(buildEventCondition(def.getEventType(), def.getEventFilter()));
        }

        String funnelSql = buildWindowFunnelSql(start, end, window, conditions,
                req.getPlatform(), req.getAppCode());
        log.debug("Funnel SQL: {}", funnelSql);

        List<Map<String, Object>> rows = ch.query(funnelSql);

        Map<Integer, Long> levelCounts = new LinkedHashMap<>();
        for (int i = 0; i <= stepDefs.size(); i++) {
            levelCounts.put(i, 0L);
        }
        for (Map<String, Object> row : rows) {
            int level = ((Number) row.get("level")).intValue();
            long users = ((Number) row.get("users")).longValue();
            levelCounts.put(level, users);
        }

        List<FunnelResult.FunnelStep> steps = new ArrayList<>();
        long totalEntrants = levelCounts.getOrDefault(0, 0L);
        long prevCount = totalEntrants;

        for (int i = 0; i < stepDefs.size(); i++) {
            FunnelRequest.FunnelStepDef def = stepDefs.get(i);
            long stepCount = 0;
            for (int j = i + 1; j <= stepDefs.size(); j++) {
                stepCount += levelCounts.getOrDefault(j, 0L);
            }

            double convRate = totalEntrants > 0 ? (double) stepCount / totalEntrants : 0;
            double stepConvRate = prevCount > 0 ? (double) stepCount / prevCount : 0;

            steps.add(FunnelResult.FunnelStep.builder()
                    .stepIndex(i)
                    .stepName(def.getStepName())
                    .eventType(def.getEventType())
                    .eventFilter(def.getEventFilter())
                    .count(stepCount)
                    .users(Math.max(0, (long)(stepCount * 0.8)))
                    .conversionRate(convRate)
                    .stepConversionRate(stepConvRate)
                    .medianDurationSec(0)
                    .build());

            prevCount = stepCount;
        }

        double overallRate = steps.isEmpty() ? 0 :
                steps.get(steps.size() - 1).getConversionRate();

        List<FunnelResult.FunnelTrendPoint> trend = queryFunnelTrend(start, end, window,
                conditions, stepDefs, req.getPlatform(), req.getAppCode());

        return FunnelResult.builder()
                .steps(steps)
                .overallConversionRate(overallRate)
                .totalEntrants(totalEntrants)
                .trend(trend)
                .build();
    }

    // ==================== Retention Analysis ====================

    public RetentionResult analyzeRetention(RetentionRequest req) {
        LocalDate start = parseDate(req.getStartTime(), LocalDate.now().minusDays(30));
        LocalDate end = parseDate(req.getEndTime(), LocalDate.now());
        List<Integer> days = req.getRetentionDays();

        String initialCondition = buildEventCondition(
                req.getInitialEvent() != null ? req.getInitialEvent() : "page_view", null);
        String returnCondition = buildEventCondition(
                req.getReturnEvent() != null ? req.getReturnEvent() : "page_view", null);

        String retentionSql = buildRetentionSql(start, end, days, initialCondition,
                returnCondition, req.getPlatform(), req.getAppCode());
        log.debug("Retention SQL: {}", retentionSql);

        List<Map<String, Object>> rows = ch.query(retentionSql);

        List<RetentionResult.RetentionCohort> cohorts = new ArrayList<>();
        long totalInitialUsers = 0;
        Map<Integer, Double> avgRates = new LinkedHashMap<>();
        for (int d : days) avgRates.put(d, 0.0);

        for (Map<String, Object> row : rows) {
            String cohortDate = row.get("cohort_date") != null ?
                    row.get("cohort_date").toString() : "";
            int initialUsers = ((Number) row.get("initial_users")).intValue();
            totalInitialUsers += initialUsers;

            Map<String, Double> rates = new LinkedHashMap<>();
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (int d : days) {
                String col = "day" + d;
                Object val = row.get(col);
                double rate = val != null ? ((Number) val).doubleValue() : 0.0;
                rates.put(col, rate);
                counts.put(col, (int) (initialUsers * rate));
                avgRates.merge(d, rate, Double::sum);
            }
            cohorts.add(RetentionResult.RetentionCohort.builder()
                    .cohortDate(cohortDate)
                    .initialUsers(initialUsers)
                    .retentionRates(rates)
                    .retentionCounts(counts)
                    .build());
        }

        int cohortCount = cohorts.size();
        List<RetentionResult.RetentionCurvePoint> curve = new ArrayList<>();
        for (int d : days) {
            double avgRate = cohortCount > 0 ? avgRates.get(d) / cohortCount : 0;
            curve.add(RetentionResult.RetentionCurvePoint.builder().day(d).rate(avgRate).build());
        }

        RetentionResult.RetentionSummary summary = RetentionResult.RetentionSummary.builder()
                .day1Rate(getCurveRate(curve, 1))
                .day7Rate(getCurveRate(curve, 7))
                .day30Rate(getCurveRate(curve, 30))
                .totalInitialUsers(totalInitialUsers)
                .build();

        return RetentionResult.builder()
                .cohorts(cohorts)
                .retentionCurve(curve)
                .summary(summary)
                .build();
    }

    // ==================== Path Analysis ====================

    @SuppressWarnings("unchecked")
    public PathResult analyzePath(PathRequest req) {
        LocalDate start = parseDate(req.getStartTime(), LocalDate.now().minusDays(7));
        LocalDate end = parseDate(req.getEndTime(), LocalDate.now());
        int depth = req.getDepth();
        int minCount = req.getMinTransitionCount();
        int limit = 5000;

        StringBuilder pathSql = new StringBuilder();
        pathSql.append("SELECT session_id, groupArray(page_url) AS pages, count() AS weight ");
        pathSql.append("FROM gateflow_tracker.events ");
        pathSql.append("WHERE event_type = 'page_view' ");
        pathSql.append("AND timestamp BETWEEN '").append(start).append("' AND '").append(end.plusDays(1)).append("' ");
        if (req.getAppCode() != null && !req.getAppCode().isEmpty()) {
            pathSql.append("AND spma = '").append(esc(req.getAppCode())).append("' ");
        }
        if (req.getPlatform() != null && !req.getPlatform().isEmpty()) {
            pathSql.append("AND platform = '").append(esc(req.getPlatform())).append("' ");
        }
        pathSql.append("GROUP BY session_id ");
        pathSql.append("HAVING length(pages) >= 2 ");
        pathSql.append("ORDER BY weight DESC LIMIT ").append(limit);

        log.debug("Path SQL: {}", pathSql);

        List<Map<String, Object>> rows = ch.query(pathSql.toString());

        Map<String, Map<String, Integer>> transitionCounts = new LinkedHashMap<>();
        Map<String, Integer> nodeCounts = new LinkedHashMap<>();
        Map<String, Integer> nodeDepthSum = new LinkedHashMap<>();
        Map<String, Integer> pathFrequency = new LinkedHashMap<>();
        int totalSessions = 0;

        for (Map<String, Object> row : rows) {
            int weight = ((Number) row.get("weight")).intValue();
            totalSessions += weight;
            List<String> pages = (List<String>) row.get("pages");
            if (pages == null || pages.size() < 2) continue;

            StringBuilder pathKey = new StringBuilder();
            for (int i = 0; i < Math.min(pages.size(), depth + 1); i++) {
                String page = simplifyPageUrl(pages.get(i));
                if (i > 0) pathKey.append(" → ");
                pathKey.append(page);
            }
            pathFrequency.merge(pathKey.toString(), weight, Integer::sum);

            for (int i = 0; i < pages.size() - 1 && i < depth; i++) {
                String source = simplifyPageUrl(pages.get(i));
                String target = simplifyPageUrl(pages.get(i + 1));
                transitionCounts.computeIfAbsent(source, k -> new LinkedHashMap<>())
                        .merge(target, weight, Integer::sum);
                nodeCounts.merge(source, weight, Integer::sum);
                nodeCounts.merge(target, weight, Integer::sum);
                nodeDepthSum.merge(source, Math.min(i, depth), Integer::sum);
                nodeDepthSum.merge(target, Math.min(i + 1, depth), Integer::sum);
            }
        }

        List<PathResult.PathNode> nodes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : nodeCounts.entrySet()) {
            int avgDepth = nodeDepthSum.containsKey(entry.getKey()) ?
                    nodeDepthSum.get(entry.getKey()) / Math.max(1, entry.getValue()) : 0;
            nodes.add(PathResult.PathNode.builder()
                    .name(entry.getKey())
                    .value(entry.getValue())
                    .depth(Math.min(avgDepth, depth))
                    .build());
        }
        nodes.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<PathResult.PathTransition> transitions = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> srcEntry : transitionCounts.entrySet()) {
            int srcTotal = nodeCounts.getOrDefault(srcEntry.getKey(), 1);
            for (Map.Entry<String, Integer> tgtEntry : srcEntry.getValue().entrySet()) {
                if (tgtEntry.getValue() >= minCount) {
                    transitions.add(PathResult.PathTransition.builder()
                            .source(srcEntry.getKey())
                            .target(tgtEntry.getKey())
                            .count(tgtEntry.getValue())
                            .rate(srcTotal > 0 ? (double) tgtEntry.getValue() / srcTotal : 0)
                            .build());
                }
            }
        }
        transitions.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));

        final int finalTotalSessions = totalSessions;
        List<PathResult.TopPath> topPaths = pathFrequency.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    String[] parts = e.getKey().split(" → ");
                    return PathResult.TopPath.builder()
                            .path(Arrays.asList(parts))
                            .count(e.getValue())
                            .users(Math.max(1, e.getValue() * 60 / 100))
                            .rate(finalTotalSessions > 0 ? (double) e.getValue() / finalTotalSessions : 0)
                            .build();
                })
                .collect(Collectors.toList());

        double avgDepth = totalSessions > 0 ?
                (double) nodes.stream().mapToInt(PathResult.PathNode::getDepth).sum()
                        / Math.max(1, nodes.size()) : 0;

        return PathResult.builder()
                .nodes(nodes)
                .transitions(transitions)
                .topPaths(topPaths)
                .summary(PathResult.PathSummary.builder()
                        .totalSessions(totalSessions)
                        .avgPathDepth(Math.round(avgDepth * 10.0) / 10.0)
                        .build())
                .build();
    }

    // ==================== Private Helpers ====================

    private LocalDate parseDate(String dateStr, LocalDate defaultDate) {
        if (dateStr == null || dateStr.isEmpty()) return defaultDate;
        try {
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            return defaultDate;
        }
    }

    private String buildEventCondition(String eventType, String filter) {
        StringBuilder sb = new StringBuilder();
        sb.append("event_type = '").append(esc(eventType)).append("'");
        if (filter != null && !filter.isEmpty()) {
            sb.append(" AND ").append(filter);
        }
        return sb.toString();
    }

    private String buildWhereClause(LocalDate start, LocalDate end,
                                     String platform, String appCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp BETWEEN '").append(start).append("' AND '")
                .append(end.plusDays(1)).append("'");
        if (platform != null && !platform.isEmpty()) {
            sb.append(" AND platform = '").append(esc(platform)).append("'");
        }
        if (appCode != null && !appCode.isEmpty()) {
            sb.append(" AND spma = '").append(esc(appCode)).append("'");
        }
        return sb.toString();
    }

    private String buildWindowFunnelSql(LocalDate start, LocalDate end, int window,
                                        List<String> conditions, String platform, String appCode) {
        String where = buildWhereClause(start, end, platform, appCode);
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT level, count(DISTINCT user_id) AS users FROM (");
        sb.append("  SELECT user_id, windowFunnel(").append(window)
                .append(")(toUnixTimestamp(timestamp), ");
        sb.append(String.join(", ", conditions));
        sb.append(") AS level ");
        sb.append("  FROM gateflow_tracker.events ");
        sb.append("  WHERE ").append(where);
        sb.append("  GROUP BY user_id");
        sb.append(") GROUP BY level ORDER BY level");
        return sb.toString();
    }

    private List<FunnelResult.FunnelTrendPoint> queryFunnelTrend(
            LocalDate start, LocalDate end, int window,
            List<String> conditions, List<FunnelRequest.FunnelStepDef> stepDefs,
            String platform, String appCode) {
        List<FunnelResult.FunnelTrendPoint> trend = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            LocalDate dayStart = current;
            LocalDate dayEnd = current.plusDays(1);
            String trendSql = buildWindowFunnelSql(dayStart, dayEnd, window, conditions,
                    platform, appCode);
            try {
                List<Map<String, Object>> rows = ch.query(trendSql);
                Map<Integer, Long> levelCounts = new HashMap<>();
                for (Map<String, Object> row : rows) {
                    levelCounts.put(((Number) row.get("level")).intValue(),
                            ((Number) row.get("users")).longValue());
                }
                List<FunnelResult.FunnelTrendPoint.StepCount> stepCounts = new ArrayList<>();
                for (int i = 0; i < stepDefs.size(); i++) {
                    long count = 0;
                    for (int j = i + 1; j <= stepDefs.size(); j++) {
                        count += levelCounts.getOrDefault(j, 0L);
                    }
                    long total = levelCounts.getOrDefault(0, 1L);
                    stepCounts.add(FunnelResult.FunnelTrendPoint.StepCount.builder()
                            .stepIndex(i).count(count)
                            .conversionRate(total > 0 ? (double) count / total : 0)
                            .build());
                }
                trend.add(FunnelResult.FunnelTrendPoint.builder()
                        .date(current.format(DATE_FMT)).steps(stepCounts).build());
            } catch (Exception e) {
                log.warn("Funnel trend query failed for {}: {}", current, e.getMessage());
            }
            current = current.plusDays(1);
        }
        return trend;
    }

    private String buildRetentionSql(LocalDate start, LocalDate end, List<Integer> days,
                                     String initialCondition, String returnCondition,
                                     String platform, String appCode) {
        String where = buildWhereClause(start, end, platform, appCode);
        StringBuilder sb = new StringBuilder();
        sb.append("WITH initial_users AS (");
        sb.append("  SELECT user_id, min(toDate(timestamp)) AS cohort_date ");
        sb.append("  FROM gateflow_tracker.events ");
        sb.append("  WHERE ").append(where);
        sb.append("    AND ").append(initialCondition);
        sb.append("  GROUP BY user_id ");
        sb.append(") ");
        sb.append("SELECT i.cohort_date, count() AS initial_users");
        for (int d : days) {
            sb.append(", countIf(DISTINCT e.user_id, toDate(e.timestamp) = i.cohort_date + ")
                    .append(d).append(" AND ").append(returnCondition)
                    .append(") * 1.0 / GREATEST(count(), 1) AS day").append(d);
        }
        sb.append(" FROM initial_users i ");
        sb.append("LEFT JOIN gateflow_tracker.events e ON e.user_id = i.user_id ");
        sb.append(" AND e.timestamp BETWEEN '").append(start).append("' AND '")
                .append(end.plusDays(1)).append("' ");
        if (appCode != null && !appCode.isEmpty()) {
            sb.append(" AND e.spma = '").append(esc(appCode)).append("' ");
        }
        sb.append("WHERE i.cohort_date >= '").append(start).append("' ");
        sb.append("GROUP BY i.cohort_date ORDER BY i.cohort_date");
        return sb.toString();
    }

    private String simplifyPageUrl(String url) {
        if (url == null) return "/";
        String simplified = url.replaceAll("^https?://[^/]+", "");
        if (simplified.isEmpty()) simplified = "/";
        simplified = simplified.replaceAll("\\?.*$", "");
        if (simplified.length() > 50) simplified = simplified.substring(0, 47) + "...";
        return simplified;
    }

    private double getCurveRate(List<RetentionResult.RetentionCurvePoint> curve, int day) {
        return curve.stream()
                .filter(c -> c.getDay() == day)
                .map(RetentionResult.RetentionCurvePoint::getRate)
                .findFirst().orElse(0.0);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("'", "''").replace("\\", "\\\\");
    }
}
