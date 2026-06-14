-- SPM 埋点管理 — 四级层级结构
-- V3__add_setup_tables.sql

CREATE TABLE tracker_app (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_code    VARCHAR(256) NOT NULL UNIQUE COMMENT '应用标识',
    app_name    VARCHAR(128) NOT NULL COMMENT '应用名称',
    description VARCHAR(512) COMMENT '描述',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_app_code (app_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

CREATE TABLE tracker_page (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_id      BIGINT NOT NULL COMMENT '所属应用ID',
    page_code   VARCHAR(256) NOT NULL COMMENT '页面标识',
    page_name   VARCHAR(128) NOT NULL COMMENT '页面名称',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_app_page (app_id, page_code),
    INDEX idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面表';

CREATE TABLE tracker_block (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    page_id     BIGINT NOT NULL COMMENT '所属页面ID',
    block_code  VARCHAR(256) NOT NULL COMMENT '模块标识',
    block_name  VARCHAR(128) NOT NULL COMMENT '模块名称',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_page_block (page_id, block_code),
    INDEX idx_page_id (page_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模块表';

CREATE TABLE tracker_function (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    block_id    BIGINT NOT NULL COMMENT '所属模块ID',
    func_code   VARCHAR(256) NOT NULL COMMENT '功能标识',
    func_name   VARCHAR(128) NOT NULL COMMENT '功能名称',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_block_func (block_id, func_code),
    INDEX idx_block_id (block_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='功能表';
