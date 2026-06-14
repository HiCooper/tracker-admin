package com.gateflow.tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DebugService {

    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public String createSession(String appCode) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        sessions.put(sessionId, appCode);
        log.info("Debug session created: id={}, app={}", sessionId, appCode);
        return sessionId;
    }

    public void closeSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Debug session closed: id={}", sessionId);
    }

    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}
