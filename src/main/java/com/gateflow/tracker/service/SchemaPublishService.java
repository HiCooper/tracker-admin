package com.gateflow.tracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.dto.AppSchemaDto;
import com.gateflow.tracker.domain.dto.PlanVO;
import com.gateflow.tracker.domain.entity.TrackerApp;
import com.gateflow.tracker.domain.entity.TrackerPlan;
import com.gateflow.tracker.repository.TrackerAppMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 把上线的埋点方案编译为 app 事件契约并发布到 Redis(tracker:schema:{appCode}),
 * 供采集服务 SchemaRegistry 读取。发布失败不影响方案工作流(best-effort)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaPublishService {

    static final String KEY_PREFIX = "tracker:schema:";

    private final TrackerAppMapper appMapper;
    private final SchemaCompiler compiler;
    private final StringRedisTemplate schemaStringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 发布某方案的契约;成功返回发布所用的 appCode,失败返回 null。 */
    public String publishForPlan(TrackerPlan plan) {
        if (plan == null) {
            return null;
        }
        try {
            String appCode = resolveAppCode(plan.getAppId());
            if (!StringUtils.hasText(appCode)) {
                log.warn("Cannot publish schema: appCode unresolved for plan {} (appId={})",
                        plan.getId(), plan.getAppId());
                return null;
            }
            List<PlanVO.EventItem> events = parseEvents(plan.getEventsJson());
            long version = plan.getUpdatedAt() != null
                    ? plan.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                    : System.currentTimeMillis();
            AppSchemaDto schema = compiler.compile(appCode, version, events);
            String json = objectMapper.writeValueAsString(schema);
            schemaStringRedisTemplate.opsForValue().set(KEY_PREFIX + appCode, json);
            log.info("Published schema for app {} (plan {}, {} events)", appCode, plan.getId(), events.size());
            return appCode;
        } catch (Exception e) {
            log.error("Failed to publish schema for plan {}: {}", plan.getId(), e.getMessage());
            return null;
        }
    }

    private String resolveAppCode(Long appId) {
        if (appId == null) {
            return null;
        }
        TrackerApp app = appMapper.selectById(appId);
        return app != null ? app.getAppCode() : null;
    }

    private List<PlanVO.EventItem> parseEvents(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PlanVO.EventItem>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse plan eventsJson: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
