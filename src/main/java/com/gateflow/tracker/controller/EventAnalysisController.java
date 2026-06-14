package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.EventAnalysisQueryRequest;
import com.gateflow.tracker.domain.dto.EventAnalysisVO;
import com.gateflow.tracker.service.EventAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/event-analysis")
@RequiredArgsConstructor
public class EventAnalysisController {

    private final EventAnalysisService eventAnalysisService;

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping
    @Operation(summary = "查询事件分析数据")
    public ResponseEntity<ApiResponse<List<EventAnalysisVO>>> query(
            @Parameter(description = "事件标识") @RequestParam(required = false) String eventKey,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "平台") @RequestParam(required = false) String platform) {
        
        EventAnalysisQueryRequest request = new EventAnalysisQueryRequest();
        request.setEventKey(eventKey);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPlatform(platform);
        
        List<EventAnalysisVO> result = eventAnalysisService.query(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近N天的事件分析数据")
    public ResponseEntity<ApiResponse<List<EventAnalysisVO>>> getRecentData(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") int days) {
        List<EventAnalysisVO> result = eventAnalysisService.getRecentData(days);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}