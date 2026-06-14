package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tracker_block")
public class TrackerBlock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pageId;

    private String blockCode;

    private String blockName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
