package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_event")
public class TrackerEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_key")
    private String eventKey;

    @TableField("event_name")
    private String eventName;

    @TableField("description")
    private String description;

    @TableField("category")
    private String category;

    @TableField("status")
    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
