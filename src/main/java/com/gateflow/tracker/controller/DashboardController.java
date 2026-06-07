package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.service.DashboardService;
import com.gateflow.tracker.service.DashboardDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dashboard API", description = "看板管理接口")
@RestController
@RequestMapping("/api/v1/dashboards")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "看板列表")
    public ResponseEntity<ApiResponse<List<DashboardVO>>> listDashboards() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.listDashboards()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取看板详情")
    public ResponseEntity<ApiResponse<DashboardVO>> getDashboard(
            @Parameter(description = "看板ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardById(id)));
    }

    @GetMapping("/{id}/data")
    @Operation(summary = "执行看板查询，返回实时数据")
    public ResponseEntity<ApiResponse<DashboardDataService.DashboardData>> getDashboardData(
            @Parameter(description = "看板ID") @PathVariable Long id,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getDashboardData(id, startTime, endTime)));
    }

    @PostMapping
    @Operation(summary = "创建看板")
    public ResponseEntity<ApiResponse<DashboardVO>> createDashboard(
            @Valid @RequestBody CreateDashboardRequest request) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.createDashboard(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新看板")
    public ResponseEntity<ApiResponse<DashboardVO>> updateDashboard(
            @Parameter(description = "看板ID") @PathVariable Long id,
            @Valid @RequestBody UpdateDashboardRequest request) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.updateDashboard(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除看板")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(
            @Parameter(description = "看板ID") @PathVariable Long id) {
        dashboardService.deleteDashboard(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}