package com.gateflow.tracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityService {

    @Qualifier("clickHouseJdbcTemplate")
    private final NamedParameterJdbcTemplate chJdbc;

    public List<Map<String, Object>> fieldNullRates() {
        String sql = """
            SELECT event_type, count() AS total,
                   countIf(empty(user_id)) * 1.0 / count() AS user_id_null,
                   countIf(empty(session_id)) * 1.0 / count() AS session_id_null,
                   countIf(empty(page_url)) * 1.0 / count() AS page_url_null,
                   countIf(empty(platform)) * 1.0 / count() AS platform_null
            FROM gateflow_tracker.events
            WHERE timestamp >= now() - INTERVAL 24 HOUR
            GROUP BY event_type ORDER BY total DESC
            """;
        List<Map<String, Object>> rows = chJdbc.queryForList(sql, new MapSqlParameterSource());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("eventType", row.get("event_type")); item.put("total", row.get("total"));
            item.put("userIdNullRate", row.get("user_id_null"));
            item.put("sessionIdNullRate", row.get("session_id_null"));
            item.put("pageUrlNullRate", row.get("page_url_null"));
            item.put("platformNullRate", row.get("platform_null"));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> volumeComparison() {
        String sql = """
            SELECT today.event_type, today.cnt AS today_count,
                   COALESCE(yesterday.cnt, 0) AS yesterday_count,
                   (today.cnt - COALESCE(yesterday.cnt, 0)) * 1.0 / GREATEST(COALESCE(yesterday.cnt, 0), 1) AS change_rate
            FROM (SELECT event_type, count() AS cnt FROM gateflow_tracker.events
                  WHERE timestamp >= today() GROUP BY event_type) today
            LEFT JOIN (SELECT event_type, count() AS cnt FROM gateflow_tracker.events
                       WHERE timestamp >= yesterday() AND timestamp < today() GROUP BY event_type) yesterday
            ON today.event_type = yesterday.event_type ORDER BY today.cnt DESC
            """;
        List<Map<String, Object>> rows = chJdbc.queryForList(sql, new MapSqlParameterSource());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("eventType", row.get("event_type"));
            item.put("todayCount", row.get("today_count"));
            item.put("yesterdayCount", row.get("yesterday_count"));
            item.put("changeRate", row.get("change_rate"));
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> summary() {
        Map<String, Object> s = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> nullRates = fieldNullRates();
            List<Map<String, Object>> volume = volumeComparison();
            double avgNull = nullRates.stream()
                    .mapToDouble(r -> { Object v = r.get("userIdNullRate"); return v instanceof Number n ? n.doubleValue() : 0; })
                    .average().orElse(0);
            long totalToday = volume.stream()
                    .mapToLong(r -> { Object v = r.get("todayCount"); return v instanceof Number n ? n.longValue() : 0; })
                    .sum();
            s.put("avgFieldNullRate", Math.round(avgNull * 10000) / 10000.0);
            s.put("totalEventsToday", totalToday);
            s.put("eventTypes", volume.size());
            s.put("fieldNullRates", nullRates);
            s.put("volumeComparison", volume);
            s.put("checkedAt", Instant.now().toString());
        } catch (Exception e) { log.error("Data quality summary failed", e); s.put("error", e.getMessage()); }
        return s;
    }
}
