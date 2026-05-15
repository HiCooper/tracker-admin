package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tracker_event_agg")
public class TrackerEventAgg {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("date")
    private LocalDate date;

    @TableField("hour")
    private Integer hour;

    @TableField("platform")
    private String platform;

    @TableField("event_type")
    private String eventType;

    @TableField("event_count")
    private Long eventCount;

    @TableField("user_count")
    private Long userCount;

    @TableField("device_count")
    private Long deviceCount;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
