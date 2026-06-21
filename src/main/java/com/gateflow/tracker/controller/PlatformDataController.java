package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.platform.PlatformDto.*;
import com.gateflow.tracker.service.PlatformDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Platform Data API", description = "平台数据 overview")
@RestController
@RequestMapping("/api/v1/data-platform")
@RequiredArgsConstructor
public class PlatformDataController {

    private final PlatformDataService service;

    @GetMapping("/core-metrics")
    @Operation(summary = "核心指标")
    public ResponseEntity<ApiResponse<CoreMetrics>> coreMetrics(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) String appCode) {
        return ResponseEntity.ok(ApiResponse.success(service.coreMetrics(appCode, startTime, endTime)));
    }

    @GetMapping("/channels")
    @Operation(summary = "渠道分布")
    public ResponseEntity<ApiResponse<List<ChannelBreakdown>>> channels(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) String appCode) {
        return ResponseEntity.ok(ApiResponse.success(service.channels(appCode, startTime, endTime)));
    }

    @GetMapping("/pages")
    @Operation(summary = "页面分布")
    public ResponseEntity<ApiResponse<List<PageBreakdown>>> pages(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) String appCode) {
        return ResponseEntity.ok(ApiResponse.success(service.pages(appCode, startTime, endTime)));
    }

    @GetMapping("/realtime")
    @Operation(summary = "实时快照")
    public ResponseEntity<ApiResponse<RealtimeSnapshot>> realtime(
            @RequestParam(required = false) String appCode) {
        return ResponseEntity.ok(ApiResponse.success(service.realtime(appCode)));
    }

    @GetMapping("/analysis")
    @Operation(summary = "分析概览")
    public ResponseEntity<ApiResponse<AnalysisOverview>> analysis(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) String appCode) {
        return ResponseEntity.ok(ApiResponse.success(service.analysis(appCode, startTime, endTime)));
    }

    @GetMapping("/retention")
    @Operation(summary = "留存")
    public ResponseEntity<ApiResponse<RetentionResult>> retention(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) String appCode) {
        return ResponseEntity.ok(ApiResponse.success(service.retention(appCode, startTime, endTime)));
    }

    @GetMapping("/anomalies")
    @Operation(summary = "异常检测")
    public ResponseEntity<ApiResponse<List<AnomalyItem>>> anomalies(
            @RequestParam String date, @RequestParam(required = false) String appCode) {
        return ResponseEntity.ok(ApiResponse.success(service.anomalies(appCode, date)));
    }
}
