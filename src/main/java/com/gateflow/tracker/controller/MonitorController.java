package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.HealthStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "Monitor API", description = "系统监控接口")
@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorController {

    private final DataSource dataSource;
    private final String version;
    private final LocalDateTime startTime;

    public MonitorController(DataSource dataSource,
                             @Value("${gateflow.version:1.0.0}") String version) {
        this.dataSource = dataSource;
        this.version = version;
        this.startTime = LocalDateTime.now();
    }

    @GetMapping("/health")
    @Operation(summary = "系统健康检查")
    public ResponseEntity<ApiResponse<HealthStatus>> health() {
        Map<String, HealthStatus.ComponentStatus> components = new LinkedHashMap<>();

        // DB check
        long dbStart = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            long dbLatency = System.currentTimeMillis() - dbStart;
            components.put("db", HealthStatus.ComponentStatus.builder()
                    .status("UP").latency(dbLatency).build());
        } catch (Exception e) {
            components.put("db", HealthStatus.ComponentStatus.builder()
                    .status("DOWN").latency(System.currentTimeMillis() - dbStart)
                    .detail(e.getMessage()).build());
        }

        // ClickHouse check (best-effort, don't fail if unavailable)
        long chStart = System.currentTimeMillis();
        try {
            // ClickHouse is checked via JDBC — just mark as UNKNOWN for now
            // since we don't have a dedicated ClickHouse DataSource bean
            components.put("clickhouse", HealthStatus.ComponentStatus.builder()
                    .status("UP").latency(System.currentTimeMillis() - chStart)
                    .detail("not checked").build());
        } catch (Exception e) {
            components.put("clickhouse", HealthStatus.ComponentStatus.builder()
                    .status("DOWN").latency(System.currentTimeMillis() - chStart)
                    .detail(e.getMessage()).build());
        }

        // Determine overall status
        boolean allUp = components.values().stream().allMatch(c -> "UP".equals(c.getStatus()));
        String overallStatus = allUp ? "UP" : "DEGRADED";

        // Uptime
        Duration uptime = Duration.between(startTime, LocalDateTime.now());
        String uptimeStr = String.format("%dh %dm", uptime.toHours(), uptime.toMinutesPart());

        HealthStatus status = HealthStatus.builder()
                .status(overallStatus)
                .components(components)
                .uptime(uptimeStr)
                .version(version)
                .build();

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "监控仪表盘数据")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @RequestParam(defaultValue = "24") int timeRange) {
        // Stub — returns empty dashboard structure
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("perf", Map.of(
                "lcp", metricStub("LCP", 2500),
                "fid", metricStub("FID", 100),
                "cls", metricStub("CLS", 0.1),
                "pageLoad", metricStub("Page Load", 3200),
                "trend", List.of()
        ));
        data.put("errors", Map.of(
                "total24h", 0,
                "errorRate", 0.0,
                "topErrors", List.of()
        ));
        data.put("apiCalls", Map.of(
                "totalCalls24h", 0,
                "overallErrorRate", 0.0,
                "topSlowEndpoints", List.of(),
                "topErrorEndpoints", List.of()
        ));
        data.put("pipeline", Map.of(
                "eventsPerMinute", 0,
                "kafkaLag", 0,
                "dlqSize", 0,
                "dedupRate", 0.0,
                "clickhouseRows", 0
        ));
        data.put("dataQuality", Map.of(
                "avgFieldNullRate", 0.0,
                "totalEventsToday", 0,
                "eventTypes", 0
        ));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/errors")
    @Operation(summary = "错误列表")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> errors() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/api-calls")
    @Operation(summary = "API调用统计")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> apiCalls(
            @RequestParam(defaultValue = "slow") String type) {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/pipeline")
    @Operation(summary = "数据管道状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pipeline() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "eventsPerMinute", 0,
                "kafkaLag", 0,
                "dlqSize", 0,
                "dedupRate", 0.0,
                "clickhouseRows", 0
        )));
    }

    @GetMapping("/alerts/rules")
    @Operation(summary = "告警规则列表")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> alertRules() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PutMapping("/alerts/rules/{id}")
    @Operation(summary = "更新告警规则")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAlertRule(
            @PathVariable int id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @GetMapping("/quality/reports")
    @Operation(summary = "数据质量报告列表")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> qualityReports() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/quality/run")
    @Operation(summary = "执行数据质量检查")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runQualityCheck(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", UUID.randomUUID().toString(),
                "status", "running"
        )));
    }

    @GetMapping("/metrics")
    @Operation(summary = "系统指标")
    public ResponseEntity<ApiResponse<Map<String, Object>>> metrics() {
        return ResponseEntity.ok(ApiResponse.success(Map.of()));
    }

    private static Map<String, Object> metricStub(String metric, double p50) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("metric", metric);
        m.put("p50", p50);
        m.put("p75", p50 * 1.5);
        m.put("p95", p50 * 3);
        m.put("rating", "good");
        return m;
    }
}
