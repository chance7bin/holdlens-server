CREATE TABLE IF NOT EXISTS `bookkeeping_entry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收支条目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `request_id` VARCHAR(64) NOT NULL COMMENT '创建幂等键',
    `type` VARCHAR(20) NOT NULL COMMENT 'EXPENSE/INCOME',
    `category_code` VARCHAR(50) NOT NULL COMMENT '稳定收支分类代码',
    `amount` DECIMAL(20,2) NOT NULL COMMENT '正数金额',
    `currency` VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '固定人民币',
    `entry_date` DATE NOT NULL COMMENT '发生日期',
    `note` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `status` VARCHAR(20) NOT NULL COMMENT 'ACTIVE/DELETED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bookkeeping_entry_user_request` (`user_id`,`request_id`),
    KEY `idx_bookkeeping_entry_detail` (`user_id`,`status`,`entry_date`,`id`),
    KEY `idx_bookkeeping_entry_category` (`user_id`,`status`,`type`,`category_code`,`entry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='独立收支条目';
