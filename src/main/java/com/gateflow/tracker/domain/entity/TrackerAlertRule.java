package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 告警规则。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tracker_alert_rule")
public class TrackerAlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    /** 目标 app code;null = 所有 app。 */
    private String appCode;
    /** event_volume_drop | error_rate | null_rate。 */
    private String metric;
    private Double threshold;
    private Integer windowMinutes;
    private Integer enabled;
    private String notifyChannel;
    private String notifyTarget;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
