package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_plan_event")
public class TrackerPlanEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("plan_id")
    private Long planId;

    @TableField("event_key")
    private String eventKey;

    @TableField("event_name")
    private String eventName;

    @TableField("category")
    private String category;

    @TableField("description")
    private String description;

    @TableField("spm_code")
    private String spmCode;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
