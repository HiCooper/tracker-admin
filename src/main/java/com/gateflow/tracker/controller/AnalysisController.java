package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Analysis API", description = "流量分析接口 — 查询 ClickHouse 事件数据")
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/apps")
    @Operation(summary = "应用指标列表")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAppMetrics(
            @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(ApiResponse.success(
                analysisService.getAppMetrics(params.get("startTime"), params.get("endTime"))));
    }

    @PostMapping("/apps/{appCode}/pages")
    @Operation(summary = "页面分析数据")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPageMetrics(
            @PathVariable String appCode, @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(ApiResponse.success(
                analysisService.getPageMetrics(appCode, params.get("startTime"), params.get("endTime"))));
    }

    @PostMapping("/apps/{appCode}/pages/{pageCode}/blocks")
    @Operation(summary = "区块分析数据")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBlockMetrics(
            @PathVariable String appCode, @PathVariable String pageCode,
            @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(ApiResponse.success(
                analysisService.getBlockMetrics(appCode, pageCode, params.get("startTime"), params.get("endTime"))));
    }

    @PostMapping("/apps/{appCode}/pages/{pageCode}/blocks/{blockCode}/functions")
    @Operation(summary = "功能分析数据")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFunctionMetrics(
            @PathVariable String appCode, @PathVariable String pageCode,
            @PathVariable String blockCode, @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(ApiResponse.success(
                analysisService.getFunctionMetrics(appCode, pageCode, blockCode,
                        params.get("startTime"), params.get("endTime"))));
    }

    @PostMapping("/trend-detail")
    @Operation(summary = "趋势详情")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrendDetail(
            @RequestBody Map<String, Object> params) {
        int days = params.get("days") instanceof Number n ? n.intValue() : 7;
        return ResponseEntity.ok(ApiResponse.success(analysisService.getTrendDetail(days)));
    }
}
