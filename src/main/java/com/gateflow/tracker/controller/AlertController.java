package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.entity.TrackerAlertRecord;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import com.gateflow.tracker.service.AlertService;
import com.gateflow.tracker.service.AlertService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Alert API", description = "告警规则管理与异常检测")
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/rules")
    @Operation(summary = "告警规则列表")
    public ResponseEntity<ApiResponse<List<TrackerAlertRule>>> listRules() {
        return ResponseEntity.ok(ApiResponse.success(alertService.listRules()));
    }

    @GetMapping("/rules/{id}")
    @Operation(summary = "告警规则详情")
    public ResponseEntity<ApiResponse<TrackerAlertRule>> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(alertService.getRule(id)));
    }

    @PostMapping("/rules")
    @Operation(summary = "创建告警规则")
    public ResponseEntity<ApiResponse<TrackerAlertRule>> createRule(@RequestBody AlertRuleRequest req) {
        return ResponseEntity.ok(ApiResponse.success(alertService.createRule(req)));
    }

    @PutMapping("/rules/{id}")
    @Operation(summary = "更新告警规则")
    public ResponseEntity<ApiResponse<TrackerAlertRule>> updateRule(
            @PathVariable Long id, @RequestBody AlertRuleRequest req) {
        return ResponseEntity.ok(ApiResponse.success(alertService.updateRule(id, req)));
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除告警规则")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        alertService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @PostMapping("/rules/{id}/toggle")
    @Operation(summary = "启用/禁用告警规则")
    public ResponseEntity<ApiResponse<String>> toggleRule(
            @PathVariable Long id, @RequestParam boolean enable) {
        alertService.toggleRule(id, enable);
        return ResponseEntity.ok(ApiResponse.success(enable ? "enabled" : "disabled"));
    }

    @PostMapping("/check")
    @Operation(summary = "执行所有告警规则检查")
    public ResponseEntity<ApiResponse<List<AlertCheckResult>>> checkAll() {
        return ResponseEntity.ok(ApiResponse.success(alertService.checkAll()));
    }

    @GetMapping("/records")
    @Operation(summary = "告警记录列表")
    public ResponseEntity<ApiResponse<List<TrackerAlertRecord>>> listRecords(
            @RequestParam(required = false) Long ruleId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(alertService.listRecords(ruleId, limit)));
    }
}
