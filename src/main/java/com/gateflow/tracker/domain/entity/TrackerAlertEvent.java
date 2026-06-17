package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 告警触发历史。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tracker_alert_event")
public class TrackerAlertEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;
    private String ruleName;
    private String metric;
    private String appCode;
    private Double observed;
    private Double threshold;
    private String message;

    @TableField(value = "fired_at", fill = FieldFill.INSERT)
    private LocalDateTime firedAt;
}
