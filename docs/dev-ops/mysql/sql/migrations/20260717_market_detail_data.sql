-- 市场详情数据前向迁移。回滚时停止新任务与查询入口，保留公开历史数据，避免破坏性删除。

CREATE TABLE `fund_nav_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '基金净值历史ID',
    `fund_code` VARCHAR(50) NOT NULL COMMENT '基金代码',
    `nav_date` DATE NOT NULL COMMENT '净值日期',
    `unit_nav` DECIMAL(18,6) DEFAULT NULL COMMENT '单位净值',
    `accumulated_nav` DECIMAL(18,6) DEFAULT NULL COMMENT '累计净值',
    `daily_growth_rate` DECIMAL(12,4) DEFAULT NULL COMMENT '日增长率，单位为百分点',
    `source_as_of` DATETIME DEFAULT NULL COMMENT '数据源数据时间',
    `fetched_at` DATETIME NOT NULL COMMENT '数据获取时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fund_nav_history_code_date` (`fund_code`,`nav_date`),
    KEY `idx_fund_nav_history_code_date` (`fund_code`,`nav_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金净值历史';

CREATE TABLE `stock_price_bar` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '股票价格时间序列ID',
    `stock_code` VARCHAR(50) NOT NULL COMMENT '股票代码',
    `market` VARCHAR(20) NOT NULL COMMENT '业务市场：A_SHARE/US_STOCK',
    `granularity` VARCHAR(20) NOT NULL COMMENT '时间粒度',
    `bar_time` DATETIME NOT NULL COMMENT '行情周期时间',
    `open_price` DECIMAL(20,6) DEFAULT NULL COMMENT '开盘价',
    `high_price` DECIMAL(20,6) DEFAULT NULL COMMENT '最高价',
    `low_price` DECIMAL(20,6) DEFAULT NULL COMMENT '最低价',
    `close_price` DECIMAL(20,6) DEFAULT NULL COMMENT '收盘价',
    `volume` DECIMAL(28,6) DEFAULT NULL COMMENT '成交量',
    `currency` VARCHAR(3) DEFAULT NULL COMMENT '价格币种',
    `source_as_of` DATETIME DEFAULT NULL COMMENT '数据源数据时间',
    `fetched_at` DATETIME NOT NULL COMMENT '数据获取时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_price_bar_identity` (`stock_code`,`market`,`granularity`,`bar_time`),
    KEY `idx_stock_price_bar_query` (`stock_code`,`market`,`granularity`,`bar_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票价格时间序列';

CREATE TABLE `stock_company_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '股票公司资料ID',
    `stock_code` VARCHAR(50) NOT NULL COMMENT '股票代码',
    `market` VARCHAR(20) NOT NULL COMMENT '业务市场：A_SHARE/US_STOCK',
    `company_name` VARCHAR(200) DEFAULT NULL COMMENT '公司名称',
    `industry` VARCHAR(200) DEFAULT NULL COMMENT '所属行业',
    `business_summary` TEXT DEFAULT NULL COMMENT '主营业务摘要',
    `company_profile` TEXT DEFAULT NULL COMMENT '公司简介',
    `website` VARCHAR(500) DEFAULT NULL COMMENT '公司网站',
    `source_as_of` DATETIME DEFAULT NULL COMMENT '数据源数据时间',
    `fetched_at` DATETIME NOT NULL COMMENT '数据获取时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_company_profile_identity` (`stock_code`,`market`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票公司资料';
