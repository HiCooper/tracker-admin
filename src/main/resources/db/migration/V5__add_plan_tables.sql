-- 埋点需求方案 — 工作流管理
-- V5__add_plan_tables.sql

CREATE TABLE IF NOT EXISTS tracker_plan (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_name       VARCHAR(256) NOT NULL COMMENT '方案名称',
    app_id          BIGINT NOT NULL COMMENT '关联应用ID',
    app_name        VARCHAR(256) DEFAULT '' COMMENT '应用名称(冗余)',
    app_version     VARCHAR(64) NOT NULL COMMENT '版本号',
    status          VARCHAR(32) DEFAULT 'draft' COMMENT '状态: draft/reviewing/approved/implementing/verified/online/rejected',
    events_json     MEDIUMTEXT COMMENT '事件列表 JSON',
    submitter       VARCHAR(64) DEFAULT '' COMMENT '提交人',
    reviewer        VARCHAR(64) DEFAULT '' COMMENT '审核人',
    review_comment  VARCHAR(512) DEFAULT '' COMMENT '审核意见',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (status),
    INDEX idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='埋点需求方案表';
