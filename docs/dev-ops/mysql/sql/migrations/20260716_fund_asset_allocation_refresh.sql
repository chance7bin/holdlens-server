-- 基金资产配置独立刷新：前向迁移。
-- 只增加兼容列、当前快照表和索引，不删除或改写基金、收益和重仓数据。

ALTER TABLE `fund`
    ADD COLUMN `asset_allocation_as_of` DATE DEFAULT NULL COMMENT '资产配置报告期' AFTER `public_holdings_status`,
    ADD COLUMN `asset_allocation_status` VARCHAR(20) NOT NULL DEFAULT 'missing' COMMENT '资产配置状态：available/unavailable/missing' AFTER `asset_allocation_as_of`,
    ADD COLUMN `asset_allocation_fetched_at` DATETIME DEFAULT NULL COMMENT '基金资产配置最近认可获取时间' AFTER `top_holding_fetched_at`,
    ADD INDEX `idx_fund_asset_allocation_refresh` (`asset_allocation_status`, `asset_allocation_fetched_at`, `asset_allocation_as_of`);

CREATE TABLE `fund_asset_allocation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '基金资产配置ID',
    `fund_code` VARCHAR(50) NOT NULL COMMENT '基金代码',
    `asset_type` VARCHAR(50) NOT NULL COMMENT '标准资产类型',
    `asset_type_name` VARCHAR(100) NOT NULL COMMENT '数据源原始资产类型名称',
    `allocation_ratio` DECIMAL(12, 4) NOT NULL COMMENT '配置占比（百分点）',
    `display_order` INT NOT NULL COMMENT '展示顺序，从1开始',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fund_asset_allocation_type_name` (`fund_code`, `asset_type`, `asset_type_name`),
    KEY `idx_fund_asset_allocation_fund_code` (`fund_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金当前资产配置表';
