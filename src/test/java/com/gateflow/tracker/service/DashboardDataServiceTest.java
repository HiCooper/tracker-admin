package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.platform.PlatformDto.CoreMetrics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardDataServiceTest {

    private final DashboardService dashboardService = mock(DashboardService.class);
    private final PlatformDataService platformData = mock(PlatformDataService.class);

    private DashboardDataService service() {
        ClickHouseProperties props = new ClickHouseProperties();
        props.setUrl("jdbc:clickhouse://127.0.0.1:1/nodb");
        return new DashboardDataService(dashboardService, platformData, new ObjectMapper(), props);
    }

    @Test
    void parsesWidgetsFromWrappedObject() {
        var s = service();
        assertThat(s.parseWidgets("{\"widgets\":[{\"id\":\"w1\",\"type\":\"stat\"},{\"id\":\"w2\",\"type\":\"line\"}]}"))
                .hasSize(2);
    }

    @Test
    void parsesWidgetsFromTopLevelArray() {
        assertThat(service().parseWidgets("[{\"id\":\"a\",\"type\":\"pie\"}]")).hasSize(1);
    }

    @Test
    void parsesPanelsAlias() {
        assertThat(service().parseWidgets("{\"panels\":[{\"id\":\"p1\"}]}")).hasSize(1);
    }

    @Test
    void invalidConfigReturnsEmpty() {
        assertThat(service().parseWidgets("not json")).isEmpty();
        assertThat(service().parseWidgets("")).isEmpty();
        assertThat(service().parseWidgets(null)).isEmpty();
    }

    @Test
    void scalarMetricRoutesToCoreMetrics() {
        when(platformData.coreMetrics("s", "e"))
                .thenReturn(new CoreMetrics(100, 30, 500, 12, 45.5, 3.2, 22.1, 4.4));
        var s = service();
        assertThat(s.scalarMetric("uv", "s", "e")).isEqualTo(100);
        assertThat(s.scalarMetric("pv", "s", "e")).isEqualTo(500);
        assertThat(s.scalarMetric("bounce_rate", "s", "e")).isEqualTo(22.1);
        assertThat(s.scalarMetric("conversion_rate", "s", "e")).isEqualTo(4.4);
    }
}
