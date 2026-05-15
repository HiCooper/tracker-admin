package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tracker_spm")
public class TrackerSpm {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("spm_code")
    private String spmCode;

    @TableField("spm_name")
    private String spmName;

    @TableField("spma_label")
    private String spmaLabel;

    @TableField("spmb_label")
    private String spmbLabel;

    @TableField("spmc_label")
    private String spmcLabel;

    @TableField("spmd_label")
    private String spmdLabel;

    @TableField("description")
    private String description;

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
