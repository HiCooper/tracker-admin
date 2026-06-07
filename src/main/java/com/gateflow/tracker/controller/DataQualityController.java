package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.service.DataQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Data Quality API", description = "数据质量监控")
@RestController
@RequestMapping("/api/v1/data-quality")
@RequiredArgsConstructor
public class DataQualityController {

    private final DataQualityService dataQualityService;

    @GetMapping("/field-null-rates")
    @Operation(summary = "字段空值率检查")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> fieldNullRates() {
        return ResponseEntity.ok(ApiResponse.success(dataQualityService.fieldNullRates()));
    }

    @GetMapping("/volume-comparison")
    @Operation(summary = "今日/昨日事件量对比")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> volumeComparison() {
        return ResponseEntity.ok(ApiResponse.success(dataQualityService.volumeComparison()));
    }

    @GetMapping("/summary")
    @Operation(summary = "数据质量概览")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiResponse.success(dataQualityService.summary()));
    }
}
