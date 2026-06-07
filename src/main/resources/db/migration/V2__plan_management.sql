-- V2__plan_management.sql
-- 埋点需求方案管理表结构

CREATE TABLE IF NOT EXISTS tracker_plan (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_name       VARCHAR(256) NOT NULL COMMENT '方案名称',
    app_id          BIGINT NOT NULL COMMENT '关联应用ID',
    app_name        VARCHAR(128) COMMENT '应用名称',
    app_version     VARCHAR(64) COMMENT '应用版本号',
    status          VARCHAR(32) DEFAULT 'draft' COMMENT '状态: draft/reviewing/approved/online/rejected',
    submitter       VARCHAR(64) COMMENT '提交人',
    reviewer        VARCHAR(64) COMMENT '审核人',
    review_comment  VARCHAR(1024) COMMENT '审核意见',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_plan_status (status),
    INDEX idx_plan_app_id (app_id),
    INDEX idx_plan_submitter (submitter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='埋点需求方案表';

CREATE TABLE IF NOT EXISTS tracker_plan_event (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id         BIGINT NOT NULL COMMENT '关联方案ID',
    event_key       VARCHAR(64) NOT NULL COMMENT '事件标识',
    event_name      VARCHAR(128) NOT NULL COMMENT '事件名称',
    category        VARCHAR(32) DEFAULT 'custom' COMMENT '事件分类',
    description     VARCHAR(512) COMMENT '事件描述',
    spm_code        VARCHAR(64) COMMENT '关联SPM编码',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_pe_plan_id (plan_id),
    FOREIGN KEY (plan_id) REFERENCES tracker_plan(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='方案事件定义表';

CREATE TABLE IF NOT EXISTS tracker_plan_property (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id        BIGINT NOT NULL COMMENT '关联事件ID',
    prop_key        VARCHAR(64) NOT NULL COMMENT '属性标识',
    prop_name       VARCHAR(128) NOT NULL COMMENT '属性名称',
    data_type       VARCHAR(32) DEFAULT 'string' COMMENT '数据类型',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_pp_event_id (event_id),
    FOREIGN KEY (event_id) REFERENCES tracker_plan_event(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='方案事件属性表';
