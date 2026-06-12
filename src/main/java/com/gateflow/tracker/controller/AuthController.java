package com.gateflow.tracker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.domain.entity.TrackerSpm;
import com.gateflow.tracker.repository.TrackerSpmMapper;
import com.gateflow.tracker.security.AuthService;
import com.gateflow.tracker.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth API", description = "用户认证 + SDK Token 签发")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TrackerSpmMapper spmMapper;

    @Data
    public static class LoginRequest { private String username; private String password; }

    @Data
    public static class SdkTokenRequest { private String appCode; }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        var result = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "token", result.token(), "username", result.username(), "role", result.role())));
    }

    /**
     * 获取 SDK 采集令牌。两种方式：
     * 1. Authorization: Bearer <user JWT> → 管理后台页面使用
     * 2. X-Api-Key: <appKey> → 业务后端服务器使用（apiKey 服务端持有，不暴露给浏览器）
     */
    @PostMapping("/sdk-token")
    @Operation(summary = "获取 SDK 采集令牌。apiKey 由业务后端服务端持有，不暴露给浏览器。")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSdkToken(
            @RequestBody SdkTokenRequest request,
            Authentication auth,
            HttpServletRequest servletRequest) {

        String appCode = request.getAppCode();
        if (appCode == null || appCode.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 方式 1: user JWT（管理后台页面已登录）
        if (auth != null && auth.isAuthenticated()) {
            return ok(jwtUtil.generateSdkToken(appCode), appCode);
        }

        // 方式 2: X-Api-Key（业务后端服务器调用）
        String apiKey = servletRequest.getHeader("X-Api-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            var spm = spmMapper.selectOne(new LambdaQueryWrapper<TrackerSpm>()
                    .eq(TrackerSpm::getAppKey, apiKey).last("LIMIT 1"));
            if (spm != null && spm.getAppKey() != null) {
                return ok(jwtUtil.generateSdkToken(spm.getSpmCode()), spm.getSpmCode());
            }
            return ResponseEntity.status(401).body(ApiResponse.error(401, "Invalid apiKey"));
        }

        return ResponseEntity.status(401).body(ApiResponse.error(401, "Authentication required"));
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> ok(String token, String appCode) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "sdkToken", token, "appCode", appCode, "expiresIn", "3600")));
    }
}
