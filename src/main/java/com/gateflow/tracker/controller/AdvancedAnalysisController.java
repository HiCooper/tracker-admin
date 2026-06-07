package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.service.AdvancedAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Advanced Analysis API", description = "高级分析接口：漏斗分析、留存分析、用户路径分析")
@RestController
@RequestMapping("/api/v1/advanced-analysis")
@RequiredArgsConstructor
public class AdvancedAnalysisController {

    private final AdvancedAnalysisService advancedAnalysisService;

    @PostMapping("/funnel")
    @Operation(summary = "漏斗分析", description = "配置步骤序列，计算每一步的转化率和流失情况")
    public ResponseEntity<ApiResponse<FunnelResult>> analyzeFunnel(
            @Valid @RequestBody FunnelRequest request) {
        FunnelResult result = advancedAnalysisService.analyzeFunnel(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/retention")
    @Operation(summary = "留存分析", description = "分析用户回访留存率，支持自定义初始事件和回访事件")
    public ResponseEntity<ApiResponse<RetentionResult>> analyzeRetention(
            @Valid @RequestBody RetentionRequest request) {
        RetentionResult result = advancedAnalysisService.analyzeRetention(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/path")
    @Operation(summary = "用户路径分析", description = "分析用户的页面访问路径和跳转关系")
    public ResponseEntity<ApiResponse<PathResult>> analyzePath(
            @Valid @RequestBody PathRequest request) {
        PathResult result = advancedAnalysisService.analyzePath(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
