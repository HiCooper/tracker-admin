package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.HealthStatus.ComponentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控数据服务:用真实数据源替代此前 MonitorController 的桩数据。
 *
 * <p>- 管道/数据质量/错误:直查 ClickHouse {@code gateflow_tracker.events}。
 * <p>- 采集侧 DLQ 积压:best-effort 拉取 tracker-service 的 /health(其归属采集服务而非管理服务)。
 * <p>- 无数据源的指标(Web Vitals 性能、API 调用统计)显式标注 {@code available=false},不再编造。
 */
@Slf4j
@Service
public class MonitorService {

    private final DataSource chDataSource;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String trackerServiceBaseUrl;

    public MonitorService(ClickHouseProperties chProps,
                          ObjectMapper objectMapper,
                          @Value("${gateflow.tracker-service.base-url:}") String trackerServiceBaseUrl) {
        this.objectMapper = objectMapper;
        this.trackerServiceBaseUrl = trackerServiceBaseUrl == null ? "" : trackerServiceBaseUrl.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource for monitoring: {}", e.getMessage());
            ds = null;
        }
        this.chDataSource = ds;
    }

    /** ClickHouse 真实健康探测(带延迟)。 */
    public ComponentStatus clickHouseHealth() {
        long start = System.currentTimeMillis();
        if (chDataSource == null) {
            return ComponentStatus.builder().status("DOWN").latency(0L)
                    .detail("datasource unavailable").build();
        }
        try (Connection conn = chDataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("SELECT 1");
            return ComponentStatus.builder().status("UP")
                    .latency(System.currentTimeMillis() - start).build();
        } catch (Exception e) {
            return ComponentStatus.builder().status("DOWN")
                    .latency(System.currentTimeMillis() - start).detail(e.getMessage()).build();
        }
    }

    /** 数据管道状态:CH 行数/吞吐为真实查询;DLQ 来自采集服务;dedup/kafkaLag 暂无数据源。 */
    public Map<String, Object> pipeline() {
        Map<String, Object> m = new LinkedHashMap<>();
        Long rows = queryLong("SELECT count() FROM gateflow_tracker.events");
        Long epm = queryLong("SELECT count() FROM gateflow_tracker.events WHERE timestamp >= now() - INTERVAL 1 MINUTE");
        m.put("clickhouseRows", rows);
        m.put("eventsPerMinute", epm);
        m.put("available", rows != null);

        Long dlq = fetchDlqSizeFromCollector();
        m.put("dlqSize", dlq);
        m.put("dlqAvailable", dlq != null);
        // 以下指标归属采集服务且暂未对管理端暴露,显式标注未采集而非编造
        m.put("dedupRate", null);
        m.put("kafkaLag", null);
        m.put("collectorMetricsAvailable", false);
        return m;
    }

    /** 数据质量:今日事件总量、事件类型数、关键字段(userId)空值率。 */
    public Map<String, Object> dataQuality() {
        Map<String, Object> m = new LinkedHashMap<>();
        Long total = queryLong("SELECT count() FROM gateflow_tracker.events WHERE timestamp >= toStartOfDay(now())");
        Long types = queryLong("SELECT uniqExact(event_type) FROM gateflow_tracker.events WHERE timestamp >= toStartOfDay(now())");
        Double nullRate = queryDouble(
                "SELECT if(count() = 0, 0, countIf(user_id = '') / count()) " +
                "FROM gateflow_tracker.events WHERE timestamp >= toStartOfDay(now())");
        m.put("totalEventsToday", total);
        m.put("eventTypes", types);
        m.put("avgFieldNullRate", nullRate);
        m.put("available", total != null);
        return m;
    }

    /** 最近的 error 类型事件(真实)。 */
    public List<Map<String, Object>> recentErrors(int limit) {
        if (chDataSource == null) return List.of();
        String sql = "SELECT event_id, page_url, properties, toString(timestamp) AS ts " +
                "FROM gateflow_tracker.events WHERE event_type = 'error' " +
                "ORDER BY timestamp DESC LIMIT " + Math.max(1, Math.min(limit, 500));
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection conn = chDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("eventId", rs.getString("event_id"));
                row.put("pageUrl", rs.getString("page_url"));
                row.put("properties", rs.getString("properties"));
                row.put("timestamp", rs.getString("ts"));
                out.add(row);
            }
        } catch (Exception e) {
            log.error("recentErrors query failed: {}", e.getMessage());
        }
        return out;
    }

    /** 性能(Web Vitals)与 API 调用统计目前无采集来源,显式标注未实现。 */
    public Map<String, Object> unavailableSection(String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", false);
        m.put("reason", reason);
        return m;
    }

    // ── helpers ──

    Long fetchDlqSizeFromCollector() {
        if (trackerServiceBaseUrl.isEmpty()) return null;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(trackerServiceBaseUrl.replaceAll("/$", "") + "/health"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2 && resp.statusCode() != 503) return null;
            return parseDlqSize(resp.body());
        } catch (Exception e) {
            log.debug("collector /health fetch failed: {}", e.getMessage());
            return null;
        }
    }

    /** 解析采集服务 /health 中的 services.dlq(形如 "12 entries")。 */
    Long parseDlqSize(String healthJson) {
        try {
            JsonNode dlq = objectMapper.readTree(healthJson).path("services").path("dlq");
            if (dlq.isMissingNode()) return null;
            String text = dlq.asText("");
            String digits = text.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? null : Long.parseLong(digits);
        } catch (Exception e) {
            return null;
        }
    }

    private Long queryLong(String sql) {
        Number n = queryScalar(sql);
        return n == null ? null : n.longValue();
    }

    private Double queryDouble(String sql) {
        Number n = queryScalar(sql);
        return n == null ? null : n.doubleValue();
    }

    private Number queryScalar(String sql) {
        if (chDataSource == null) return null;
        try (Connection conn = chDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                Object v = rs.getObject(1);
                if (v instanceof Number num) return num;
                if (v != null) return Double.valueOf(v.toString());
            }
            return null;
        } catch (Exception e) {
            log.error("monitor scalar query failed [{}]: {}", sql, e.getMessage());
            return null;
        }
    }
}
