package com.gateflow.tracker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricQueryServiceTest {

    @Test
    void appFilterEmptyWhenNoAppCode() {
        assertThat(MetricQueryService.appFilter(null)).isEmpty();
        assertThat(MetricQueryService.appFilter("  ")).isEmpty();
    }

    @Test
    void appFilterAddsClause() {
        assertThat(MetricQueryService.appFilter("A_MAIN"))
                .isEqualTo(" AND app_code = 'A_MAIN'");
    }

    @Test
    void appFilterStripsQuotesToPreventInjection() {
        assertThat(MetricQueryService.appFilter("a' OR '1'='1"))
                .isEqualTo(" AND app_code = 'a OR 1=1'");
    }
}
