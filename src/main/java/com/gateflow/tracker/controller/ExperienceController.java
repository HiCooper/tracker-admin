package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.experience.ExperienceDto.*;
import com.gateflow.tracker.service.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Experience API", description = "体验分析:热力图/画像/会话/转化")
@RestController
@RequestMapping("/api/v1/experience")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService service;

    @GetMapping("/heatmap")
    @Operation(summary = "点击热力图")
    public ResponseEntity<ApiResponse<HeatmapData>> heatmap(
            @RequestParam String appCode, @RequestParam String pageUrl,
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(defaultValue = "click") String type,
            @RequestParam(required = false) Integer bucketSize) {
        return ResponseEntity.ok(ApiResponse.success(
                service.heatmap(appCode, pageUrl, startTime, endTime, type, bucketSize)));
    }

    @GetMapping("/portrait")
    @Operation(summary = "用户画像")
    public ResponseEntity<ApiResponse<UserPortrait>> portrait(
            @RequestParam String appCode, @RequestParam String startTime, @RequestParam String endTime) {
        return ResponseEntity.ok(ApiResponse.success(service.portrait(appCode, startTime, endTime)));
    }

    @GetMapping("/pages")
    @Operation(summary = "页面列表")
    public ResponseEntity<ApiResponse<List<PageListItem>>> pages(
            @RequestParam String appCode, @RequestParam String startTime, @RequestParam String endTime) {
        return ResponseEntity.ok(ApiResponse.success(service.pages(appCode, startTime, endTime)));
    }

    @GetMapping("/sessions")
    @Operation(summary = "会话列表")
    public ResponseEntity<ApiResponse<List<SessionRecord>>> sessions(
            @RequestParam String startTime, @RequestParam String endTime) {
        return ResponseEntity.ok(ApiResponse.success(service.sessions(startTime, endTime)));
    }

    @GetMapping("/conversion")
    @Operation(summary = "转化分析")
    public ResponseEntity<ApiResponse<List<ConversionStep>>> conversion(
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) String goal) {
        return ResponseEntity.ok(ApiResponse.success(service.conversion(startTime, endTime, goal)));
    }

    @GetMapping("/reports")
    @Operation(summary = "分析报告列表")
    public ResponseEntity<ApiResponse<List<AnalysisReport>>> reports() {
        return ResponseEntity.ok(ApiResponse.success(service.reports()));
    }
}
