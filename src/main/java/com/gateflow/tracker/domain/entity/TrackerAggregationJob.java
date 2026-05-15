package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_aggregation_job")
public class TrackerAggregationJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("job_type")
    private String jobType;

    @TableField("job_status")
    private String jobStatus;

    @TableField("trigger_type")
    private String triggerType;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("time_range_start")
    private LocalDateTime timeRangeStart;

    @TableField("time_range_end")
    private LocalDateTime timeRangeEnd;

    @TableField("records_processed")
    private Long recordsProcessed;

    @TableField("error_message")
    private String errorMessage;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
