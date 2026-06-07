package com.gateflow.tracker.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebugService {

    private final Map<String, DebugSession> sessions = new ConcurrentHashMap<>();

    @Data
    public static class DebugSession {
        private String sessionId;
        private String deviceId;
        private String userId;
        private String platform;
        private String startTime;
        private int eventCount;
    }

    @Data
    public static class CreateSessionRequest {
        private String deviceId;
        private String userId;
    }

    public DebugSession createSession(CreateSessionRequest req) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        DebugSession session = new DebugSession();
        session.setSessionId(sessionId);
        session.setDeviceId(req.getDeviceId() != null ? req.getDeviceId() : "device_" + sessionId);
        session.setUserId(req.getUserId() != null ? req.getUserId() : "test_user");
        session.setPlatform("web");
        session.setStartTime(Instant.now().toString());
        session.setEventCount(0);
        sessions.put(sessionId, session);
        log.info("Debug session created: {}", sessionId);
        return session;
    }

    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Debug session deleted: {}", sessionId);
    }
}
