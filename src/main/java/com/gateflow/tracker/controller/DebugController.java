package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.service.DebugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Debug API", description = "实时调试接口")
@RestController
@RequestMapping("/api/v1/engineering/debug")
@RequiredArgsConstructor
public class DebugController {

    private final DebugService debugService;

    @PostMapping("/sessions")
    @Operation(summary = "创建调试会话")
    public ResponseEntity<ApiResponse<Map<String, String>>> createSession(@RequestBody Map<String, String> body) {
        String appCode = body.getOrDefault("appCode", "unknown");
        String sessionId = debugService.createSession(appCode);
        return ResponseEntity.ok(ApiResponse.success(Map.of("sessionId", sessionId)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "结束调试会话")
    public ResponseEntity<ApiResponse<Void>> endSession(@PathVariable String sessionId) {
        debugService.closeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Closed", null));
    }
}
