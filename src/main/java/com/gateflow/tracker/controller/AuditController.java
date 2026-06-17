package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.PageResponse;
import com.gateflow.tracker.domain.entity.TrackerAuditLog;
import com.gateflow.tracker.security.RequireRole;
import com.gateflow.tracker.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Audit API", description = "管理操作审计日志")
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    @RequireRole("admin")
    @Operation(summary = "审计日志列表(仅 admin)")
    public ResponseEntity<PageResponse<TrackerAuditLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String username) {
        return ResponseEntity.ok(auditLogService.query(page, size, username));
    }
}
