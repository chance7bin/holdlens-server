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
-- 资产账户表
-- ----------------------------
DROP TABLE IF EXISTS `asset_account`;
CREATE TABLE `asset_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `account_name` VARCHAR(100) NOT NULL COMMENT '账户名称',
    `account_type` VARCHAR(20) NOT NULL DEFAULT 'unknown' COMMENT '账户类型：fund/stock/unknown',
    `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled/deleted',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_account_user_name` (`user_id`, `account_name`),
    KEY `idx_asset_account_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产账户表';

-- ----------------------------
-- 资产主数据表
-- ----------------------------
DROP TABLE IF EXISTS `asset_info`;
CREATE TABLE `asset_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资产ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `asset_code` VARCHAR(50) DEFAULT NULL COMMENT '资产代码',
    `asset_name` VARCHAR(200) NOT NULL COMMENT '资产名称',
    `asset_kind` VARCHAR(20) NOT NULL DEFAULT 'unknown' COMMENT '资产大类：fund/stock/cash/unknown',
    `asset_type` VARCHAR(100) DEFAULT NULL COMMENT '资产类型：ETF/LOF/开放式基金/普通股票等',
    `market` VARCHAR(20) DEFAULT NULL COMMENT '市场标识：SH/SZ/HK/US等',
    `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled/deleted',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_info_user_code_kind` (`user_id`, `asset_code`, `asset_kind`),
    KEY `idx_asset_info_user_name` (`user_id`, `asset_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产主数据表';

-- ----------------------------
-- 当前持仓表
-- ----------------------------
DROP TABLE IF EXISTS `asset_holding`;
CREATE TABLE `asset_holding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '持仓ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `account_id` BIGINT NOT NULL COMMENT '账户ID',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `asset_category` VARCHAR(100) DEFAULT NULL COMMENT '资产分类',
    `holding_source` VARCHAR(30) NOT NULL DEFAULT 'unknown' COMMENT '持仓来源账户类型：fund_account/stock_account/unknown',
    `amount` DECIMAL(20, 4) DEFAULT NULL COMMENT '持仓金额',
    `currency` VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    `amount_display` VARCHAR(100) DEFAULT NULL COMMENT '原始展示金额',
    `amount_missing_reason` VARCHAR(30) DEFAULT NULL COMMENT '金额缺失原因',
    `missing_reasons_json` TEXT DEFAULT NULL COMMENT '字段级缺失原因JSON',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/closed/deleted',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_holding_account_asset` (`user_id`, `account_id`, `asset_id`),
    KEY `idx_asset_holding_user_id` (`user_id`),
    KEY `idx_asset_holding_asset_id` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='当前持仓表';

-- ----------------------------
-- 持仓变更记录表
-- ----------------------------
DROP TABLE IF EXISTS `asset_holding_change`;
CREATE TABLE `asset_holding_change` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '变更记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `holding_id` BIGINT DEFAULT NULL COMMENT '持仓ID',
    `account_id` BIGINT NOT NULL COMMENT '账户ID',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `change_type` VARCHAR(30) NOT NULL COMMENT '变更类型：create/update/delete/import/ocr/agent',
    `before_amount` DECIMAL(20, 4) DEFAULT NULL COMMENT '变更前金额',
    `after_amount` DECIMAL(20, 4) DEFAULT NULL COMMENT '变更后金额',
    `currency` VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    `change_reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    `source_type` VARCHAR(30) NOT NULL DEFAULT 'manual' COMMENT '来源类型：manual/file_import/ocr/agent/api_sync',
    `source_ref_id` VARCHAR(100) DEFAULT NULL COMMENT '来源引用ID',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_asset_holding_change_user_id` (`user_id`),
    KEY `idx_asset_holding_change_holding_id` (`holding_id`),
    KEY `idx_asset_holding_change_asset_id` (`asset_id`),
    KEY `idx_asset_holding_change_source_ref_id` (`source_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持仓变更记录表';

-- ----------------------------
-- 处理任务表
-- ----------------------------
DROP TABLE IF EXISTS `processing_task`;
CREATE TABLE `processing_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '处理任务ID',
    `server_task_id` VARCHAR(100) NOT NULL COMMENT 'server任务标识',
    `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型：基金切片刷新/A股行情/美股行情',
    `task_params_json` TEXT DEFAULT NULL COMMENT '安全任务参数摘要JSON',
    `status` VARCHAR(30) NOT NULL DEFAULT 'created' COMMENT '状态：created/dispatched/running/succeeded/partial_failed/failed/dispatch_failed/callback_failed',
    `error_summary` VARCHAR(1000) DEFAULT NULL COMMENT '安全错误摘要',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_processing_task_server_task_id` (`server_task_id`),
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
    `process_status` VARCHAR(30) NOT NULL DEFAULT 'created' COMMENT '处理状态：created/processed/failed',
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
    `one_month_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近1月涨跌幅',
    `three_months_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近3月涨跌幅',
    `six_months_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近6月涨跌幅',
    `one_year_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近1年涨跌幅',
    `three_years_return` DECIMAL(12, 4) DEFAULT NULL COMMENT '近3年涨跌幅',
    `catalog_fetched_at` DATETIME DEFAULT NULL COMMENT '基金清单信息最近成功获取时间',
    `purchase_status_fetched_at` DATETIME DEFAULT NULL COMMENT '基金申购信息最近成功获取时间',
    `period_return_fetched_at` DATETIME DEFAULT NULL COMMENT '基金阶段收益信息最近成功获取时间',
    `top_holding_fetched_at` DATETIME DEFAULT NULL COMMENT '基金重仓信息最近成功获取时间',
    `last_detail_view_time` DATETIME DEFAULT NULL COMMENT '基金详情最近查看时间，仅用于公共数据刷新目标',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fund_fund_code` (`fund_code`),
    KEY `idx_fund_last_detail_view_time` (`last_detail_view_time`)
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
    `market` VARCHAR(20) DEFAULT NULL COMMENT '市场标识',
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
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_market_code_market` (`stock_code`, `market`),
    KEY `idx_stock_market_market_status` (`market`, `status`),
    KEY `idx_stock_market_refreshed_at` (`refreshed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='股票市场当前行情表';

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

SET FOREIGN_KEY_CHECKS = 1;
