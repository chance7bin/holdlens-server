-- 市场详情数据前向迁移。回滚时停止新任务与查询入口，保留公开历史数据，避免破坏性删除。

CREATE TABLE `fund_nav_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `fund_code` VARCHAR(50) NOT NULL,
    `nav_date` DATE NOT NULL,
    `unit_nav` DECIMAL(18,6) DEFAULT NULL,
    `accumulated_nav` DECIMAL(18,6) DEFAULT NULL,
    `daily_growth_rate` DECIMAL(12,4) DEFAULT NULL,
    `source_as_of` DATETIME DEFAULT NULL,
    `fetched_at` DATETIME NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fund_nav_history_code_date` (`fund_code`,`nav_date`),
    KEY `idx_fund_nav_history_code_date` (`fund_code`,`nav_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金净值历史';

CREATE TABLE `stock_price_bar` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `stock_code` VARCHAR(50) NOT NULL,
    `market` VARCHAR(20) NOT NULL,
    `granularity` VARCHAR(20) NOT NULL,
    `bar_time` DATETIME NOT NULL,
    `open_price` DECIMAL(20,6) DEFAULT NULL,
    `high_price` DECIMAL(20,6) DEFAULT NULL,
    `low_price` DECIMAL(20,6) DEFAULT NULL,
    `close_price` DECIMAL(20,6) DEFAULT NULL,
    `volume` DECIMAL(28,6) DEFAULT NULL,
    `currency` VARCHAR(3) DEFAULT NULL,
    `source_as_of` DATETIME DEFAULT NULL,
    `fetched_at` DATETIME NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_price_bar_identity` (`stock_code`,`market`,`granularity`,`bar_time`),
    KEY `idx_stock_price_bar_query` (`stock_code`,`market`,`granularity`,`bar_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票价格时间序列';

CREATE TABLE `stock_company_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `stock_code` VARCHAR(50) NOT NULL,
    `market` VARCHAR(20) NOT NULL,
    `company_name` VARCHAR(200) DEFAULT NULL,
    `industry` VARCHAR(200) DEFAULT NULL,
    `business_summary` TEXT DEFAULT NULL,
    `company_profile` TEXT DEFAULT NULL,
    `website` VARCHAR(500) DEFAULT NULL,
    `source_as_of` DATETIME DEFAULT NULL,
    `fetched_at` DATETIME NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_company_profile_identity` (`stock_code`,`market`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票公司资料';
