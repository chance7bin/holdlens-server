-- =============================================
-- HoldLens 数据库初始化脚本
-- Create: 2026-06-15
-- Timezone: 业务 DATETIME 统一存储 Asia/Shanghai 本地时间
-- Note: DATETIME 不保存 offset，不表示 UTC instant
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS holdlens
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE holdlens;

-- ----------------------------
-- 本地账号与会话
-- ----------------------------
DROP TABLE IF EXISTS `user_session`;
DROP TABLE IF EXISTS `user_account`;

CREATE TABLE `user_account` (
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地用户账号';

CREATE TABLE `user_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '账号ID',
    `token_hash` CHAR(64) NOT NULL COMMENT 'SHA-256令牌摘要',
    `installation_id` CHAR(36) DEFAULT NULL COMMENT '客户端随机安装标识；不作为认证凭据',
    `device_name` VARCHAR(100) DEFAULT NULL COMMENT '客户端提供的可读设备名称',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `revoked_at` DATETIME DEFAULT NULL COMMENT '撤销时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_session_token_hash` (`token_hash`),
    KEY `idx_user_session_user_id` (`user_id`),
    KEY `idx_user_session_user_installation` (`user_id`, `installation_id`),
    KEY `idx_user_session_active` (`token_hash`, `revoked_at`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地用户会话';

-- ----------------------------
-- 资产目录与资产记录
-- ----------------------------
DROP TABLE IF EXISTS `asset_holding_change`;
DROP TABLE IF EXISTS `asset_holding`;
DROP TABLE IF EXISTS `asset_info`;
DROP TABLE IF EXISTS `asset_account`;
DROP TABLE IF EXISTS `asset_record_change`;
DROP TABLE IF EXISTS `watchlist_item`;
DROP TABLE IF EXISTS `asset_record`;
DROP TABLE IF EXISTS `asset_catalog`;
DROP TABLE IF EXISTS `exchange_rate`;

CREATE TABLE `asset_catalog` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资产目录ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID；系统目录为空',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父目录ID',
    `catalog_code` VARCHAR(50) DEFAULT NULL COMMENT '系统目录稳定编码；用户目录为空',
    `catalog_name` VARCHAR(100) NOT NULL COMMENT '目录名称',
    `catalog_scope` VARCHAR(20) NOT NULL COMMENT '目录范围：SYSTEM/USER',
    `balance_direction` VARCHAR(20) NOT NULL COMMENT '金额方向：ADD/SUBTRACT',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DELETED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_catalog_code` (`catalog_code`),
    KEY `idx_asset_catalog_user_status` (`user_id`, `status`),
    KEY `idx_asset_catalog_parent_status` (`parent_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产目录';

CREATE TABLE `asset_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资产记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `catalog_id` BIGINT NOT NULL COMMENT '资产目录ID',
    `record_name` VARCHAR(200) NOT NULL COMMENT '记录名称',
    `asset_kind` VARCHAR(20) DEFAULT NULL COMMENT '公共标的类型：FUND/STOCK',
    `asset_id` BIGINT DEFAULT NULL COMMENT 'fund.id 或 stock_market.id',
    `amount` DECIMAL(20, 4) NOT NULL DEFAULT 0 COMMENT '用户确认的当前原币金额',
    `currency` VARCHAR(3) NOT NULL COMMENT '原币币种',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '用户备注',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED/DELETED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_asset_record_user_status` (`user_id`, `status`),
    KEY `idx_asset_record_user_catalog_status` (`user_id`, `catalog_id`, `status`),
    KEY `idx_asset_record_user_asset_status` (`user_id`, `asset_kind`, `asset_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资产记录';

CREATE TABLE `asset_record_change` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资产记录变更ID',
    `operation_id` VARCHAR(64) NOT NULL COMMENT '同一原子操作标识',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `record_id` BIGINT NOT NULL COMMENT '资产记录ID',
    `change_type` VARCHAR(30) NOT NULL COMMENT 'CREATE/UPDATE_AMOUNT/SPLIT_OUT/SPLIT_IN/ARCHIVE/RESTORE/DELETE',
    `before_amount` DECIMAL(20, 4) DEFAULT NULL COMMENT '变更前金额',
    `after_amount` DECIMAL(20, 4) DEFAULT NULL COMMENT '变更后金额',
    `currency` VARCHAR(3) NOT NULL COMMENT '金额币种',
    `before_status` VARCHAR(20) DEFAULT NULL COMMENT '变更前状态',
    `after_status` VARCHAR(20) DEFAULT NULL COMMENT '变更后状态',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_asset_record_change_user_record` (`user_id`, `record_id`, `id`),
    KEY `idx_asset_record_change_operation` (`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='只追加资产记录变更历史';

CREATE TABLE `watchlist_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自选关系ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `asset_kind` VARCHAR(20) NOT NULL COMMENT '公共标的类型：FUND/STOCK',
    `asset_id` BIGINT NOT NULL COMMENT 'fund.id 或 stock_market.id',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_watchlist_item_user_asset` (`user_id`, `asset_kind`, `asset_id`),
    KEY `idx_watchlist_item_user_kind` (`user_id`, `asset_kind`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自选公共资产关系';

CREATE TABLE `exchange_rate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '当前汇率ID',
    `base_currency` VARCHAR(3) NOT NULL COMMENT '基准币种；当前仅允许外币',
    `quote_currency` VARCHAR(3) NOT NULL COMMENT '报价币种；当前固定CNY',
    `rate` DECIMAL(24, 10) NOT NULL COMMENT '1单位基准币种兑换报价币种数量',
    `source` VARCHAR(100) DEFAULT NULL COMMENT '汇率来源',
    `source_as_of` DATETIME DEFAULT NULL COMMENT '来源数据时点',
    `fetched_at` DATETIME DEFAULT NULL COMMENT '获取时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exchange_rate_pair` (`base_currency`, `quote_currency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='当前汇率';

INSERT INTO `asset_catalog`
    (`user_id`, `parent_id`, `catalog_code`, `catalog_name`, `catalog_scope`, `balance_direction`, `sort_order`, `status`)
VALUES
    (NULL, NULL, 'CASH', '现金', 'SYSTEM', 'ADD', 10, 'ENABLED'),
    (NULL, NULL, 'BANK_CARD', '银行卡', 'SYSTEM', 'ADD', 20, 'ENABLED'),
    (NULL, NULL, 'VIRTUAL_BALANCE', '虚拟余额', 'SYSTEM', 'ADD', 30, 'ENABLED'),
    (NULL, NULL, 'INVESTMENT_ASSET', '投资资产', 'SYSTEM', 'ADD', 40, 'ENABLED'),
    (NULL, NULL, 'CLAIM', '债权', 'SYSTEM', 'ADD', 50, 'ENABLED'),
    (NULL, NULL, 'LIABILITY', '负债', 'SYSTEM', 'SUBTRACT', 60, 'ENABLED')
ON DUPLICATE KEY UPDATE
    `catalog_name` = VALUES(`catalog_name`),
    `balance_direction` = VALUES(`balance_direction`),
    `sort_order` = VALUES(`sort_order`),
    `status` = 'ENABLED';

SET @investment_asset_catalog_id := (
    SELECT `id` FROM `asset_catalog` WHERE `catalog_code` = 'INVESTMENT_ASSET' LIMIT 1
);

INSERT INTO `asset_catalog`
    (`user_id`, `parent_id`, `catalog_code`, `catalog_name`, `catalog_scope`, `balance_direction`, `sort_order`, `status`)
VALUES
    (NULL, @investment_asset_catalog_id, 'FUND', '基金', 'SYSTEM', 'ADD', 10, 'ENABLED'),
    (NULL, @investment_asset_catalog_id, 'STOCK', '股票', 'SYSTEM', 'ADD', 20, 'ENABLED')
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `catalog_name` = VALUES(`catalog_name`),
    `balance_direction` = VALUES(`balance_direction`),
    `sort_order` = VALUES(`sort_order`),
    `status` = 'ENABLED';

INSERT INTO `exchange_rate`
    (`base_currency`, `quote_currency`, `rate`, `source`, `source_as_of`, `fetched_at`)
VALUES
    ('USD', 'CNY', 7.2000000000, 'system_seed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('HKD', 'CNY', 0.9200000000, 'system_seed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('JPY', 'CNY', 0.0480000000, 'system_seed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    `rate` = VALUES(`rate`),
    `source` = VALUES(`source`),
    `source_as_of` = VALUES(`source_as_of`),
    `fetched_at` = VALUES(`fetched_at`),
    `update_time` = CURRENT_TIMESTAMP;

-- ----------------------------
-- 处理任务表
-- ----------------------------
DROP TABLE IF EXISTS `processing_task`;
CREATE TABLE `processing_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '处理任务ID',
    `server_task_id` VARCHAR(100) NOT NULL COMMENT 'server任务标识',
    `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型：基金切片刷新/A股行情/美股行情',
    `task_params_json` TEXT DEFAULT NULL COMMENT '安全任务参数摘要JSON',
    `active_key` VARCHAR(200) DEFAULT NULL COMMENT 'single-flight 活动业务键',
    `lease_until` DATETIME DEFAULT NULL COMMENT '活动业务键租约截止时间',
    `status` VARCHAR(30) NOT NULL DEFAULT 'created' COMMENT '状态：created/dispatched/running/succeeded/partial_failed/failed/dispatch_failed/callback_failed',
    `error_summary` VARCHAR(1000) DEFAULT NULL COMMENT '安全错误摘要',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_processing_task_server_task_id` (`server_task_id`),
    UNIQUE KEY `uk_processing_task_active_key` (`active_key`),
    KEY `idx_processing_task_type_status` (`task_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处理任务表';

-- ----------------------------
-- 处理回调幂等记录表
-- ----------------------------
DROP TABLE IF EXISTS `processing_callback`;
CREATE TABLE `processing_callback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '回调幂等记录ID',
    `server_task_id` VARCHAR(100) NOT NULL COMMENT 'server任务标识',
    `idempotency_key` VARCHAR(200) NOT NULL COMMENT '幂等键',
    `callback_status` VARCHAR(30) NOT NULL COMMENT '回调状态',
    `process_status` VARCHAR(30) NOT NULL DEFAULT 'created' COMMENT '处理状态：created/processing/processed/failed',
    `error_summary` VARCHAR(1000) DEFAULT NULL COMMENT '安全错误摘要',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_processing_callback_task_key` (`server_task_id`, `idempotency_key`),
    KEY `idx_processing_callback_server_task_id` (`server_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处理回调幂等记录表';

-- ----------------------------
-- 基金表
-- ----------------------------
DROP TABLE IF EXISTS `fund`;
CREATE TABLE `fund` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '基金ID',
    `fund_code` VARCHAR(50) NOT NULL COMMENT '基金代码',
    `fund_name` VARCHAR(200) NOT NULL COMMENT '基金名称',
    `fund_type` VARCHAR(100) DEFAULT NULL COMMENT '基金类型',
    `pinyin_abbr` VARCHAR(100) DEFAULT NULL COMMENT '基金名称拼音缩写',
    `pinyin_full` VARCHAR(500) DEFAULT NULL COMMENT '基金名称完整拼音',
    `buy_status` VARCHAR(30) NOT NULL DEFAULT 'unknown' COMMENT '申购状态：open/closed/limited/suspended/unknown',
    `daily_purchase_limit` VARCHAR(200) DEFAULT NULL COMMENT '单日申购限额展示文本',
    `return_coverage_status` VARCHAR(30) NOT NULL DEFAULT 'unknown' COMMENT '收益覆盖状态：covered/source_not_covered/unknown',
    `returns_as_of` DATE DEFAULT NULL COMMENT '涨跌幅数据日期',
    `unit_nav` DECIMAL(18, 6) DEFAULT NULL COMMENT '单位净值',
    `accumulated_nav` DECIMAL(18, 6) DEFAULT NULL COMMENT '累计净值',
    `daily_growth_rate` DECIMAL(12, 4) DEFAULT NULL COMMENT '日增长率（百分点）',
    `top_holdings_as_of` DATE DEFAULT NULL COMMENT '重仓披露日期',
    `public_holdings_status` VARCHAR(50) NOT NULL DEFAULT 'missing' COMMENT '公开重仓状态：public/no_public_stock_holdings/missing',
    `asset_allocation_as_of` DATE DEFAULT NULL COMMENT '资产配置报告期',
    `asset_allocation_status` VARCHAR(20) NOT NULL DEFAULT 'missing' COMMENT '资产配置状态：available/unavailable/missing',
    `one_month_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近1月涨跌幅',
    `three_months_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近3月涨跌幅',
    `six_months_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近6月涨跌幅',
    `one_year_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近1年涨跌幅',
    `three_years_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近3年涨跌幅',
    `catalog_fetched_at` DATETIME DEFAULT NULL COMMENT '基金清单信息最近成功获取时间',
    `purchase_status_fetched_at` DATETIME DEFAULT NULL COMMENT '基金申购信息最近成功获取时间',
    `period_return_fetched_at` DATETIME DEFAULT NULL COMMENT '基金阶段收益信息最近成功获取时间',
    `top_holding_fetched_at` DATETIME DEFAULT NULL COMMENT '基金重仓信息最近成功获取时间',
    `asset_allocation_fetched_at` DATETIME DEFAULT NULL COMMENT '基金资产配置最近认可获取时间',
    `last_detail_view_time` DATETIME DEFAULT NULL COMMENT '基金详情最近查看时间，仅用于公共数据刷新目标',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fund_fund_code` (`fund_code`),
    KEY `idx_fund_last_detail_view_time` (`last_detail_view_time`),
    KEY `idx_fund_asset_allocation_refresh` (`asset_allocation_status`, `asset_allocation_fetched_at`, `asset_allocation_as_of`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金表';

-- ----------------------------
-- 基金当前前十大重仓表
-- ----------------------------
DROP TABLE IF EXISTS `fund_top_holding`;
CREATE TABLE `fund_top_holding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '基金重仓ID',
    `fund_code` VARCHAR(50) NOT NULL COMMENT '基金代码',
    `rank_no` INT NOT NULL COMMENT '重仓排名',
    `stock_name` VARCHAR(100) DEFAULT NULL COMMENT '股票简称',
    `stock_code` VARCHAR(50) DEFAULT NULL COMMENT '股票代码',
    `market` VARCHAR(20) DEFAULT NULL COMMENT '业务市场：A_SHARE/US_STOCK',
    `provider_market_code` VARCHAR(20) DEFAULT NULL COMMENT '第三方数据源市场编码',
    `holding_ratio` DECIMAL(12, 4) DEFAULT NULL COMMENT '持仓占比',
    `quarter_change_type` VARCHAR(30) NOT NULL DEFAULT 'unknown' COMMENT '较上季度变化类型：new/increased/decreased/unchanged/removed/not_applicable/unknown',
    `quarter_change_value` DECIMAL(12, 4) DEFAULT NULL COMMENT '较上季度变化值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fund_top_holding_fund_rank` (`fund_code`, `rank_no`),
    KEY `idx_fund_top_holding_fund_code` (`fund_code`),
    KEY `idx_fund_top_holding_stock_code` (`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金当前前十大重仓表';

-- ----------------------------
-- 基金当前资产配置表
-- ----------------------------
DROP TABLE IF EXISTS `fund_asset_allocation`;
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

-- ----------------------------
-- 股票当前行情表
-- ----------------------------
DROP TABLE IF EXISTS `stock_market`;
CREATE TABLE `stock_market` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '股票行情ID',
    `stock_code` VARCHAR(50) NOT NULL COMMENT '股票代码',
    `market` VARCHAR(20) NOT NULL COMMENT '业务市场：A_SHARE/US_STOCK',
    `exchange_code` VARCHAR(20) DEFAULT NULL COMMENT '交易所归属代码：SH/SZ/BJ 等',
    `provider_market_code` VARCHAR(20) DEFAULT NULL COMMENT '数据源市场编码',
    `stock_name` VARCHAR(100) DEFAULT NULL COMMENT '股票简称',
    `currency` VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '价格、成交额和市值币种：CNY/USD',
    `volume_unit` VARCHAR(20) NOT NULL DEFAULT 'LOT' COMMENT '成交量单位：LOT/SHARE',
    `latest_price` DECIMAL(20, 4) DEFAULT NULL COMMENT '最新价',
    `change_percent` DECIMAL(12, 4) DEFAULT NULL COMMENT '涨跌幅，单位为百分点',
    `change_amount` DECIMAL(20, 4) DEFAULT NULL COMMENT '涨跌额',
    `volume` BIGINT DEFAULT NULL COMMENT '成交量',
    `turnover_amount` DECIMAL(24, 4) DEFAULT NULL COMMENT '成交额',
    `amplitude` DECIMAL(12, 4) DEFAULT NULL COMMENT '振幅，单位为百分点',
    `high_price` DECIMAL(20, 4) DEFAULT NULL COMMENT '最高价',
    `low_price` DECIMAL(20, 4) DEFAULT NULL COMMENT '最低价',
    `open_price` DECIMAL(20, 4) DEFAULT NULL COMMENT '今开',
    `previous_close` DECIMAL(20, 4) DEFAULT NULL COMMENT '昨收',
    `volume_ratio` DECIMAL(12, 4) DEFAULT NULL COMMENT '量比',
    `turnover_rate` DECIMAL(12, 4) DEFAULT NULL COMMENT '换手率，单位为百分点',
    `pe_dynamic` DECIMAL(12, 4) DEFAULT NULL COMMENT '市盈率-动态',
    `pe_ratio` DECIMAL(12, 4) DEFAULT NULL COMMENT '市盈率',
    `pb_ratio` DECIMAL(12, 4) DEFAULT NULL COMMENT '市净率',
    `total_market_value` DECIMAL(24, 4) DEFAULT NULL COMMENT '总市值',
    `circulating_market_value` DECIMAL(24, 4) DEFAULT NULL COMMENT '流通市值',
    `speed` DECIMAL(12, 4) DEFAULT NULL COMMENT '涨速，单位为百分点',
    `five_minute_change` DECIMAL(12, 4) DEFAULT NULL COMMENT '5分钟涨跌，单位为百分点',
    `sixty_day_change_percent` DECIMAL(12, 4) DEFAULT NULL COMMENT '60日涨跌幅，单位为百分点',
    `year_to_date_change_percent` DECIMAL(12, 4) DEFAULT NULL COMMENT '年初至今涨跌幅，单位为百分点',
    `listing_date` DATE DEFAULT NULL COMMENT '上市日期',
    `status` VARCHAR(30) NOT NULL DEFAULT 'active' COMMENT '状态：active/missing_from_refresh',
    `refreshed_at` DATETIME DEFAULT NULL COMMENT '本批行情刷新时间',
    `last_detail_view_time` DATETIME DEFAULT NULL COMMENT '股票详情最近查看时间，仅用于公共数据刷新目标',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_market_code_market` (`stock_code`, `market`),
    KEY `idx_stock_market_market_status` (`market`, `status`),
    KEY `idx_stock_market_refreshed_at` (`refreshed_at`),
    KEY `idx_stock_market_last_detail_view_time` (`last_detail_view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票市场当前行情表';

-- ----------------------------
-- 基金净值历史表
-- ----------------------------
DROP TABLE IF EXISTS `fund_nav_history`;
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
    UNIQUE KEY `uk_fund_nav_history_code_date` (`fund_code`,`nav_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金净值历史';

-- ----------------------------
-- 基金阶段业绩表
-- ----------------------------
DROP TABLE IF EXISTS `fund_period_performance`;
CREATE TABLE `fund_period_performance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '基金阶段业绩ID',
    `fund_code` VARCHAR(50) NOT NULL COMMENT '基金代码',
    `period` VARCHAR(10) NOT NULL COMMENT '阶段：1m/3m/6m/1y/3y',
    `fund_return` DECIMAL(12,4) DEFAULT NULL COMMENT '基金收益率，单位为百分点',
    `peer_average` DECIMAL(12,4) DEFAULT NULL COMMENT '同类平均收益率，单位为百分点',
    `peer_rank` INT DEFAULT NULL COMMENT '同类排名',
    `peer_total` INT DEFAULT NULL COMMENT '同类样本总数',
    `rank_change` INT DEFAULT NULL COMMENT '排名变化',
    `as_of` DATE NOT NULL COMMENT '同源快照日期',
    `fetched_at` DATETIME NOT NULL COMMENT '数据获取时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fund_period_performance_code_period` (`fund_code`,`period`),
    KEY `idx_fund_period_performance_code_as_of` (`fund_code`,`as_of`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金阶段业绩同源快照';

-- ----------------------------
-- 基金详情 slice 刷新状态表
-- ----------------------------
DROP TABLE IF EXISTS `market_detail_slice_state`;
CREATE TABLE `market_detail_slice_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '市场详情 slice 状态ID',
    `fund_code` VARCHAR(50) NOT NULL COMMENT '基金代码',
    `slice_type` VARCHAR(50) NOT NULL COMMENT 'nav_history/period_performance',
    `status` VARCHAR(20) NOT NULL COMMENT 'missing/refreshing/available/empty/failed',
    `active_task_id` VARCHAR(100) DEFAULT NULL COMMENT '当前刷新任务ID',
    `last_attempt_at` DATETIME DEFAULT NULL COMMENT '最近尝试时间',
    `last_success_at` DATETIME DEFAULT NULL COMMENT '最近成功时间',
    `error_summary` VARCHAR(500) DEFAULT NULL COMMENT '安全错误摘要',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_market_detail_slice_state_fund_slice` (`fund_code`,`slice_type`),
    KEY `idx_market_detail_slice_state_active_task` (`active_task_id`),
    KEY `idx_market_detail_slice_state_status_attempt` (`status`,`last_attempt_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金详情 slice 刷新状态';

-- ----------------------------
-- 股票详情 slice 刷新状态表
-- ----------------------------
DROP TABLE IF EXISTS `stock_detail_slice_state`;
CREATE TABLE `stock_detail_slice_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '股票详情 slice 状态ID',
    `asset_ref` VARCHAR(120) NOT NULL COMMENT '统一股票资产引用',
    `slice_type` VARCHAR(50) NOT NULL COMMENT 'price_history/company_profile',
    `status` VARCHAR(20) NOT NULL COMMENT 'missing/refreshing/available/empty/failed',
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

-- ----------------------------
-- 股票价格时间序列表
-- ----------------------------
DROP TABLE IF EXISTS `stock_price_bar`;
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

-- ----------------------------
-- 股票公司资料表
-- ----------------------------
DROP TABLE IF EXISTS `stock_company_profile`;
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

-- ----------------------------
-- 处理日志表
-- ----------------------------
DROP TABLE IF EXISTS `processing_log`;
CREATE TABLE `processing_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '处理日志ID',
    `source_ref_id` VARCHAR(100) DEFAULT NULL COMMENT '来源引用ID',
    `module` VARCHAR(50) NOT NULL COMMENT '处理模块',
    `event` VARCHAR(100) NOT NULL COMMENT '处理事件',
    `message` VARCHAR(1000) NOT NULL COMMENT '安全诊断消息',
    `severity` VARCHAR(20) NOT NULL DEFAULT 'warning' COMMENT '级别：info/warning/error',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_processing_log_source_ref_id` (`source_ref_id`),
    KEY `idx_processing_log_module_event` (`module`, `event`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处理日志表';

-- ----------------------------
-- 独立收支条目表
-- ----------------------------
DROP TABLE IF EXISTS `bookkeeping_user_category_config`;
DROP TABLE IF EXISTS `bookkeeping_category`;
CREATE TABLE `bookkeeping_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记账类别ID',
    `code` VARCHAR(50) NOT NULL COMMENT '稳定类别代码',
    `scope` VARCHAR(10) NOT NULL COMMENT 'SYSTEM/USER',
    `owner_user_id` BIGINT DEFAULT NULL COMMENT '用户类别所有者；系统类别为空',
    `entry_type` VARCHAR(20) NOT NULL COMMENT 'EXPENSE/INCOME',
    `name` VARCHAR(16) NOT NULL COMMENT '类别名称',
    `icon_key` VARCHAR(64) NOT NULL COMMENT '客户端内置图标稳定键',
    `default_enabled` TINYINT(1) NOT NULL COMMENT '无用户配置时是否启用',
    `default_sort_order` INT NOT NULL COMMENT '无用户配置时的顺序',
    `create_request_id` VARCHAR(64) DEFAULT NULL COMMENT '用户类别创建幂等键',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bookkeeping_category_code` (`code`),
    UNIQUE KEY `uk_bookkeeping_category_owner_request` (`owner_user_id`, `create_request_id`),
    UNIQUE KEY `uk_bookkeeping_category_owner_type_name` (`owner_user_id`, `entry_type`, `name`),
    KEY `idx_bookkeeping_category_visible` (`entry_type`, `scope`, `owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记账类别定义';

CREATE TABLE `bookkeeping_user_category_config` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `category_id` BIGINT NOT NULL COMMENT '类别ID',
    `status` VARCHAR(20) NOT NULL COMMENT 'ENABLED/DISABLED',
    `sort_order` INT NOT NULL COMMENT '用户类别顺序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `category_id`),
    KEY `idx_bookkeeping_category_config_order` (`user_id`, `status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户记账类别配置';

INSERT INTO `bookkeeping_category`
    (`code`, `scope`, `entry_type`, `name`, `icon_key`, `default_enabled`, `default_sort_order`)
VALUES
    ('FOOD', 'SYSTEM', 'EXPENSE', '餐饮', 'food', 1, 10),
    ('SHOPPING', 'SYSTEM', 'EXPENSE', '购物', 'shopping', 1, 20),
    ('DAILY', 'SYSTEM', 'EXPENSE', '日用', 'daily', 1, 30),
    ('TRANSPORT', 'SYSTEM', 'EXPENSE', '交通', 'transport', 1, 40),
    ('VEGETABLE', 'SYSTEM', 'EXPENSE', '买菜', 'vegetable', 0, 50),
    ('FRUIT', 'SYSTEM', 'EXPENSE', '水果', 'fruit', 0, 60),
    ('SNACK', 'SYSTEM', 'EXPENSE', '零食', 'snack', 0, 70),
    ('SPORT', 'SYSTEM', 'EXPENSE', '运动', 'sport', 0, 80),
    ('ENTERTAINMENT', 'SYSTEM', 'EXPENSE', '娱乐', 'entertainment', 1, 90),
    ('COMMUNICATION', 'SYSTEM', 'EXPENSE', '通讯', 'communication', 1, 100),
    ('CLOTHING', 'SYSTEM', 'EXPENSE', '服饰', 'clothing', 1, 110),
    ('BEAUTY', 'SYSTEM', 'EXPENSE', '美妆', 'beauty', 0, 120),
    ('HOUSING', 'SYSTEM', 'EXPENSE', '住房', 'housing', 1, 130),
    ('HOME', 'SYSTEM', 'EXPENSE', '家居', 'home', 0, 140),
    ('MEDICAL', 'SYSTEM', 'EXPENSE', '医疗', 'medical', 1, 150),
    ('OTHER_EXPENSE', 'SYSTEM', 'EXPENSE', '其他支出', 'other-expense', 1, 160),
    ('SALARY', 'SYSTEM', 'INCOME', '工资', 'salary', 1, 10),
    ('BONUS', 'SYSTEM', 'INCOME', '奖金', 'bonus', 1, 20),
    ('PART_TIME', 'SYSTEM', 'INCOME', '兼职', 'part-time', 1, 30),
    ('BUSINESS', 'SYSTEM', 'INCOME', '经营', 'business', 0, 40),
    ('INVESTMENT_INCOME', 'SYSTEM', 'INCOME', '投资收益', 'investment-income', 1, 50),
    ('REIMBURSEMENT', 'SYSTEM', 'INCOME', '报销', 'reimbursement', 0, 60),
    ('OTHER_INCOME', 'SYSTEM', 'INCOME', '其他收入', 'other-income', 1, 70);

DROP TABLE IF EXISTS `bookkeeping_entry`;
CREATE TABLE `bookkeeping_entry` (
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

SET FOREIGN_KEY_CHECKS = 1;
