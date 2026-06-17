package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.entity.TrackerAlertEvent;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import com.gateflow.tracker.security.RequireRole;
import com.gateflow.tracker.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Alert API", description = "告警规则与历史")
@RestController
@RequestMapping("/api/v1/monitor/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/rules")
    @Operation(summary = "告警规则列表")
    public ResponseEntity<ApiResponse<List<TrackerAlertRule>>> listRules() {
        return ResponseEntity.ok(ApiResponse.success(alertService.listRules()));
    }

    @PostMapping("/rules")
    @RequireRole("admin")
    @Operation(summary = "创建告警规则")
    public ResponseEntity<ApiResponse<TrackerAlertRule>> createRule(@RequestBody TrackerAlertRule rule) {
        return ResponseEntity.ok(ApiResponse.success(alertService.createRule(rule)));
    }

    @PutMapping("/rules/{id}")
    @RequireRole("admin")
    @Operation(summary = "更新告警规则")
    public ResponseEntity<ApiResponse<TrackerAlertRule>> updateRule(
            @PathVariable Long id, @RequestBody TrackerAlertRule rule) {
        return ResponseEntity.ok(ApiResponse.success(alertService.updateRule(id, rule)));
    }

    @DeleteMapping("/rules/{id}")
    @RequireRole("admin")
    @Operation(summary = "删除告警规则")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        alertService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("deleted", null));
    }

    @GetMapping("/events")
    @Operation(summary = "告警触发历史")
    public ResponseEntity<ApiResponse<List<TrackerAlertEvent>>> events(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.success(alertService.listEvents(limit)));
    }
}
