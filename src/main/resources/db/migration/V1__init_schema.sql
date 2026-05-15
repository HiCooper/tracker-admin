-- Tracker Admin 初始数据库结构
-- V1__init_schema.sql

-- 事件定义表
CREATE TABLE tracker_event (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_key   VARCHAR(64) NOT NULL UNIQUE COMMENT '事件标识',
    event_name  VARCHAR(128) NOT NULL COMMENT '事件名称',
    description VARCHAR(512),
    category    VARCHAR(32) DEFAULT 'custom' COMMENT '事件分类: page_view/click/exposure/custom',
    status      TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    INDEX idx_event_key (event_key),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件定义表';

-- 属性定义表
CREATE TABLE tracker_property (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id    BIGINT NOT NULL COMMENT '关联事件ID',
    prop_key    VARCHAR(64) NOT NULL COMMENT '属性标识',
    prop_name   VARCHAR(128) NOT NULL COMMENT '属性名称',
    data_type   VARCHAR(32) DEFAULT 'string' COMMENT '类型: string/number/boolean/date',
    description VARCHAR(512),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (event_id) REFERENCES tracker_event(id) ON DELETE CASCADE,
    UNIQUE KEY uk_event_prop (event_id, prop_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='属性定义表';

-- SPM 配置表
CREATE TABLE tracker_spm (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    spm_code    VARCHAR(64) NOT NULL UNIQUE COMMENT 'SPM编码',
    spm_name    VARCHAR(128) NOT NULL COMMENT 'SPM名称',
    spma_label  VARCHAR(64) COMMENT 'A层标签',
    spmb_label  VARCHAR(64) COMMENT 'B层标签',
    spmc_label  VARCHAR(64) COMMENT 'C层标签',
    spmd_label  VARCHAR(64) COMMENT 'D层标签',
    description VARCHAR(512),
    status      TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_spm_code (spm_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SPM配置表';

-- 看板配置表
CREATE TABLE tracker_dashboard (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(128) NOT NULL COMMENT '看板名称',
    config      JSON NOT NULL COMMENT '看板配置JSON',
    created_by  VARCHAR(64),
    status      TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='看板配置表';

-- 事件聚合表 (按时间窗口聚合)
CREATE TABLE tracker_event_agg (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    date            DATE NOT NULL COMMENT '日期',
    hour            TINYINT COMMENT '小时 (0-23, -1表示全天)',
    platform        VARCHAR(32) COMMENT '平台',
    event_type      VARCHAR(64) NOT NULL COMMENT '事件类型',
    event_count     BIGINT DEFAULT 0 COMMENT '事件次数',
    user_count      BIGINT DEFAULT 0 COMMENT '用户数 (去重)',
    device_count    BIGINT DEFAULT 0 COMMENT '设备数 (去重)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date_hour_platform_event (date, hour, platform, event_type),
    INDEX idx_date (date),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件聚合表';

-- Session 聚合表
CREATE TABLE tracker_session_agg (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    date                DATE NOT NULL COMMENT '日期',
    hour                TINYINT COMMENT '小时',
    platform            VARCHAR(32) COMMENT '平台',
    session_count       BIGINT DEFAULT 0 COMMENT '会话次数',
    user_count          BIGINT DEFAULT 0 COMMENT '用户数 (去重)',
    avg_duration        DECIMAL(10,2) DEFAULT 0 COMMENT '平均会话时长(秒)',
    avg_page_depth      DECIMAL(10,2) DEFAULT 0 COMMENT '平均页面深度',
    bounce_count        BIGINT DEFAULT 0 COMMENT '跳出次数 (单页Session)',
    bounce_rate         DECIMAL(5,4) DEFAULT 0 COMMENT '跳出率',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date_hour_platform (date, hour, platform),
    INDEX idx_date (date),
    INDEX idx_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Session聚合表';

-- 聚合任务调度记录表
CREATE TABLE tracker_aggregation_job (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_type            VARCHAR(32) NOT NULL COMMENT '任务类型: event/session',
    job_status          VARCHAR(32) NOT NULL COMMENT '任务状态: running/success/failed',
    trigger_type        VARCHAR(32) NOT NULL COMMENT '触发类型: scheduled/manual',
    start_time          DATETIME NOT NULL COMMENT '开始时间',
    end_time            DATETIME COMMENT '结束时间',
    time_range_start    DATETIME NOT NULL COMMENT '数据时间范围开始',
    time_range_end      DATETIME NOT NULL COMMENT '数据时间范围结束',
    records_processed   BIGINT DEFAULT 0 COMMENT '处理记录数',
    error_message       VARCHAR(1024) COMMENT '错误信息',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_job_type_status (job_type, job_status),
    INDEX idx_trigger_type (trigger_type),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聚合任务调度记录表';
