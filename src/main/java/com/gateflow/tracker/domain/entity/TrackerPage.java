package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tracker_page")
public class TrackerPage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;

    private String pageCode;

    private String pageName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
