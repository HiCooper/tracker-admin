package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gateflow.tracker.security.RequireRole;

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

    @RequireRole("admin")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除看板")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(
            @Parameter(description = "看板ID") @PathVariable Long id) {
        dashboardService.deleteDashboard(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}