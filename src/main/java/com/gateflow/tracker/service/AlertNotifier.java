package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.entity.TrackerAlertEvent;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 告警通知:目前支持 webhook(POST JSON)。best-effort,通知失败不影响告警记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotifier {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    public void notify(TrackerAlertRule rule, TrackerAlertEvent event) {
        if (!"webhook".equalsIgnoreCase(rule.getNotifyChannel())
                || !StringUtils.hasText(rule.getNotifyTarget())) {
            return;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(rule.getNotifyTarget()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(payload(event)))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Alert webhook notify failed for rule {}: {}", rule.getId(), e.getMessage());
        }
    }

    /** 构造通知载荷(纯函数,便于测试)。 */
    String payload(TrackerAlertEvent event) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("rule", event.getRuleName());
            body.put("metric", event.getMetric());
            body.put("appCode", event.getAppCode());
            body.put("observed", event.getObserved());
            body.put("threshold", event.getThreshold());
            body.put("message", event.getMessage());
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return "{}";
        }
    }
}
