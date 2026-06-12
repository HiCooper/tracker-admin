package com.gateflow.tracker.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.service.DebugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebugWebSocketHandler extends TextWebSocketHandler {

    private final DebugService debugService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = extractSessionId(session);
        String role = extractRole(session);
        if (sessionId == null) {
            close(session, CloseStatus.BAD_DATA);
            return;
        }
        DebugService.DebugSession debugSession = debugService.getSession(sessionId);
        if (debugSession == null) {
            close(session, CloseStatus.NOT_ACCEPTABLE.withReason("Session not found"));
            return;
        }

        session.getAttributes().put("sessionId", sessionId);
        session.getAttributes().put("role", role);

        if ("sdk".equals(role)) {
            debugService.addSdkConnection(sessionId, session);
            log.info("SDK connected to debug session: {}", sessionId);
            debugService.broadcastToViewers(sessionId, Map.of(
                    "type", "device_connected",
                    "sessionId", sessionId,
                    "timestamp", System.currentTimeMillis()
            ));
        } else {
            debugService.addViewerConnection(sessionId, session);
            log.info("Viewer connected to debug session: {}", sessionId);
            for (Map<String, Object> event : debugService.getSessionEvents(sessionId)) {
                sendJson(session, event);
            }
            boolean hasSdk = debugService.hasSdkConnection(sessionId);
            sendJson(session, Map.of(
                    "type", "viewer_ready",
                    "sessionId", sessionId,
                    "sdkConnected", hasSdk,
                    "bufferedCount", debugService.getSessionEvents(sessionId).size()
            ));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = (String) session.getAttributes().get("sessionId");
        String role = (String) session.getAttributes().get("role");
        if (sessionId == null) return;

        try {
            if ("sdk".equals(role)) {
                Map<String, Object> event = objectMapper.readValue(message.getPayload(), Map.class);
                event.put("_receivedAt", System.currentTimeMillis());
                debugService.addEvent(sessionId, event);
                debugService.broadcastToViewers(sessionId, event);
            }
        } catch (Exception e) {
            log.warn("WS message error session {}: {}", sessionId, e.getMessage());
            sendJson(session, Map.of("type", "error", "message", e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = (String) session.getAttributes().get("sessionId");
        String role = (String) session.getAttributes().get("role");
        if (sessionId == null) return;

        if ("sdk".equals(role)) {
            debugService.removeSdkConnection(sessionId);
            debugService.broadcastToViewers(sessionId, Map.of(
                    "type", "device_disconnected",
                    "sessionId", sessionId,
                    "timestamp", System.currentTimeMillis()
            ));
        } else {
            debugService.removeViewerConnection(sessionId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable error) {
        log.error("WS transport error: {}", error.getMessage());
        try { session.close(CloseStatus.SERVER_ERROR); } catch (IOException ignored) {}
    }

    private String extractSessionId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String[] parts = uri.getPath().split("/");
        return parts.length >= 4 ? parts[parts.length - 1] : null;
    }

    private String extractRole(WebSocketSession session) {
        URI uri = session.getUri();
        return uri != null && uri.getPath().contains("/sdk/") ? "sdk" : "viewer";
    }

    private void sendJson(WebSocketSession session, Object data) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
            }
        } catch (IOException e) {
            log.warn("Failed to send: {}", e.getMessage());
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try { session.close(status); } catch (IOException ignored) {}
    }
}
