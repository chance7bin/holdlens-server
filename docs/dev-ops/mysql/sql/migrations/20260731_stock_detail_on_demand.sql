ALTER TABLE `processing_task`
    ADD COLUMN `active_key` VARCHAR(200) DEFAULT NULL COMMENT 'single-flight 活动业务键' AFTER `task_params_json`,
    ADD COLUMN `lease_until` DATETIME DEFAULT NULL COMMENT '活动业务键租约截止时间' AFTER `active_key`,
    ADD UNIQUE KEY `uk_processing_task_active_key` (`active_key`);

CREATE TABLE IF NOT EXISTS `stock_detail_slice_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '股票详情 slice 状态ID',
    `asset_ref` VARCHAR(120) NOT NULL COMMENT '统一股票资产引用',
    `slice_type` VARCHAR(50) NOT NULL COMMENT 'price_history/company_profile',
    `status` VARCHAR(20) NOT NULL COMMENT 'available/refreshing/empty/failed',
    `active_task_id` VARCHAR(100) DEFAULT NULL COMMENT '当前刷新任务ID',
    `last_attempt_at` DATETIME DEFAULT NULL COMMENT '最近尝试时间',
    `last_success_at` DATETIME DEFAULT NULL COMMENT '最近成功时间',
    `error_summary` VARCHAR(500) DEFAULT NULL COMMENT '安全错误摘要',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_detail_slice_state_asset_slice` (`asset_ref`,`slice_type`),
    KEY `idx_stock_detail_slice_state_active_task` (`active_task_id`),
    KEY `idx_stock_detail_slice_state_status_attempt` (`status`,`last_attempt_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票详情 slice 刷新状态';
