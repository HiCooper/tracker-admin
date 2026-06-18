package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.advanced.FunnelDto;
import com.gateflow.tracker.domain.dto.advanced.PathDto;
import com.gateflow.tracker.domain.dto.advanced.RetentionDto;
import com.gateflow.tracker.service.AdvancedAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Advanced Analysis API", description = "高级行为分析:漏斗 / 留存 / 路径")
@RestController
@RequestMapping("/api/v1/advanced-analysis")
@RequiredArgsConstructor
public class AdvancedAnalysisController {

    private final AdvancedAnalysisService service;

    @PostMapping("/funnel")
    @Operation(summary = "漏斗分析")
    public ResponseEntity<ApiResponse<FunnelDto.Result>> funnel(@RequestBody FunnelDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success(service.funnel(req)));
    }

    @PostMapping("/retention")
    @Operation(summary = "留存分析")
    public ResponseEntity<ApiResponse<RetentionDto.Result>> retention(@RequestBody RetentionDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success(service.retention(req)));
    }

    @PostMapping("/path")
    @Operation(summary = "路径分析")
    public ResponseEntity<ApiResponse<PathDto.Result>> path(@RequestBody PathDto.Request req) {
        return ResponseEntity.ok(ApiResponse.success(service.path(req)));
    }
}
