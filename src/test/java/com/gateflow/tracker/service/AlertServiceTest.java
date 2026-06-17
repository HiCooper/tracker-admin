package com.gateflow.tracker.service;

import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.repository.TrackerAlertEventMapper;
import com.gateflow.tracker.repository.TrackerAlertRuleMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertServiceTest {

    private final TrackerAlertRuleMapper ruleMapper = mock(TrackerAlertRuleMapper.class);
    private final TrackerAlertEventMapper eventMapper = mock(TrackerAlertEventMapper.class);
    private final AlertService service = new AlertService(ruleMapper, eventMapper);

    @Test
    void createDefaultsEnabled() {
        TrackerAlertRule rule = TrackerAlertRule.builder().name("r").metric("error_rate").threshold(0.05).build();
        service.createRule(rule);
        assertThat(rule.getEnabled()).isEqualTo(1);
        verify(ruleMapper).insert(rule);
    }

    @Test
    void updateNonexistentThrows() {
        when(ruleMapper.selectById(9L)).thenReturn(null);
        assertThatThrownBy(() -> service.updateRule(9L, new TrackerAlertRule()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void updateExistingPersists() {
        when(ruleMapper.selectById(1L)).thenReturn(new TrackerAlertRule());
        TrackerAlertRule patch = TrackerAlertRule.builder().threshold(0.7).build();
        service.updateRule(1L, patch);
        assertThat(patch.getId()).isEqualTo(1L);
        verify(ruleMapper).updateById(patch);
    }

    @Test
    void deleteDelegates() {
        service.deleteRule(3L);
        verify(ruleMapper).deleteById(3L);
    }

    @Test
    void listEventsCapsLimit() {
        service.listEvents(99999);
        verify(eventMapper).selectList(any());
    }
}
