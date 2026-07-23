UPDATE `asset_catalog`
SET `catalog_name` = '银行卡',
    `update_time` = CURRENT_TIMESTAMP
WHERE `catalog_scope` = 'SYSTEM'
  AND `catalog_code` = 'BANK_CARD'
  AND `catalog_name` <> '银行卡';
