package com.gateflow.tracker.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/** 管理操作审计日志。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tracker_audit_log")
public class TrackerAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String role;
    private String method;
    private String path;
    private Integer status;
    private String requestId;
    private String ip;
    private Long durationMs;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
