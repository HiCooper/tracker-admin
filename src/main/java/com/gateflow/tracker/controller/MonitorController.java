package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.HealthStatus;
import com.gateflow.tracker.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Monitor API", description = "系统监控接口")
@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorController {

    private final DataSource dataSource;
    private final MonitorService monitorService;
    private final String version;
    private final LocalDateTime startTime;

    public MonitorController(DataSource dataSource,
                             MonitorService monitorService,
                             @Value("${gateflow.version:1.0.0}") String version) {
        this.dataSource = dataSource;
        this.monitorService = monitorService;
        this.version = version;
        this.startTime = LocalDateTime.now();
    }

    @GetMapping("/health")
    @Operation(summary = "系统健康检查")
    public ResponseEntity<ApiResponse<HealthStatus>> health() {
        Map<String, HealthStatus.ComponentStatus> components = new LinkedHashMap<>();

        // MySQL (real)
        long dbStart = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            components.put("db", HealthStatus.ComponentStatus.builder()
                    .status("UP").latency(System.currentTimeMillis() - dbStart).build());
        } catch (Exception e) {
            components.put("db", HealthStatus.ComponentStatus.builder()
                    .status("DOWN").latency(System.currentTimeMillis() - dbStart)
                    .detail(e.getMessage()).build());
        }

        // ClickHouse (real probe)
        components.put("clickhouse", monitorService.clickHouseHealth());

        boolean allUp = components.values().stream().allMatch(c -> "UP".equals(c.getStatus()));
        Duration uptime = Duration.between(startTime, LocalDateTime.now());

        HealthStatus status = HealthStatus.builder()
                .status(allUp ? "UP" : "DEGRADED")
                .components(components)
                .uptime(String.format("%dh %dm", uptime.toHours(), uptime.toMinutesPart()))
                .version(version)
                .build();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "监控仪表盘数据")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @RequestParam(defaultValue = "24") int timeRange) {
        Map<String, Object> data = new LinkedHashMap<>();
        // Web Vitals 性能、API 调用统计当前无采集来源,显式标注未实现(不再编造数值)
        data.put("perf", monitorService.unavailableSection("Web Vitals 采集未接入"));
        data.put("apiCalls", monitorService.unavailableSection("API 调用统计采集未接入"));
        data.put("errors", Map.of("recent", monitorService.recentErrors(20)));
        data.put("pipeline", monitorService.pipeline());
        data.put("dataQuality", monitorService.dataQuality());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/errors")
    @Operation(summary = "错误列表")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> errors(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(monitorService.recentErrors(limit)));
    }

    @GetMapping("/api-calls")
    @Operation(summary = "API调用统计(未接入)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> apiCalls(
            @RequestParam(defaultValue = "slow") String type) {
        return ResponseEntity.ok(ApiResponse.success(
                monitorService.unavailableSection("API 调用统计采集未接入")));
    }

    @GetMapping("/pipeline")
    @Operation(summary = "数据管道状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pipeline() {
        return ResponseEntity.ok(ApiResponse.success(monitorService.pipeline()));
    }

    @GetMapping("/metrics")
    @Operation(summary = "系统指标")
    public ResponseEntity<ApiResponse<Map<String, Object>>> metrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pipeline", monitorService.pipeline());
        m.put("dataQuality", monitorService.dataQuality());
        return ResponseEntity.ok(ApiResponse.success(m));
    }

    // ── 以下能力暂未实现(无告警引擎/质量检查引擎):保留端点契约,返回空/未实现标记,避免前端 404 与假数据 ──

    @GetMapping("/alerts/rules")
    @Operation(summary = "告警规则列表(未接入告警引擎)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> alertRules() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PutMapping("/alerts/rules/{id}")
    @Operation(summary = "更新告警规则(未接入)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAlertRule(
            @PathVariable int id, @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(
                monitorService.unavailableSection("告警引擎未接入")));
    }

    @GetMapping("/quality/reports")
    @Operation(summary = "数据质量报告列表(未接入)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> qualityReports() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/quality/failures")
    @Operation(summary = "数据质量失败项(未接入)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> qualityFailures() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/quality/run")
    @Operation(summary = "执行数据质量检查(未接入)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runQualityCheck(
            @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(
                monitorService.unavailableSection("数据质量检查引擎未接入")));
    }
}
