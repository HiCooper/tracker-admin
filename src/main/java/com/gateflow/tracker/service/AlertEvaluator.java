package com.gateflow.tracker.service;

import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 告警规则评估(纯函数,易测)。给定规则与观测值,判断是否触发并产出告警事件。
 */
@Component
public class AlertEvaluator {

    /** 触发结果:observed 为记录用的观测值(跌幅或比率),message 为可读说明。 */
    public record Breach(double observed, String message) {}

    /**
     * @param current  当前窗口指标值(计数或比率)
     * @param baseline 基线值(仅 event_volume_drop 使用)
     */
    public Optional<Breach> evaluate(TrackerAlertRule rule, double current, double baseline) {
        if (rule == null || rule.getMetric() == null) {
            return Optional.empty();
        }
        double th = rule.getThreshold() != null ? rule.getThreshold() : 0d;
        return switch (rule.getMetric()) {
            case "event_volume_drop" -> {
                if (baseline <= 0) {
                    yield Optional.empty(); // 无基线不告警(避免冷启动误报)
                }
                double drop = (baseline - current) / baseline;
                yield drop >= th
                        ? Optional.of(new Breach(drop, String.format(
                            "事件量较基线下跌 %.1f%%(当前 %.0f / 基线 %.0f),超过阈值 %.1f%%",
                            drop * 100, current, baseline, th * 100)))
                        : Optional.empty();
            }
            case "error_rate" -> current >= th
                    ? Optional.of(new Breach(current, String.format(
                        "错误率 %.2f%% 超过阈值 %.2f%%", current * 100, th * 100)))
                    : Optional.empty();
            case "null_rate" -> current >= th
                    ? Optional.of(new Breach(current, String.format(
                        "关键字段空值率 %.2f%% 超过阈值 %.2f%%", current * 100, th * 100)))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }
}
