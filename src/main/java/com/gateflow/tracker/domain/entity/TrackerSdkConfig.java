package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_sdk_config")
public class TrackerSdkConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_name")
    private String configName;

    @TableField("app_id")
    private String appId;

    @TableField("app_version")
    private String appVersion;

    @TableField("platform")
    private String platform;

    @TableField("config_data")
    private String configData;

    @TableField("priority")
    private Integer priority;

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
