package com.gateflow.tracker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.entity.TrackerApp;
import com.gateflow.tracker.repository.TrackerAppMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Internal API for tracker-service — NOT exposed to the public.
 * Called by tracker-service to validate appKey and fetch appSecret.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final TrackerAppMapper appMapper;

    @Value("${gateflow.jwt.secret}")
    private String jwtSecret;

    /**
     * Validate an appKey (appCode). Returns 200 with appCode if the app exists,
     * 404 if not.  Called by tracker-service AuthController.
     */
    @GetMapping("/app-key/{appKey}")
    public ResponseEntity<Map<String, String>> getAppKey(@PathVariable String appKey) {
        TrackerApp app = appMapper.selectOne(
                new LambdaQueryWrapper<TrackerApp>().eq(TrackerApp::getAppCode, appKey));

        if (app == null) {
            log.warn("Internal app-key lookup failed: {}", appKey);
            return ResponseEntity.notFound().build();
        }

        log.debug("Internal app-key OK: {} -> {}", appKey, app.getAppName());
        return ResponseEntity.ok(Map.of(
                "appCode", app.getAppCode(),
                "appName", app.getAppName()));
    }

    /**
     * Return a derived appSecret for HMAC signing.
     * The secret is HMAC-SHA256(appCode, jwtSecret) — deterministic, no storage needed.
     * Called by tracker-service SignatureVerifier.
     */
    @GetMapping("/app-secret/{appKey}")
    public ResponseEntity<Map<String, String>> getAppSecret(@PathVariable String appKey) {
        TrackerApp app = appMapper.selectOne(
                new LambdaQueryWrapper<TrackerApp>().eq(TrackerApp::getAppCode, appKey));

        if (app == null) {
            return ResponseEntity.notFound().build();
        }

        String secret = deriveSecret(appKey);
        return ResponseEntity.ok(Map.of(
                "appSecret", secret,
                "appCode", app.getAppCode()));
    }

    private String deriveSecret(String appKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(appKey.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive secret for " + appKey, e);
        }
    }
}
