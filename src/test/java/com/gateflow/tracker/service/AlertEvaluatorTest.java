package com.gateflow.tracker.service;

import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertEvaluatorTest {

    private final AlertEvaluator evaluator = new AlertEvaluator();

    private TrackerAlertRule rule(String metric, double threshold) {
        return TrackerAlertRule.builder().name("r").metric(metric).threshold(threshold).build();
    }

    @Test
    void volumeDropFiresWhenDropExceedsThreshold() {
        // 当前 40,基线 100 → 跌 60% ≥ 阈值 50%
        var breach = evaluator.evaluate(rule("event_volume_drop", 0.5), 40, 100);
        assertThat(breach).isPresent();
        assertThat(breach.get().observed()).isEqualTo(0.6);
        assertThat(breach.get().message()).contains("下跌");
    }

    @Test
    void volumeDropNoFireWhenWithinThreshold() {
        assertThat(evaluator.evaluate(rule("event_volume_drop", 0.5), 80, 100)).isEmpty();
    }

    @Test
    void volumeDropNoFireWithoutBaseline() {
        // 冷启动:基线为 0 不告警
        assertThat(evaluator.evaluate(rule("event_volume_drop", 0.5), 0, 0)).isEmpty();
    }

    @Test
    void errorRateFiresAtOrAboveThreshold() {
        assertThat(evaluator.evaluate(rule("error_rate", 0.05), 0.05, 0)).isPresent();
        assertThat(evaluator.evaluate(rule("error_rate", 0.05), 0.04, 0)).isEmpty();
    }

    @Test
    void nullRateFires() {
        var b = evaluator.evaluate(rule("null_rate", 0.2), 0.3, 0);
        assertThat(b).isPresent();
        assertThat(b.get().message()).contains("空值率");
    }

    @Test
    void unknownMetricNeverFires() {
        assertThat(evaluator.evaluate(rule("weird", 0.1), 1, 1)).isEmpty();
    }
}
