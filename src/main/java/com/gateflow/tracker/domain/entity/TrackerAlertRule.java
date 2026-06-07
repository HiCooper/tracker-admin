package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_alert_rule")
public class TrackerAlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_name")
    private String ruleName;

    @TableField("metric")
    private String metric;

    @TableField("event_type")
    private String eventType;

    @TableField("aggregation")
    private String aggregation;

    @TableField("dimension_filters")
    private String dimensionFilters;

    @TableField("condition_type")
    private String conditionType;

    @TableField("threshold")
    private Double threshold;

    @TableField("comparison_period")
    private String comparisonPeriod;

    @TableField("check_interval")
    private String checkInterval;

    @TableField("channels")
    private String channels;

    @TableField("webhook_url")
    private String webhookUrl;

    @TableField("status")
    private Integer status;

    @TableField("last_checked_at")
    private LocalDateTime lastCheckedAt;

    @TableField("last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
