package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_plan")
public class TrackerPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("plan_name")
    private String planName;

    @TableField("app_id")
    private Long appId;

    @TableField("app_name")
    private String appName;

    @TableField("app_version")
    private String appVersion;

    @TableField("status")
    private String status;

    @TableField("submitter")
    private String submitter;

    @TableField("reviewer")
    private String reviewer;

    @TableField("review_comment")
    private String reviewComment;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
