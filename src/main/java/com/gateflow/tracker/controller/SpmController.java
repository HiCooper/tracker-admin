package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.service.SpmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gateflow.tracker.security.RequireRole;

import java.util.List;

@Tag(name = "SPM API", description = "SPM管理接口")
@RestController
@RequestMapping("/api/v1/spm")
@RequiredArgsConstructor
public class SpmController {

    private final SpmService spmService;

    @GetMapping
    @Operation(summary = "SPM列表")
    public ResponseEntity<ApiResponse<List<SpmVO>>> listSpms() {
        return ResponseEntity.ok(ApiResponse.success(spmService.listSpms()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取SPM详情")
    public ResponseEntity<ApiResponse<SpmVO>> getSpm(
            @Parameter(description = "SPM ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(spmService.getSpmById(id)));
    }

    @PostMapping
    @Operation(summary = "创建SPM")
    public ResponseEntity<ApiResponse<SpmVO>> createSpm(
            @Valid @RequestBody CreateSpmRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spmService.createSpm(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新SPM")
    public ResponseEntity<ApiResponse<SpmVO>> updateSpm(
            @Parameter(description = "SPM ID") @PathVariable Long id,
            @Valid @RequestBody CreateSpmRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spmService.updateSpm(id, request)));
    }

    @RequireRole("admin")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除SPM")
    public ResponseEntity<ApiResponse<Void>> deleteSpm(
            @Parameter(description = "SPM ID") @PathVariable Long id) {
        spmService.deleteSpm(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}