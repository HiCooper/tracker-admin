package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.service.SetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gateflow.tracker.security.RequireRole;

import java.util.List;

@Tag(name = "Setup API", description = "埋点管理 — App/Page/Block/Function 四级层级接口")
@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;

    // ── Apps ───────────────────────────────────────────────────

    @GetMapping("/apps")
    @Operation(summary = "应用列表")
    public ResponseEntity<ApiResponse<List<AppVO>>> listApps() {
        return ResponseEntity.ok(ApiResponse.success(setupService.listApps()));
    }

    @GetMapping("/apps/{id}")
    @Operation(summary = "获取应用详情")
    public ResponseEntity<ApiResponse<AppVO>> getApp(
            @Parameter(description = "应用ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(setupService.getApp(id)));
    }

    @PostMapping("/apps")
    @Operation(summary = "创建应用")
    public ResponseEntity<ApiResponse<AppVO>> createApp(
            @Valid @RequestBody CreateAppRequest request) {
        return ResponseEntity.ok(ApiResponse.success(setupService.createApp(request)));
    }

    @RequireRole("admin")
    @DeleteMapping("/apps/{id}")
    @Operation(summary = "删除应用")
    public ResponseEntity<ApiResponse<Void>> deleteApp(
            @Parameter(description = "应用ID") @PathVariable Long id) {
        setupService.deleteApp(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    // ── Pages ───────────────────────────────────────────────────

    @GetMapping("/apps/{appId}/pages")
    @Operation(summary = "页面列表")
    public ResponseEntity<ApiResponse<List<PageVO>>> listPages(
            @Parameter(description = "应用ID") @PathVariable Long appId) {
        return ResponseEntity.ok(ApiResponse.success(setupService.listPages(appId)));
    }

    @PostMapping("/apps/{appId}/pages")
    @Operation(summary = "创建页面")
    public ResponseEntity<ApiResponse<PageVO>> createPage(
            @Parameter(description = "应用ID") @PathVariable Long appId,
            @Valid @RequestBody CreatePageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(setupService.createPage(appId, request)));
    }

    @RequireRole("admin")
    @DeleteMapping("/pages/{id}")
    @Operation(summary = "删除页面")
    public ResponseEntity<ApiResponse<Void>> deletePage(
            @Parameter(description = "页面ID") @PathVariable Long id) {
        setupService.deletePage(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    // ── Blocks ──────────────────────────────────────────────────

    @GetMapping("/pages/{pageId}/blocks")
    @Operation(summary = "模块列表")
    public ResponseEntity<ApiResponse<List<BlockVO>>> listBlocks(
            @Parameter(description = "页面ID") @PathVariable Long pageId) {
        return ResponseEntity.ok(ApiResponse.success(setupService.listBlocks(pageId)));
    }

    @PostMapping("/pages/{pageId}/blocks")
    @Operation(summary = "创建模块")
    public ResponseEntity<ApiResponse<BlockVO>> createBlock(
            @Parameter(description = "页面ID") @PathVariable Long pageId,
            @Valid @RequestBody CreateBlockRequest request) {
        return ResponseEntity.ok(ApiResponse.success(setupService.createBlock(pageId, request)));
    }

    @RequireRole("admin")
    @DeleteMapping("/blocks/{id}")
    @Operation(summary = "删除模块")
    public ResponseEntity<ApiResponse<Void>> deleteBlock(
            @Parameter(description = "模块ID") @PathVariable Long id) {
        setupService.deleteBlock(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    // ── Functions ───────────────────────────────────────────────

    @GetMapping("/blocks/{blockId}/functions")
    @Operation(summary = "功能列表")
    public ResponseEntity<ApiResponse<List<FunctionVO>>> listFunctions(
            @Parameter(description = "模块ID") @PathVariable Long blockId) {
        return ResponseEntity.ok(ApiResponse.success(setupService.listFunctions(blockId)));
    }

    @PostMapping("/blocks/{blockId}/functions")
    @Operation(summary = "创建功能")
    public ResponseEntity<ApiResponse<FunctionVO>> createFunction(
            @Parameter(description = "模块ID") @PathVariable Long blockId,
            @Valid @RequestBody CreateFunctionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(setupService.createFunction(blockId, request)));
    }

    @RequireRole("admin")
    @DeleteMapping("/functions/{id}")
    @Operation(summary = "删除功能")
    public ResponseEntity<ApiResponse<Void>> deleteFunction(
            @Parameter(description = "功能ID") @PathVariable Long id) {
        setupService.deleteFunction(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
