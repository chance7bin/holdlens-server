-- 基金重仓市场语义前向迁移：保留第三方原始码，并把 market 收敛为业务市场。

ALTER TABLE `fund_top_holding`
    ADD COLUMN `provider_market_code` VARCHAR(20) DEFAULT NULL COMMENT '第三方数据源市场编码' AFTER `market`;

UPDATE `fund_top_holding`
SET `provider_market_code` = `market`
WHERE `market` IS NOT NULL
  AND `market` NOT IN ('A_SHARE', 'US_STOCK');

UPDATE `fund_top_holding`
SET `market` = CASE
    WHEN `market` IN ('0', '1') THEN 'A_SHARE'
    WHEN `market` IN ('105', '106', '107') THEN 'US_STOCK'
    WHEN `market` IN ('A_SHARE', 'US_STOCK') THEN `market`
    ELSE NULL
END;
