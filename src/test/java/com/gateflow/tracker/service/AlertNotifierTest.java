package com.gateflow.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.entity.TrackerAlertEvent;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AlertNotifierTest {

    private final AlertNotifier notifier = new AlertNotifier(new ObjectMapper());

    private TrackerAlertEvent event() {
        return TrackerAlertEvent.builder().ruleName("vol").metric("event_volume_drop")
                .observed(0.6).threshold(0.5).message("跌 60%").build();
    }

    @Test
    void payloadContainsKeyFields() {
        String json = notifier.payload(event());
        assertThat(json).contains("\"rule\":\"vol\"")
                .contains("\"metric\":\"event_volume_drop\"")
                .contains("\"message\":\"跌 60%\"");
    }

    @Test
    void noWebhookChannelIsNoOp() {
        TrackerAlertRule rule = TrackerAlertRule.builder().notifyChannel(null).build();
        assertThatNoException().isThrownBy(() -> notifier.notify(rule, event()));
    }

    @Test
    void webhookWithoutTargetIsNoOp() {
        TrackerAlertRule rule = TrackerAlertRule.builder().notifyChannel("webhook").notifyTarget("").build();
        assertThatNoException().isThrownBy(() -> notifier.notify(rule, event()));
    }
}
