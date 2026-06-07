package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.entity.TrackerSdkConfig;
import com.gateflow.tracker.service.SdkConfigService;
import com.gateflow.tracker.service.SdkConfigService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "SDK Config API", description = "SDK远程配置管理")
@RestController
@RequestMapping("/api/v1/sdk-configs")
@RequiredArgsConstructor
public class SdkConfigController {

    private final SdkConfigService configService;

    @GetMapping
    @Operation(summary = "配置列表")
    public ResponseEntity<ApiResponse<List<TrackerSdkConfig>>> list() {
        return ResponseEntity.ok(ApiResponse.success(configService.list()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "配置详情")
    public ResponseEntity<ApiResponse<TrackerSdkConfig>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(configService.get(id)));
    }

    @PostMapping
    @Operation(summary = "创建配置")
    public ResponseEntity<ApiResponse<TrackerSdkConfig>> create(@RequestBody ConfigRequest req) {
        return ResponseEntity.ok(ApiResponse.success(configService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新配置")
    public ResponseEntity<ApiResponse<TrackerSdkConfig>> update(@PathVariable Long id, @RequestBody ConfigRequest req) {
        return ResponseEntity.ok(ApiResponse.success(configService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @GetMapping("/pull")
    @Operation(summary = "SDK拉取配置 — 按appId+platform+version匹配合并")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pull(
            @RequestParam String appId,
            @RequestParam(defaultValue = "web") String platform,
            @RequestParam(defaultValue = "1.0.0") String appVersion) {
        return ResponseEntity.ok(ApiResponse.success(configService.pullConfig(appId, platform, appVersion)));
    }
}
