-- 自建账号与会话认证：仅新增安全事实表，不读取、复制或修改 user_id = 1 的业务数据。

CREATE TABLE IF NOT EXISTS `user_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账号ID；1保留给开发固定用户，注册账号从2开始',
    `username` VARCHAR(32) NOT NULL COMMENT '规范化用户名',
    `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE/DISABLED',
    `failed_login_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `locked_until` DATETIME DEFAULT NULL COMMENT '锁定截止时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地用户账号';

ALTER TABLE `user_account` AUTO_INCREMENT = 2;

CREATE TABLE IF NOT EXISTS `user_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '账号ID',
    `token_hash` CHAR(64) NOT NULL COMMENT 'SHA-256令牌摘要',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `revoked_at` DATETIME DEFAULT NULL COMMENT '撤销时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_session_token_hash` (`token_hash`),
    KEY `idx_user_session_user_id` (`user_id`),
    KEY `idx_user_session_active` (`token_hash`, `revoked_at`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地用户会话';
