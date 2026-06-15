package com.gateflow.tracker.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.service.DebugService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DebugWebSocketHandler extends TextWebSocketHandler {

    private final DebugService debugService;
    private final ObjectMapper om = new ObjectMapper();
    
    // sessionId → set of viewer WebSocket sessions
    private final Map<String, Set<WebSocketSession>> viewers = new ConcurrentHashMap<>();
    // sessionId → sdk WebSocket session
    private final Map<String, WebSocketSession> sdkSessions = new ConcurrentHashMap<>();

    public DebugWebSocketHandler(DebugService debugService) {
        this.debugService = debugService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri().getPath();
        String sessionId = extractSessionId(path);
        boolean isViewer = path.contains("/view/");
        boolean isSdk = path.contains("/sdk/");

        if (isViewer) {
            viewers.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("Viewer connected: sessionId={}", sessionId);

            sendJson(session, Map.of("type", "viewer_ready", "sessionId", sessionId));

            // 若该会话的 SDK 已在线,立即告知观看端设备已连接(此前用每连接新建的
            // ScheduledExecutorService 发送 mock 事件,既泄漏线程又是假数据,已移除)。
            if (sdkSessions.containsKey(sessionId)) {
                sendJson(session, Map.of("type", "device_connected", "sessionId", sessionId));
            }

        } else if (isSdk) {
            sdkSessions.put(sessionId, session);
            log.info("SDK connected: sessionId={}", sessionId);

            // Notify viewers that device connected
            for (WebSocketSession vs : viewers.getOrDefault(sessionId, Collections.emptySet())) {
                sendJson(vs, Map.of("type", "device_connected", "sessionId", sessionId));
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String path = session.getUri().getPath();
        String sessionId = extractSessionId(path);
        boolean isSdk = path.contains("/sdk/");

        if (isSdk) {
            // SDK sent an event — forward to viewers
            try {
                Map<String, Object> event = om.readValue(message.getPayload(), Map.class);
                for (WebSocketSession vs : viewers.getOrDefault(sessionId, Collections.emptySet())) {
                    sendJson(vs, event);
                }
            } catch (IOException e) {
                log.warn("Failed to parse SDK message: {}", e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String path = session.getUri().getPath();
        String sessionId = extractSessionId(path);
        boolean isSdk = path.contains("/sdk/");

        if (isSdk) {
            sdkSessions.remove(sessionId);
            for (WebSocketSession vs : viewers.getOrDefault(sessionId, Collections.emptySet())) {
                sendJson(vs, Map.of("type", "device_disconnected", "sessionId", sessionId));
            }
            log.info("SDK disconnected: sessionId={}", sessionId);
        } else {
            Set<WebSocketSession> set = viewers.get(sessionId);
            if (set != null) set.remove(session);
            log.info("Viewer disconnected: sessionId={}", sessionId);
        }
    }

    private String extractSessionId(String path) {
        // /ws/debug/view/{sessionId} or /ws/debug/sdk/{sessionId}
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }

    private void sendJson(WebSocketSession session, Object data) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(om.writeValueAsString(data)));
            } catch (IOException e) {
                log.warn("Failed to send WS message: {}", e.getMessage());
            }
        }
    }
}
