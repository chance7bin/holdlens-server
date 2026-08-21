-- 为会话增加客户端随机安装标识；历史会话保持为空，标识不参与认证或授权。

ALTER TABLE `user_session`
    ADD COLUMN `installation_id` CHAR(36) DEFAULT NULL COMMENT '客户端随机安装标识；不作为认证凭据' AFTER `token_hash`,
    ADD COLUMN `device_name` VARCHAR(100) DEFAULT NULL COMMENT '客户端提供的可读设备名称' AFTER `installation_id`,
    ADD KEY `idx_user_session_user_installation` (`user_id`, `installation_id`);
