package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_plan")
public class TrackerPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planName;

    private Long appId;

    private String appName;

    private String appVersion;

    private String status;

    private String eventsJson;

    private String submitter;

    private String reviewer;

    private String reviewComment;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
