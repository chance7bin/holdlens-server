-- 基金刷新按数据切片拆分：前向迁移。
-- 本脚本只增加兼容列和索引，不删除或改写现有基金、持仓和任务数据。

ALTER TABLE `fund`
    ADD COLUMN `fund_type` VARCHAR(100) DEFAULT NULL COMMENT '基金类型' AFTER `fund_name`,
    ADD COLUMN `pinyin_abbr` VARCHAR(100) DEFAULT NULL COMMENT '基金名称拼音缩写' AFTER `fund_type`,
    ADD COLUMN `pinyin_full` VARCHAR(500) DEFAULT NULL COMMENT '基金名称完整拼音' AFTER `pinyin_abbr`,
    ADD COLUMN `return_coverage_status` VARCHAR(30) NOT NULL DEFAULT 'unknown' COMMENT '收益覆盖状态：covered/source_not_covered/unknown' AFTER `daily_purchase_limit`,
    ADD COLUMN `unit_nav` DECIMAL(18, 6) DEFAULT NULL COMMENT '单位净值' AFTER `returns_as_of`,
    ADD COLUMN `accumulated_nav` DECIMAL(18, 6) DEFAULT NULL COMMENT '累计净值' AFTER `unit_nav`,
    ADD COLUMN `daily_growth_rate` DECIMAL(12, 4) DEFAULT NULL COMMENT '日增长率（百分点）' AFTER `accumulated_nav`,
    ADD COLUMN `catalog_fetched_at` DATETIME DEFAULT NULL COMMENT '基金清单信息最近成功获取时间' AFTER `three_years_return`,
    ADD COLUMN `purchase_status_fetched_at` DATETIME DEFAULT NULL COMMENT '基金申购信息最近成功获取时间' AFTER `catalog_fetched_at`,
    ADD COLUMN `period_return_fetched_at` DATETIME DEFAULT NULL COMMENT '基金阶段收益信息最近成功获取时间' AFTER `purchase_status_fetched_at`,
    ADD COLUMN `top_holding_fetched_at` DATETIME DEFAULT NULL COMMENT '基金重仓信息最近成功获取时间' AFTER `period_return_fetched_at`,
    ADD COLUMN `last_detail_view_time` DATETIME DEFAULT NULL COMMENT '基金详情最近查看时间，仅用于公共数据刷新目标' AFTER `top_holding_fetched_at`,
    ADD INDEX `idx_fund_last_detail_view_time` (`last_detail_view_time`);
