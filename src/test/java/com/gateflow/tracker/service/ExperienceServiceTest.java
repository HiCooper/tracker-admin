package com.gateflow.tracker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceServiceTest {

    @Test
    void formatDurationReadable() {
        assertThat(ExperienceService.formatDur(0)).isEqualTo("0s");
        assertThat(ExperienceService.formatDur(45_000)).isEqualTo("45s");
        assertThat(ExperienceService.formatDur(150_000)).isEqualTo("2m 30s");
        assertThat(ExperienceService.formatDur(3_600_000)).isEqualTo("60m 0s");
        assertThat(ExperienceService.formatDur(-5)).isEqualTo("0s");
    }
}
