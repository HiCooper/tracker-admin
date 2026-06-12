package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.service.DebugService;
import com.gateflow.tracker.service.DebugService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Debug API", description = "埋点实时验证接口 — QR + WebSocket")
@RestController
@RequestMapping("/api/v1/engineering/debug")
@RequiredArgsConstructor
public class DebugController {

    private final DebugService debugService;

    @PostMapping("/sessions")
    @Operation(summary = "创建实时验证会话，返回 sessionId 供生成二维码")
    public ResponseEntity<ApiResponse<DebugSession>> createSession(
            @RequestBody CreateSessionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(debugService.createSession(request)));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "获取会话状态")
    public ResponseEntity<ApiResponse<DebugSession>> getSession(@PathVariable String sessionId) {
        DebugSession session = debugService.getSession(sessionId);
        if (session == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @GetMapping("/sessions/{sessionId}/events")
    @Operation(summary = "获取会话事件（轮询备用，优先 WebSocket）")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEvents(
            @PathVariable String sessionId) {
        if (debugService.getSession(sessionId) == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(debugService.getSessionEvents(sessionId)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "结束验证会话，断开所有连接")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable String sessionId) {
        debugService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session closed", null));
    }
}
