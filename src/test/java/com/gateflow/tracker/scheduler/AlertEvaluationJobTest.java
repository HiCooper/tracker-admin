package com.gateflow.tracker.scheduler;

import com.gateflow.tracker.domain.entity.TrackerAlertEvent;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import com.gateflow.tracker.repository.TrackerAlertEventMapper;
import com.gateflow.tracker.service.AlertEvaluator;
import com.gateflow.tracker.service.AlertNotifier;
import com.gateflow.tracker.service.AlertService;
import com.gateflow.tracker.service.MetricQueryService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertEvaluationJobTest {

    private final AlertService alertService = mock(AlertService.class);
    private final MetricQueryService metricQuery = mock(MetricQueryService.class);
    private final AlertEvaluator evaluator = mock(AlertEvaluator.class);
    private final AlertNotifier notifier = mock(AlertNotifier.class);
    private final TrackerAlertEventMapper eventMapper = mock(TrackerAlertEventMapper.class);
    private final AlertEvaluationJob job =
            new AlertEvaluationJob(alertService, metricQuery, evaluator, notifier, eventMapper);

    private TrackerAlertRule rule() {
        return TrackerAlertRule.builder().id(1L).name("vol").metric("event_volume_drop")
                .threshold(0.5).windowMinutes(60).build();
    }

    @Test
    void firesRecordsAndNotifiesOnBreach() {
        when(metricQuery.sample(any())).thenReturn(new MetricQueryService.MetricSample(40, 100));
        when(evaluator.evaluate(any(), eq(40d), eq(100d)))
                .thenReturn(Optional.of(new AlertEvaluator.Breach(0.6, "跌 60%")));

        boolean fired = job.evaluateRule(rule());

        assertThat(fired).isTrue();
        verify(eventMapper).insert(any(TrackerAlertEvent.class));
        verify(notifier).notify(any(), any(TrackerAlertEvent.class));
    }

    @Test
    void noBreachDoesNotRecordOrNotify() {
        when(metricQuery.sample(any())).thenReturn(new MetricQueryService.MetricSample(95, 100));
        when(evaluator.evaluate(any(), anyDouble(), anyDouble())).thenReturn(Optional.empty());

        assertThat(job.evaluateRule(rule())).isFalse();
        verify(eventMapper, never()).insert(any(TrackerAlertEvent.class));
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void evaluateAllIteratesEnabledRules() {
        when(alertService.enabledRules()).thenReturn(java.util.List.of(rule(), rule()));
        when(metricQuery.sample(any())).thenReturn(new MetricQueryService.MetricSample(100, 100));
        when(evaluator.evaluate(any(), anyDouble(), anyDouble())).thenReturn(Optional.empty());

        job.evaluateAll();

        verify(metricQuery, times(2)).sample(any());
    }
}
