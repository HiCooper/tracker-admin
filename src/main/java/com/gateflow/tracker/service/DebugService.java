package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebugService {

    private final Map<String, DebugSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_EVENTS = 500;

    @Data
    public static class DebugSession {
        private String sessionId;
        private String deviceId;
        private String userId;
        private String appCode;
        private String platform;
        private String startTime;
        private int eventCount;
        private final Set<WebSocketSession> sdkConnections = ConcurrentHashMap.newKeySet();
        private final Set<WebSocketSession> viewerConnections = ConcurrentHashMap.newKeySet();
        private final Deque<Map<String, Object>> eventBuffer = new ConcurrentLinkedDeque<>();
    }

    @Data
    public static class CreateSessionRequest {
        private String deviceId;
        private String userId;
        private String appCode;
    }

    public DebugSession createSession(CreateSessionRequest req) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        DebugSession session = new DebugSession();
        session.setSessionId(sessionId);
        session.setDeviceId(req.getDeviceId() != null ? req.getDeviceId() : "device_" + sessionId);
        session.setUserId(req.getUserId() != null ? req.getUserId() : "test_user");
        session.setAppCode(req.getAppCode());
        session.setPlatform("web");
        session.setStartTime(Instant.now().toString());
        session.setEventCount(0);
        sessions.put(sessionId, session);
        log.info("Debug session created: {} app={}", sessionId, req.getAppCode());
        return session;
    }

    public DebugSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void deleteSession(String sessionId) {
        DebugSession session = sessions.remove(sessionId);
        if (session != null) {
            closeAllConnections(session);
            log.info("Debug session closed: {}", sessionId);
        }
    }

    // ============ WebSocket connection management ============

    public void addSdkConnection(String sessionId, WebSocketSession ws) {
        DebugSession session = sessions.get(sessionId);
        if (session != null) session.getSdkConnections().add(ws);
    }

    public void removeSdkConnection(String sessionId) {
        DebugSession session = sessions.get(sessionId);
        if (session != null) session.getSdkConnections().removeIf(ws -> !ws.isOpen());
    }

    public boolean hasSdkConnection(String sessionId) {
        DebugSession session = sessions.get(sessionId);
        return session != null && !session.getSdkConnections().isEmpty();
    }

    public void addViewerConnection(String sessionId, WebSocketSession ws) {
        DebugSession session = sessions.get(sessionId);
        if (session != null) session.getViewerConnections().add(ws);
    }

    public void removeViewerConnection(String sessionId) {
        DebugSession session = sessions.get(sessionId);
        if (session != null) session.getViewerConnections().removeIf(ws -> !ws.isOpen());
    }

    // ============ Event relay ============

    public void addEvent(String sessionId, Map<String, Object> event) {
        DebugSession session = sessions.get(sessionId);
        if (session == null) return;
        session.getEventBuffer().addLast(event);
        if (session.getEventBuffer().size() > MAX_EVENTS) {
            session.getEventBuffer().removeFirst();
        }
        session.setEventCount(session.getEventCount() + 1);
    }

    public List<Map<String, Object>> getSessionEvents(String sessionId) {
        DebugSession session = sessions.get(sessionId);
        if (session == null) return List.of();
        return new ArrayList<>(session.getEventBuffer());
    }

    public void broadcastToViewers(String sessionId, Map<String, Object> data) {
        DebugSession session = sessions.get(sessionId);
        if (session == null) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Failed to serialize broadcast: {}", e.getMessage());
            return;
        }
        TextMessage msg = new TextMessage(json);
        session.getViewerConnections().removeIf(ws -> {
            if (!ws.isOpen()) return true;
            try {
                ws.sendMessage(msg);
                return false;
            } catch (IOException e) {
                return true;
            }
        });
    }

    // ============ cleanup ============

    private void closeAllConnections(DebugSession session) {
        broadcastToViewers(session.getSessionId(), Map.of(
                "type", "session_closed",
                "sessionId", session.getSessionId()
        ));
        session.getSdkConnections().removeIf(ws -> { try { if (ws.isOpen()) ws.close(); } catch (Exception ignored) {} return true; });
        session.getViewerConnections().removeIf(ws -> { try { if (ws.isOpen()) ws.close(); } catch (Exception ignored) {} return true; });
    }
}
