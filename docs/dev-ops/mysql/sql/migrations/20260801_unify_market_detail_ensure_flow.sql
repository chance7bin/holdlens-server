ALTER TABLE `stock_market`
    ADD COLUMN `last_detail_view_time` DATETIME DEFAULT NULL COMMENT '股票详情最近查看时间，仅用于公共数据刷新目标' AFTER `refreshed_at`,
    ADD INDEX `idx_stock_market_last_detail_view_time` (`last_detail_view_time`);

UPDATE `market_detail_slice_state`
SET `status` = 'missing'
WHERE `status` = 'failed'
  AND `last_attempt_at` IS NULL
  AND `last_success_at` IS NULL;

UPDATE `stock_detail_slice_state`
SET `status` = 'missing'
WHERE `status` = 'failed'
  AND `last_attempt_at` IS NULL
  AND `last_success_at` IS NULL;
