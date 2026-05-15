package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.SessionAnalysisVO;
import com.gateflow.tracker.service.SessionAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/session-analysis")
@RequiredArgsConstructor
public class SessionAnalysisController {

    private final SessionAnalysisService sessionAnalysisService;

    @GetMapping
    @Operation(summary = "查询Session分析数据")
    public ResponseEntity<ApiResponse<List<SessionAnalysisVO>>> query(
            @Parameter(description = "Session ID") @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<SessionAnalysisVO> result = sessionAnalysisService.query(sessionId, userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近N天的Session分析数据")
    public ResponseEntity<ApiResponse<List<SessionAnalysisVO>>> getRecentData(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") int days) {
        List<SessionAnalysisVO> result = sessionAnalysisService.getRecentData(days);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}