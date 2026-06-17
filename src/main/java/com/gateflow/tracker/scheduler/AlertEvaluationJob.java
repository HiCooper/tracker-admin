package com.gateflow.tracker.scheduler;

import com.gateflow.tracker.domain.entity.TrackerAlertEvent;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import com.gateflow.tracker.repository.TrackerAlertEventMapper;
import com.gateflow.tracker.service.AlertEvaluator;
import com.gateflow.tracker.service.AlertNotifier;
import com.gateflow.tracker.service.AlertService;
import com.gateflow.tracker.service.MetricQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时评估告警规则:对每条启用规则计算指标 → 评估 → 触发则记录并通知。
 * 默认每 5 分钟一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluationJob {

    private final AlertService alertService;
    private final MetricQueryService metricQueryService;
    private final AlertEvaluator evaluator;
    private final AlertNotifier notifier;
    private final TrackerAlertEventMapper eventMapper;

    @Scheduled(fixedDelayString = "${gateflow.alert.eval-interval-ms:300000}",
               initialDelayString = "${gateflow.alert.eval-initial-delay-ms:60000}")
    public void evaluateAll() {
        List<TrackerAlertRule> rules = alertService.enabledRules();
        if (rules.isEmpty()) {
            return;
        }
        int fired = 0;
        for (TrackerAlertRule rule : rules) {
            try {
                if (evaluateRule(rule)) {
                    fired++;
                }
            } catch (Exception e) {
                log.error("Alert rule {} evaluation failed: {}", rule.getId(), e.getMessage());
            }
        }
        if (fired > 0) {
            log.info("Alert evaluation fired {} alerts", fired);
        }
    }

    /** 评估单条规则;触发返回 true。包级可见以便测试。 */
    boolean evaluateRule(TrackerAlertRule rule) {
        MetricQueryService.MetricSample sample = metricQueryService.sample(rule);
        var breach = evaluator.evaluate(rule, sample.current(), sample.baseline());
        if (breach.isEmpty()) {
            return false;
        }
        TrackerAlertEvent event = TrackerAlertEvent.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .metric(rule.getMetric())
                .appCode(rule.getAppCode())
                .observed(breach.get().observed())
                .threshold(rule.getThreshold())
                .message(breach.get().message())
                .build();
        eventMapper.insert(event);
        notifier.notify(rule, event);
        log.warn("ALERT [{}] {}", rule.getName(), event.getMessage());
        return true;
    }
}
