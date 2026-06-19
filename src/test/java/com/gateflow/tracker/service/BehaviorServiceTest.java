package com.gateflow.tracker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviorServiceTest {

    @Test
    void prevWindowIsEqualLengthImmediatelyBefore() {
        // [06-10, 06-16] 跨度 6 天 → 上一周期 [06-03, 06-09]
        String[] p = BehaviorService.prevWindow("2026-06-10", "2026-06-16");
        assertThat(p).containsExactly("2026-06-03", "2026-06-09");
    }

    @Test
    void prevWindowSingleDay() {
        String[] p = BehaviorService.prevWindow("2026-06-16", "2026-06-16");
        assertThat(p).containsExactly("2026-06-15", "2026-06-15");
    }

    @Test
    void prevWindowHandlesIso() {
        String[] p = BehaviorService.prevWindow("2026-06-10T00:00:00Z", "2026-06-12T23:59:59Z");
        assertThat(p).containsExactly("2026-06-07", "2026-06-09");
    }

    @Test
    void roundingHelpers() {
        assertThat(BehaviorService.round1(2.346)).isEqualTo(2.3);
        assertThat(BehaviorService.round4(0.123456)).isEqualTo(0.1235);
    }
}
