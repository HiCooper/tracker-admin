package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.entity.TrackerAlertEvent;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.exception.ErrorCode;
import com.gateflow.tracker.repository.TrackerAlertEventMapper;
import com.gateflow.tracker.repository.TrackerAlertRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/** 告警规则 CRUD 与触发历史查询。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final TrackerAlertRuleMapper ruleMapper;
    private final TrackerAlertEventMapper eventMapper;

    public List<TrackerAlertRule> listRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<TrackerAlertRule>()
                .orderByDesc(TrackerAlertRule::getCreatedAt));
    }

    public List<TrackerAlertRule> enabledRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<TrackerAlertRule>()
                .eq(TrackerAlertRule::getEnabled, 1));
    }

    public TrackerAlertRule createRule(TrackerAlertRule rule) {
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        ruleMapper.insert(rule);
        return rule;
    }

    public TrackerAlertRule updateRule(Long id, TrackerAlertRule rule) {
        TrackerAlertRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "Alert rule not found: " + id);
        }
        rule.setId(id);
        ruleMapper.updateById(rule);
        return ruleMapper.selectById(id);
    }

    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    public List<TrackerAlertEvent> listEvents(int limit) {
        return eventMapper.selectList(new LambdaQueryWrapper<TrackerAlertEvent>()
                .orderByDesc(TrackerAlertEvent::getFiredAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 1000))));
    }
}
