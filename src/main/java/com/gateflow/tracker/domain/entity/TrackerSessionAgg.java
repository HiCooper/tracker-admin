package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tracker_session_agg")
public class TrackerSessionAgg {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("date")
    private LocalDate date;

    @TableField("hour")
    private Integer hour;

    @TableField("platform")
    private String platform;

    @TableField("session_count")
    private Long sessionCount;

    @TableField("user_count")
    private Long userCount;

    @TableField("avg_duration")
    private BigDecimal avgDuration;

    @TableField("avg_page_depth")
    private BigDecimal avgPageDepth;

    @TableField("bounce_count")
    private Long bounceCount;

    @TableField("bounce_rate")
    private BigDecimal bounceRate;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
