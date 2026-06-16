package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorServiceTest {

    private MonitorService service(String baseUrl) {
        ClickHouseProperties props = new ClickHouseProperties();
        // 指向不可达地址,确保不会误连真实 CH;本测试只覆盖纯逻辑分支
        props.setUrl("jdbc:clickhouse://127.0.0.1:1/nodb");
        return new MonitorService(props, new ObjectMapper(), baseUrl);
    }

    @Test
    void parsesStructuredPipelineSection() {
        MonitorService s = service("");
        Map<String, Object> p = s.parseCollectorPipeline(
                "{\"pipeline\":{\"dlqSize\":12,\"dedupHitRate\":0.75}}");
        assertThat(p).containsEntry("dlqSize", 12L).containsEntry("dedupHitRate", 0.75);
    }

    @Test
    void fallsBackToLegacyServicesDlqString() {
        MonitorService s = service("");
        Map<String, Object> p = s.parseCollectorPipeline("{\"services\":{\"dlq\":\"7 entries\"}}");
        assertThat(p).containsEntry("dlqSize", 7L);
        assertThat(p).doesNotContainKey("dedupHitRate");
    }

    @Test
    void parseCollectorPipelineReturnsNullWhenMissingOrInvalid() {
        MonitorService s = service("");
        assertThat(s.parseCollectorPipeline("{\"services\":{}}")).isNull();
        assertThat(s.parseCollectorPipeline("not json")).isNull();
        assertThat(s.parseCollectorPipeline("{\"services\":{\"dlq\":\"n/a\"}}")).isNull();
    }

    @Test
    void fetchCollectorPipelineReturnsNullWhenNoBaseUrlConfigured() {
        assertThat(service("").fetchCollectorPipeline()).isNull();
    }

    @Test
    void unavailableSectionMarksNotAvailable() {
        Map<String, Object> sec = service("").unavailableSection("x 未接入");
        assertThat(sec).containsEntry("available", false).containsEntry("reason", "x 未接入");
    }

    @Test
    void pipelineMarksCollectorMetricsUnavailableAndDlqNullWithoutCollector() {
        // CH 不可达 → 标量查询返回 null;collector 未配置 → dlq null
        Map<String, Object> p = service("").pipeline();
        assertThat(p).containsEntry("available", false);
        assertThat(p.get("dlqSize")).isNull();
        assertThat(p).containsEntry("collectorMetricsAvailable", false);
        assertThat(p).containsKey("clickhouseRows");
    }

    @Test
    void dataQualityIsUnavailableWhenClickHouseUnreachable() {
        Map<String, Object> q = service("").dataQuality();
        assertThat(q).containsEntry("available", false).containsKey("totalEventsToday");
    }
}
