package com.gateflow.tracker.service;

import com.gateflow.tracker.domain.dto.platform.PlatformDto.AnomalyItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PlatformDataServiceTest {

    @Test
    void anomalyRisingExceedsThreshold() {
        AnomalyItem a = PlatformDataService.anomaly("UV", 120, 100);
        assertThat(a).isNotNull();
        assertThat(a.getDir()).isEqualTo("up");
        assertThat(a.getChange()).isEqualTo("+20.0%");
        assertThat(a.getDetail()).contains("120").contains("100");
    }

    @Test
    void anomalyFallingExceedsThreshold() {
        AnomalyItem a = PlatformDataService.anomaly("UV", 80, 100);
        assertThat(a.getDir()).isEqualTo("down");
        assertThat(a.getChange()).isEqualTo("-20.0%");
    }

    @Test
    void anomalyBelowThresholdIsNull() {
        assertThat(PlatformDataService.anomaly("UV", 103, 100)).isNull(); // 3% < 5%
    }

    @Test
    void anomalyNoBaselineIsNull() {
        assertThat(PlatformDataService.anomaly("UV", 50, 0)).isNull();
    }

    @Test
    void percentAndRounding() {
        assertThat(PlatformDataService.pct(0.452)).isCloseTo(45.2, within(1e-9));
        assertThat(PlatformDataService.round1(2.346)).isCloseTo(2.3, within(1e-9));
        assertThat(PlatformDataService.round4(0.123456)).isCloseTo(0.1235, within(1e-9));
    }
}
