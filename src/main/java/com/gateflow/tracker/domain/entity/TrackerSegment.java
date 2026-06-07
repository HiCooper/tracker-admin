package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_segment")
public class TrackerSegment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("segment_name")
    private String segmentName;

    @TableField("segment_key")
    private String segmentKey;

    @TableField("description")
    private String description;

    @TableField("segment_type")
    private String segmentType;

    @TableField("rules")
    private String rules;

    @TableField("estimated_size")
    private Long estimatedSize;

    @TableField("refresh_interval")
    private String refreshInterval;

    @TableField("last_refreshed_at")
    private LocalDateTime lastRefreshedAt;

    @TableField("status")
    private Integer status;

    @TableField("created_by")
    private String createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
