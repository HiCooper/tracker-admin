package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.security.RequireRole;
import com.gateflow.tracker.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Plan API", description = "埋点需求方案管理接口")
@RestController
@RequestMapping("/api/v1/engineering/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @Operation(summary = "方案列表")
    public ResponseEntity<PageResponse<PlanVO>> listPlans(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "50") Integer size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        return ResponseEntity.ok(planService.listPlans(page, size, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "方案详情")
    public ResponseEntity<ApiResponse<PlanVO>> getPlan(
            @Parameter(description = "方案ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.getPlan(id)));
    }

    @PostMapping
    @Operation(summary = "创建方案")
    public ResponseEntity<ApiResponse<PlanVO>> createPlan(
            @Valid @RequestBody CreatePlanRequest request,
            @RequestAttribute(value = "username", required = false) String username) {
        return ResponseEntity.ok(ApiResponse.success(planService.createPlan(request, username)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新方案")
    public ResponseEntity<ApiResponse<PlanVO>> updatePlan(
            @Parameter(description = "方案ID") @PathVariable Long id,
            @Valid @RequestBody UpdatePlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(planService.updatePlan(id, request)));
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    @Operation(summary = "删除方案")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @Parameter(description = "方案ID") @PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审核")
    public ResponseEntity<ApiResponse<PlanVO>> submitForReview(
            @Parameter(description = "方案ID") @PathVariable Long id,
            @RequestAttribute(value = "username", required = false) String username) {
        return ResponseEntity.ok(ApiResponse.success(planService.submitForReview(id, username)));
    }

    @PostMapping("/{id}/review")
    @RequireRole("admin")
    @Operation(summary = "审核方案")
    public ResponseEntity<ApiResponse<PlanVO>> reviewPlan(
            @Parameter(description = "方案ID") @PathVariable Long id,
            @Valid @RequestBody ReviewPlanRequest request,
            @RequestAttribute(value = "username", required = false) String username) {
        return ResponseEntity.ok(ApiResponse.success(planService.reviewPlan(id, request, username)));
    }

    @PostMapping("/{id}/online")
    @RequireRole("admin")
    @Operation(summary = "上线方案")
    public ResponseEntity<ApiResponse<PlanVO>> goOnline(
            @Parameter(description = "方案ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.goOnline(id)));
    }
}
