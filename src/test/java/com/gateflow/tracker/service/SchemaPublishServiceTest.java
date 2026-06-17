package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.entity.TrackerApp;
import com.gateflow.tracker.domain.entity.TrackerPlan;
import com.gateflow.tracker.repository.TrackerAppMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class SchemaPublishServiceTest {

    private final TrackerAppMapper appMapper = mock(TrackerAppMapper.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final SchemaPublishService service =
            new SchemaPublishService(appMapper, new SchemaCompiler(), redis, new ObjectMapper());

    private TrackerPlan plan() {
        TrackerPlan p = new TrackerPlan();
        p.setId(1L);
        p.setAppId(10L);
        p.setEventsJson("[{\"eventKey\":\"purchase\",\"properties\":[{\"propKey\":\"orderId\",\"dataType\":\"string\"}]}]");
        return p;
    }

    private void appExists() {
        TrackerApp app = new TrackerApp();
        app.setId(10L);
        app.setAppCode("A_MAIN");
        when(appMapper.selectById(10L)).thenReturn(app);
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void publishesSchemaUnderAppCodeKey() {
        appExists();

        String appCode = service.publishForPlan(plan());

        assertThat(appCode).isEqualTo("A_MAIN");
        verify(valueOps).set(eq("tracker:schema:A_MAIN"), argThat(json ->
                json.contains("\"appId\":\"A_MAIN\"") && json.contains("\"purchase\"")
                        && json.contains("\"orderId\"")));
    }

    @Test
    void returnsNullWhenAppCodeUnresolved() {
        when(appMapper.selectById(10L)).thenReturn(null);
        assertThat(service.publishForPlan(plan())).isNull();
        verifyNoInteractions(valueOps);
    }

    @Test
    void bestEffortSwallowsRedisErrors() {
        appExists();
        doThrow(new RuntimeException("redis down")).when(valueOps).set(anyString(), anyString());
        // 发布失败不得抛出,返回 null
        assertThat(service.publishForPlan(plan())).isNull();
    }

    @Test
    void nullPlanReturnsNull() {
        assertThat(service.publishForPlan(null)).isNull();
    }
}
