-- 用户认证表
-- V5__auth_users.sql

CREATE TABLE tracker_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(256) NOT NULL COMMENT '密码 (BCrypt)',
    role        VARCHAR(32) DEFAULT 'ADMIN' COMMENT '角色: ADMIN/VIEWER',
    enabled     TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户认证表';
