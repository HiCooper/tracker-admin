package com.gateflow.tracker.controller;

import com.gateflow.tracker.domain.dto.ApiResponse;
import com.gateflow.tracker.service.DebugService;
import com.gateflow.tracker.service.DebugService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Debug API", description = "埋点实时验证接口")
@RestController
@RequestMapping("/api/v1/engineering/debug")
@RequiredArgsConstructor
public class DebugController {

    private final DebugService debugService;

    @PostMapping("/sessions")
    @Operation(summary = "创建实时验证会话")
    public ResponseEntity<ApiResponse<DebugSession>> createSession(
            @RequestBody CreateSessionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(debugService.createSession(request)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "结束验证会话")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable String sessionId) {
        debugService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
