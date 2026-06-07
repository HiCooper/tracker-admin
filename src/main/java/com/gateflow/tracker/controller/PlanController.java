package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.dto.PageResponse.PageData;
import com.gateflow.tracker.service.PlanService;
import com.gateflow.tracker.service.PlanService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ResponseEntity<ApiResponse<PageData<PlanVO>>> listPlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(planService.listPlans(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "方案详情")
    public ResponseEntity<ApiResponse<PlanVO>> getPlan(@PathVariable Long id) {
        PlanVO plan = planService.getPlan(id);
        if (plan == null) {
            return ResponseEntity.ok(ApiResponse.error(4001, "方案不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @PostMapping
    @Operation(summary = "创建方案")
    public ResponseEntity<ApiResponse<PlanVO>> createPlan(@RequestBody PlanCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(planService.createPlan(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新方案")
    public ResponseEntity<ApiResponse<PlanVO>> updatePlan(
            @PathVariable Long id, @RequestBody PlanUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(planService.updatePlan(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除方案")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审核")
    public ResponseEntity<ApiResponse<PlanVO>> submitForReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.submitForReview(id)));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "审核方案")
    public ResponseEntity<ApiResponse<PlanVO>> reviewPlan(
            @PathVariable Long id, @RequestBody PlanReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(planService.reviewPlan(id, request)));
    }

    @PostMapping("/{id}/online")
    @Operation(summary = "上线方案")
    public ResponseEntity<ApiResponse<PlanVO>> goOnline(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.goOnline(id)));
    }
}
