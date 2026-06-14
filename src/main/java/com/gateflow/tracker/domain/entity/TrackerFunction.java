package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tracker_function")
public class TrackerFunction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long blockId;

    private String funcCode;

    private String funcName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
