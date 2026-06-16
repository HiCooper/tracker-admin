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
    void parseDlqSizeExtractsLeadingNumber() {
        MonitorService s = service("");
        assertThat(s.parseDlqSize("{\"services\":{\"dlq\":\"12 entries\"}}")).isEqualTo(12L);
        assertThat(s.parseDlqSize("{\"services\":{\"dlq\":\"0 entries\"}}")).isEqualTo(0L);
    }

    @Test
    void parseDlqSizeReturnsNullWhenMissingOrInvalid() {
        MonitorService s = service("");
        assertThat(s.parseDlqSize("{\"services\":{}}")).isNull();
        assertThat(s.parseDlqSize("not json")).isNull();
        assertThat(s.parseDlqSize("{\"services\":{\"dlq\":\"n/a\"}}")).isNull();
    }

    @Test
    void fetchDlqSizeReturnsNullWhenNoBaseUrlConfigured() {
        assertThat(service("").fetchDlqSizeFromCollector()).isNull();
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
