package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.security.RequireRole;
import com.gateflow.tracker.service.AdminPrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Privacy API", description = "隐私合规——被遗忘权")
@RestController
@RequestMapping("/api/v1/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final AdminPrivacyService privacyService;

    @DeleteMapping("/users/{userId}")
    @RequireRole("admin")
    @Operation(summary = "删除用户全部埋点数据(被遗忘权,仅 admin)")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @Parameter(description = "用户ID") @PathVariable String userId) {
        boolean ok = privacyService.deleteUserData(userId);
        return ResponseEntity.ok(ApiResponse.success(ok ? "submitted" : "failed",
                ok ? userId : null));
    }
}
