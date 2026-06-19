package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.behavior.BehaviorDto.*;
import com.gateflow.tracker.service.BehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Behavior API", description = "行为分析:概览/事件/漏斗/路径/留存")
@RestController
@RequestMapping("/api/v1/behavior")
@RequiredArgsConstructor
public class BehaviorController {

    private final BehaviorService service;

    @GetMapping("/overview")
    @Operation(summary = "行为概览")
    public ResponseEntity<ApiResponse<BehaviorOverview>> overview(
            @RequestParam String startTime, @RequestParam String endTime) {
        return ResponseEntity.ok(ApiResponse.success(service.overview(startTime, endTime)));
    }

    @GetMapping("/events")
    @Operation(summary = "事件分析")
    public ResponseEntity<ApiResponse<List<EventSummary>>> events(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) List<String> eventTypes) {
        return ResponseEntity.ok(ApiResponse.success(service.events(startTime, endTime, eventTypes)));
    }

    @GetMapping("/funnel")
    @Operation(summary = "漏斗分析")
    public ResponseEntity<ApiResponse<FunnelData>> funnel(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) List<String> steps) {
        return ResponseEntity.ok(ApiResponse.success(service.funnel(startTime, endTime, steps)));
    }

    @GetMapping("/path")
    @Operation(summary = "路径分析")
    public ResponseEntity<ApiResponse<PathData>> path(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) String startPage,
            @RequestParam(required = false) Integer depth) {
        return ResponseEntity.ok(ApiResponse.success(service.path(startTime, endTime, startPage, depth)));
    }

    @GetMapping("/retention")
    @Operation(summary = "留存分析")
    public ResponseEntity<ApiResponse<RetentionData>> retention(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) String initialEvent,
            @RequestParam(required = false) String returnEvent) {
        return ResponseEntity.ok(ApiResponse.success(service.retention(startTime, endTime, initialEvent, returnEvent)));
    }
}
