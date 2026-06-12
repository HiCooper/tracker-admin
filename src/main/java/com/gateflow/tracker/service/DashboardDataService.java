package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.config.ClickHouseQueryHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardDataService {

    private final ClickHouseQueryHelper ch;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EVENTS = "gateflow_tracker.events";

    @Data
    public static class PanelData {
        private String panelId;
        private String type;
        private String title;
        private Object result;
        private String error;
    }

    @Data
    public static class DashboardData {
        private Long dashboardId;
        private String name;
        private List<PanelData> panels;
    }

    public DashboardData execute(Long dashboardId, String name, String configJson,
                                  String startTime, String endTime) {
        DashboardData data = new DashboardData();
        data.setDashboardId(dashboardId);
        data.setName(name);
        List<PanelData> panels = new ArrayList<>();

        LocalDate start = parseDate(startTime, LocalDate.now().minusDays(7));
        LocalDate end = parseDate(endTime, LocalDate.now());

        try {
            JsonNode cfg = objectMapper.readTree(configJson);
            JsonNode pns = cfg.get("panels");
            if (pns != null && pns.isArray()) {
                for (JsonNode p : pns) {
                    panels.add(execPanel(p, start, end));
                }
            }
        } catch (Exception e) {
            log.error("Dashboard exec failed id={}", dashboardId, e);
            PanelData err = new PanelData();
            err.setError("Execution failed: " + e.getMessage());
            panels.add(err);
        }
        data.setPanels(panels);
        return data;
    }

    private PanelData execPanel(JsonNode p, LocalDate start, LocalDate end) {
        PanelData pd = new PanelData();
        try {
            pd.setPanelId(p.has("id") ? p.get("id").asText() : UUID.randomUUID().toString().substring(0, 8));
            pd.setType(p.has("type") ? p.get("type").asText() : "metric");
            pd.setTitle(p.has("title") ? p.get("title").asText() : "");
            JsonNode q = p.get("query");
            if (q == null) { pd.setResult(null); return pd; }
            pd.setResult(switch (pd.getType()) {
                case "metric" -> execMetric(q, start, end);
                case "trend" -> execTrend(q, start, end);
                case "table" -> execTable(q, start, end);
                case "bar", "pie" -> execGroup(q, start, end);
                default -> Map.of("msg", "Unsupported: " + pd.getType());
            });
        } catch (Exception e) {
            log.warn("Panel failed: {}", pd.getPanelId(), e);
            pd.setError(e.getMessage());
        }
        return pd;
    }

    private Object execMetric(JsonNode q, LocalDate start, LocalDate end) {
        String aggFn = agg(q);
        String eventType = q.has("eventType") ? q.get("eventType").asText() : null;
        StringBuilder sql = new StringBuilder("SELECT ").append(aggFn).append(" AS value FROM ").append(EVENTS);
        sql.append(" WHERE timestamp BETWEEN '").append(start).append("' AND '").append(end.plusDays(1)).append("'");
        if (eventType != null) sql.append(" AND event_type = '").append(esc(eventType)).append("'");
        appendFilters(sql, q);
        Map<String, Object> row = ch.queryOne(sql.toString());
        return Map.of("value", row.get("value"), "label", q.has("label") ? q.get("label").asText() : "value");
    }

    private Object execTrend(JsonNode q, LocalDate start, LocalDate end) {
        String aggFn = agg(q);
        String eventType = q.has("eventType") ? q.get("eventType").asText() : null;
        int days = q.has("days") ? q.get("days").asInt() : 7;

        StringBuilder sql = new StringBuilder("SELECT toDate(timestamp) AS date, ").append(aggFn).append(" AS value FROM ").append(EVENTS);
        sql.append(" WHERE timestamp BETWEEN '").append(start).append("' AND '").append(end.plusDays(1)).append("'");
        if (eventType != null) sql.append(" AND event_type = '").append(esc(eventType)).append("'");
        appendFilters(sql, q);
        sql.append(" GROUP BY date ORDER BY date");

        List<Map<String, Object>> rows = ch.query(sql.toString());
        List<Map<String, Object>> pts = new ArrayList<>();
        for (var r : rows) pts.add(Map.of("date", str(r.get("date")), "value", r.get("value")));
        return Map.of("points", pts);
    }

    private Object execTable(JsonNode q, LocalDate start, LocalDate end) {
        String aggFn = agg(q);
        String groupBy = q.has("groupBy") ? q.get("groupBy").asText() : "event_type";
        String eventType = q.has("eventType") ? q.get("eventType").asText() : null;
        int limit = q.has("limit") ? q.get("limit").asInt() : 10;

        StringBuilder sql = new StringBuilder("SELECT ").append(groupBy).append(" AS dim, ").append(aggFn).append(" AS value FROM ").append(EVENTS);
        sql.append(" WHERE timestamp BETWEEN '").append(start).append("' AND '").append(end.plusDays(1)).append("'");
        if (eventType != null) sql.append(" AND event_type = '").append(esc(eventType)).append("'");
        appendFilters(sql, q);
        sql.append(" GROUP BY dim ORDER BY value DESC LIMIT ").append(limit);

        List<Map<String, Object>> rows = ch.query(sql.toString());
        List<Map<String, Object>> items = new ArrayList<>();
        for (var r : rows) items.add(Map.of("dimension", str(r.get("dim")), "value", r.get("value")));
        return Map.of("rows", items, "groupBy", groupBy);
    }

    private Object execGroup(JsonNode q, LocalDate start, LocalDate end) {
        String aggFn = agg(q);
        String groupBy = q.has("groupBy") ? q.get("groupBy").asText() : "event_type";
        int limit = q.has("limit") ? q.get("limit").asInt() : 10;

        StringBuilder sql = new StringBuilder("SELECT ").append(groupBy).append(" AS name, ").append(aggFn).append(" AS value FROM ").append(EVENTS);
        sql.append(" WHERE timestamp BETWEEN '").append(start).append("' AND '").append(end.plusDays(1)).append("'");
        appendFilters(sql, q);
        sql.append(" GROUP BY name ORDER BY value DESC LIMIT ").append(limit);

        List<Map<String, Object>> rows = ch.query(sql.toString());
        List<Map<String, Object>> items = new ArrayList<>();
        for (var r : rows) items.add(Map.of("name", str(r.get("name")), "value", r.get("value")));
        return Map.of("items", items);
    }

    private String agg(JsonNode q) {
        String m = q.has("metric") ? q.get("metric").asText() : "count";
        String a = q.has("aggregation") ? q.get("aggregation").asText() : "count";
        return switch (a) {
            case "countDistinct" -> "count(DISTINCT " + m + ")";
            case "avg" -> "avg(" + m + ")";
            case "sum" -> "sum(" + m + ")";
            case "max" -> "max(" + m + ")";
            case "min" -> "min(" + m + ")";
            default -> "count()";
        };
    }

    private void appendFilters(StringBuilder sql, JsonNode q) {
        for (String f : new String[]{"platform", "device_type", "os", "browser", "utm_source", "utm_medium", "utm_campaign"}) {
            if (q.has(f)) sql.append(" AND ").append(f).append(" = '").append(esc(q.get(f).asText())).append("'");
        }
    }

    private LocalDate parseDate(String d, LocalDate def) {
        if (d == null || d.isEmpty()) return def;
        try { return LocalDate.parse(d.substring(0, 10)); } catch (Exception ex) { return def; }
    }

    private String esc(String s) { return s.replace("'", "''"); }
    private String str(Object o) { return o != null ? o.toString() : ""; }
}
