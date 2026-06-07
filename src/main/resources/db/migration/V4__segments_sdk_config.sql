-- V4__segments_sdk_config.sql
-- 用户分群 + SDK远程配置

CREATE TABLE IF NOT EXISTS tracker_segment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    segment_name    VARCHAR(128) NOT NULL COMMENT '分群名称',
    segment_key     VARCHAR(64) NOT NULL UNIQUE COMMENT '分群标识',
    description     VARCHAR(512) COMMENT '分群描述',
    segment_type    VARCHAR(32) DEFAULT 'dynamic' COMMENT 'dynamic/static',
    rules           JSON NOT NULL COMMENT '规则条件JSON',
    estimated_size  BIGINT DEFAULT 0 COMMENT '预估人数',
    refresh_interval VARCHAR(16) DEFAULT '24h' COMMENT '刷新间隔',
    last_refreshed_at DATETIME COMMENT '上次刷新时间',
    status          TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
    created_by      VARCHAR(64) COMMENT '创建人',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_seg_key (segment_key),
    INDEX idx_seg_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户分群表';

CREATE TABLE IF NOT EXISTS tracker_sdk_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_name     VARCHAR(128) NOT NULL COMMENT '配置名称',
    app_id          VARCHAR(64) NOT NULL COMMENT '应用ID',
    app_version     VARCHAR(64) DEFAULT '*' COMMENT '适用版本',
    platform        VARCHAR(32) DEFAULT '*' COMMENT '平台',
    config_data     JSON NOT NULL COMMENT '配置JSON',
    priority        INT DEFAULT 0 COMMENT '优先级',
    status          TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_sdk_app (app_id, platform, app_version),
    INDEX idx_sdk_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SDK远程配置表';
