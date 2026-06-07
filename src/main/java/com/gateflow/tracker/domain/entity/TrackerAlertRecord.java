package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_alert_record")
public class TrackerAlertRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("rule_name")
    private String ruleName;

    @TableField("metric")
    private String metric;

    @TableField("current_value")
    private Double currentValue;

    @TableField("threshold_value")
    private Double thresholdValue;

    @TableField("alert_message")
    private String alertMessage;

    @TableField("triggered_at")
    private LocalDateTime triggeredAt;

    @TableField("acknowledged")
    private Integer acknowledged;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
