-- 基金详情补全前向迁移。回滚 Server 时保留公开市场事实和状态，避免破坏性删除。

CREATE TABLE IF NOT EXISTS `fund_period_performance` (
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

CREATE TABLE IF NOT EXISTS `market_detail_slice_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '市场详情 slice 状态ID',
    `fund_code` VARCHAR(50) NOT NULL COMMENT '基金代码',
    `slice_type` VARCHAR(50) NOT NULL COMMENT 'nav_history/period_performance',
    `status` VARCHAR(20) NOT NULL COMMENT 'available/refreshing/empty/failed',
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
