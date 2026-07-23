-- HoldLens 开发环境脱敏资产数据
-- Usage: run after holdlens.sql

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';
USE holdlens;

START TRANSACTION;

SET @mock_user_id := 10001;

DELETE FROM `asset_record_change` WHERE `user_id` = @mock_user_id;
DELETE FROM `watchlist_item` WHERE `user_id` = @mock_user_id;
DELETE FROM `asset_record` WHERE `user_id` = @mock_user_id;
DELETE FROM `asset_catalog` WHERE `catalog_scope` = 'USER' AND `user_id` = @mock_user_id;

INSERT INTO `fund` (`fund_code`, `fund_name`, `fund_type`)
VALUES ('DEMO-FUND-001', '示例基金', '示例类型')
ON DUPLICATE KEY UPDATE `fund_name` = VALUES(`fund_name`), `fund_type` = VALUES(`fund_type`);

INSERT INTO `stock_market`
    (`stock_code`, `market`, `stock_name`, `currency`, `volume_unit`, `status`)
VALUES ('DEMO', 'US_STOCK', '示例股票', 'USD', 'SHARE', 'active')
ON DUPLICATE KEY UPDATE
    `stock_name` = VALUES(`stock_name`),
    `currency` = VALUES(`currency`),
    `volume_unit` = VALUES(`volume_unit`),
    `status` = VALUES(`status`);

SET @cash_catalog_id := (SELECT `id` FROM `asset_catalog` WHERE `catalog_code` = 'CASH' LIMIT 1);
SET @fund_catalog_id := (SELECT `id` FROM `asset_catalog` WHERE `catalog_code` = 'FUND' LIMIT 1);
SET @stock_catalog_id := (SELECT `id` FROM `asset_catalog` WHERE `catalog_code` = 'STOCK' LIMIT 1);
SET @demo_fund_id := (SELECT `id` FROM `fund` WHERE `fund_code` = 'DEMO-FUND-001' LIMIT 1);
SET @demo_stock_id := (SELECT `id` FROM `stock_market` WHERE `stock_code` = 'DEMO' AND `market` = 'US_STOCK' LIMIT 1);

INSERT INTO `asset_record`
    (`user_id`, `catalog_id`, `record_name`, `asset_kind`, `asset_id`, `amount`, `currency`, `remark`, `status`)
VALUES
    (@mock_user_id, @cash_catalog_id, '日常现金', NULL, NULL, 2000.0000, 'CNY', NULL, 'ACTIVE'),
    (@mock_user_id, @fund_catalog_id, '未细分基金', 'FUND', NULL, 50000.0000, 'CNY', NULL, 'ACTIVE'),
    (@mock_user_id, @fund_catalog_id, '示例基金', 'FUND', @demo_fund_id, 10000.0000, 'CNY', '开发环境示例', 'ACTIVE'),
    (@mock_user_id, @stock_catalog_id, '示例股票', 'STOCK', @demo_stock_id, 1000.0000, 'USD', NULL, 'ACTIVE');

INSERT INTO `asset_record_change`
    (`operation_id`, `user_id`, `record_id`, `change_type`, `before_amount`, `after_amount`, `currency`, `before_status`, `after_status`, `operator_id`)
SELECT
    CONCAT('mock-create-', `id`), `user_id`, `id`, 'CREATE', NULL, `amount`, `currency`, NULL, `status`, `user_id`
FROM `asset_record`
WHERE `user_id` = @mock_user_id;

INSERT INTO `watchlist_item` (`user_id`, `asset_kind`, `asset_id`)
VALUES
    (@mock_user_id, 'FUND', @demo_fund_id),
    (@mock_user_id, 'STOCK', @demo_stock_id)
ON DUPLICATE KEY UPDATE `id` = LAST_INSERT_ID(`id`);

COMMIT;
