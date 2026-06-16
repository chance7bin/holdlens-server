-- =============================================
-- HoldLens 开发 mock 资产数据
-- Source: holdlens-agent/finance/my-account.md
-- Snapshot date: 2026-06-01
-- Usage: run after holdlens.sql
-- =============================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';
USE holdlens;

START TRANSACTION;

SET @mock_user_id := 10001;
SET @mock_source_ref_id := _utf8mb4'mock-my-account-20260601' COLLATE utf8mb4_unicode_ci;

-- ----------------------------
-- Mock accounts
-- ----------------------------
INSERT INTO `asset_account` (`user_id`, `account_name`, `account_type`, `status`, `remark`)
VALUES
    (@mock_user_id, '开发Mock-股票账户', 'stock', 'enabled', '来自账户资产清单 2026-06-01 的开发 mock 数据'),
    (@mock_user_id, '开发Mock-支付宝基金账户', 'fund', 'enabled', '来自账户资产清单 2026-06-01 的开发 mock 数据'),
    (@mock_user_id, '开发Mock-招行基金账户', 'fund', 'enabled', '来自账户资产清单 2026-06-01 的开发 mock 数据'),
    (@mock_user_id, '开发Mock-京东金融账户', 'unknown', 'enabled', '来自账户资产清单 2026-06-01 的开发 mock 数据'),
    (@mock_user_id, '开发Mock-现金账户', 'unknown', 'enabled', '来自账户资产清单 2026-06-01 的开发 mock 数据')
ON DUPLICATE KEY UPDATE
    `account_type` = VALUES(`account_type`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);

-- ----------------------------
-- Mock asset master data
-- ----------------------------
INSERT INTO `asset_info` (`user_id`, `asset_code`, `asset_name`, `asset_kind`, `asset_type`, `market`, `status`)
VALUES
    (@mock_user_id, '159941', '纳指ETF广发', 'fund', 'ETF', 'SZ', 'enabled'),
    (@mock_user_id, '159513', '纳斯达克100ETF大成', 'fund', 'ETF', 'SZ', 'enabled'),
    (@mock_user_id, '513650', '标普500ETF南方', 'fund', 'ETF', 'SH', 'enabled'),
    (@mock_user_id, '513300', '纳斯达克ETF华夏', 'fund', 'ETF', 'SH', 'enabled'),
    (@mock_user_id, '161130', '纳斯达克100LOF', 'fund', 'LOF', 'SZ', 'enabled'),
    (@mock_user_id, '601288', '农业银行', 'stock', '普通股票', 'SH', 'enabled'),
    (@mock_user_id, '601398', '工商银行', 'stock', '普通股票', 'SH', 'enabled'),
    (@mock_user_id, '159934', '黄金ETF易方达', 'fund', 'ETF', 'SZ', 'enabled'),
    (@mock_user_id, '270023', '广发全球精选股票(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '007280', '摩根日本精选股票(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '486002', '工银瑞信全球精选股票(QDII)', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '012920', '易方达全球成长精选混合(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '017730', '嘉实全球产业升级股票(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '539002', '建信新兴市场优选混合(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '018147', '建信新兴市场优选混合(QDII)C', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '378006', '摩根全球新兴市场混合(QDII)', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '040046', '华安纳斯达克100ETF联接(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '050025', '博时标普500ETF联接(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '270042', '广发纳斯达克100ETF联接(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '017641', '摩根标普500指数(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '007721', '天弘标普500(QDII-FOF)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '016532', '嘉实纳斯达克100ETF联接(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '006479', '广发纳斯达克100ETF联接(QDII)C', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '019547', '招商纳斯达克100ETF联接(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '019548', '招商纳斯达克100ETF联接(QDII)C', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '000834', '大成纳斯达克100ETF联接(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '003385', '工银瑞信全球美元债债券(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '004998', '长信全球债券(QDII)', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '009051', '易方达中证红利ETF联接A', 'fund', '开放式基金', 'OTC', 'enabled'),
    (@mock_user_id, '007466', '华泰柏瑞中证红利低波动ETF联接A', 'fund', '开放式基金', 'OTC', 'enabled'),
    (@mock_user_id, '018387', '华泰柏瑞中证港股通红利ETF联接A', 'fund', '开放式基金', 'OTC', 'enabled'),
    (@mock_user_id, '002611', '博时黄金ETF联接C', 'fund', '开放式基金', 'OTC', 'enabled'),
    (@mock_user_id, '000218', '国泰黄金ETF联接A', 'fund', '开放式基金', 'OTC', 'enabled'),
    (@mock_user_id, '005827', '易方达蓝筹精选混合', 'fund', '开放式基金', 'OTC', 'enabled'),
    (@mock_user_id, '164906', '交银施罗德中证海外中国互联网指数(QDII-LOF)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '012804', '广发恒生科技ETF联接(QDII)A', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '012805', '广发恒生科技ETF联接(QDII)C', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, '006480', '广发纳斯达克100ETF联接美元(QDII)C', 'fund', '开放式基金/QDII', 'OTC', 'enabled'),
    (@mock_user_id, 'JD_GOLD_CNY', '京东金融黄金', 'unknown', '黄金账户', NULL, 'enabled'),
    (@mock_user_id, 'CASH_CNY', '现金-CNY', 'cash', '现金', NULL, 'enabled')
ON DUPLICATE KEY UPDATE
    `asset_name` = VALUES(`asset_name`),
    `asset_type` = VALUES(`asset_type`),
    `market` = VALUES(`market`),
    `status` = VALUES(`status`);

-- ----------------------------
-- Mock current holdings
-- ----------------------------
INSERT INTO `asset_holding` (
    `user_id`, `account_id`, `asset_id`, `asset_category`, `holding_source`,
    `amount`, `currency`, `amount_display`, `amount_missing_reason`,
    `missing_reasons_json`, `status`
)
SELECT
    @mock_user_id,
    aa.`id`,
    ai.`id`,
    h.`asset_category`,
    h.`holding_source`,
    h.`amount`,
    'CNY',
    h.`amount_display`,
    NULL,
    NULL,
    'active'
FROM (
    SELECT _utf8mb4'开发Mock-股票账户' COLLATE utf8mb4_unicode_ci AS `account_name`, _utf8mb4'159941' COLLATE utf8mb4_unicode_ci AS `asset_code`, _utf8mb4'fund' COLLATE utf8mb4_unicode_ci AS `asset_kind`, _utf8mb4'ETF/指数基金' COLLATE utf8mb4_unicode_ci AS `asset_category`, _utf8mb4'stock_account' COLLATE utf8mb4_unicode_ci AS `holding_source`, 6703.20 AS `amount`, _utf8mb4'6,703.20' COLLATE utf8mb4_unicode_ci AS `amount_display`
    UNION ALL SELECT '开发Mock-股票账户', '159513', 'fund', 'ETF/指数基金', 'stock_account', 6755.00, '6,755.00'
    UNION ALL SELECT '开发Mock-股票账户', '513650', 'fund', 'ETF/指数基金', 'stock_account', 5681.10, '5,681.10'
    UNION ALL SELECT '开发Mock-股票账户', '513300', 'fund', 'ETF/指数基金', 'stock_account', 6303.00, '6,303.00'
    UNION ALL SELECT '开发Mock-股票账户', '161130', 'fund', 'ETF/指数基金', 'stock_account', 12818.00, '12,818.00'
    UNION ALL SELECT '开发Mock-股票账户', '601288', 'stock', '红利/低波', 'stock_account', 649.00, '649.00'
    UNION ALL SELECT '开发Mock-股票账户', '601398', 'stock', '红利/低波', 'stock_account', 2190.00, '2,190.00'
    UNION ALL SELECT '开发Mock-股票账户', '159934', 'fund', '黄金', 'stock_account', 1953.00, '1,953.00'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '270023', 'fund', '全球主动基金', 'fund_account', 38153.57, '38,153.57'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '007280', 'fund', '全球主动基金', 'fund_account', 16277.46, '16,277.46'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '486002', 'fund', '全球主动基金', 'fund_account', 5639.85, '5,639.85'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '012920', 'fund', '全球主动基金', 'fund_account', 8252.69, '8,252.69'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '017730', 'fund', '全球主动基金', 'fund_account', 12387.29, '12,387.29'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '539002', 'fund', '全球主动基金', 'fund_account', 7572.54, '7,572.54'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '018147', 'fund', '全球主动基金', 'fund_account', 1897.30, '1,897.30'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '378006', 'fund', '全球主动基金', 'fund_account', 11703.90, '11,703.90'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '040046', 'fund', 'ETF/指数基金', 'fund_account', 206.54, '206.54'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '050025', 'fund', 'ETF/指数基金', 'fund_account', 7295.80, '7,295.80'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '270042', 'fund', 'ETF/指数基金', 'fund_account', 10430.72, '10,430.72'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '017641', 'fund', 'ETF/指数基金', 'fund_account', 9040.98, '9,040.98'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '007721', 'fund', 'ETF/指数基金', 'fund_account', 532.76, '532.76'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '016532', 'fund', 'ETF/指数基金', 'fund_account', 2236.47, '2,236.47'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '006479', 'fund', 'ETF/指数基金', 'fund_account', 652.27, '652.27'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '019547', 'fund', 'ETF/指数基金', 'fund_account', 6474.20, '6,474.20'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '019548', 'fund', 'ETF/指数基金', 'fund_account', 4768.04, '4,768.04'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '000834', 'fund', 'ETF/指数基金', 'fund_account', 3222.86, '3,222.86'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '003385', 'fund', '债券', 'fund_account', 9803.97, '9,803.97'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '004998', 'fund', '债券', 'fund_account', 6212.65, '6,212.65'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '009051', 'fund', '红利/低波', 'fund_account', 3826.84, '3,826.84'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '007466', 'fund', '红利/低波', 'fund_account', 3996.00, '3,996.00'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '018387', 'fund', '红利/低波', 'fund_account', 3142.06, '3,142.06'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '002611', 'fund', '黄金', 'fund_account', 1.95, '1.95'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '000218', 'fund', '黄金', 'fund_account', 2283.98, '2,283.98'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '005827', 'fund', 'A/H/港股科技', 'fund_account', 106.13, '106.13'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '164906', 'fund', 'A/H/港股科技', 'fund_account', 85.65, '85.65'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '012804', 'fund', 'A/H/港股科技', 'fund_account', 770.22, '770.22'
    UNION ALL SELECT '开发Mock-支付宝基金账户', '012805', 'fund', 'A/H/港股科技', 'fund_account', 770.08, '770.08'
    UNION ALL SELECT '开发Mock-招行基金账户', '006480', 'fund', 'ETF/指数基金', 'fund_account', 1350.62, '1,350.62'
    UNION ALL SELECT '开发Mock-京东金融账户', 'JD_GOLD_CNY', 'unknown', '黄金', 'unknown', 3920.33, '3,920.33'
    UNION ALL SELECT '开发Mock-现金账户', 'CASH_CNY', 'cash', '现金', 'unknown', 10000.00, '10,000.00'
) h
JOIN `asset_account` aa
    ON aa.`user_id` = @mock_user_id
    AND aa.`account_name` COLLATE utf8mb4_unicode_ci = h.`account_name` COLLATE utf8mb4_unicode_ci
JOIN `asset_info` ai
    ON ai.`user_id` = @mock_user_id
    AND ai.`asset_code` COLLATE utf8mb4_unicode_ci = h.`asset_code` COLLATE utf8mb4_unicode_ci
    AND ai.`asset_kind` COLLATE utf8mb4_unicode_ci = h.`asset_kind` COLLATE utf8mb4_unicode_ci
ON DUPLICATE KEY UPDATE
    `asset_category` = VALUES(`asset_category`),
    `holding_source` = VALUES(`holding_source`),
    `amount` = VALUES(`amount`),
    `currency` = VALUES(`currency`),
    `amount_display` = VALUES(`amount_display`),
    `amount_missing_reason` = VALUES(`amount_missing_reason`),
    `missing_reasons_json` = VALUES(`missing_reasons_json`),
    `status` = VALUES(`status`);

-- ----------------------------
-- Optional import audit records
-- ----------------------------
INSERT INTO `asset_holding_change` (
    `user_id`, `holding_id`, `account_id`, `asset_id`, `change_type`,
    `before_amount`, `after_amount`, `currency`, `change_reason`,
    `source_type`, `source_ref_id`, `operator_id`
)
SELECT
    ah.`user_id`,
    ah.`id`,
    ah.`account_id`,
    ah.`asset_id`,
    'import',
    NULL,
    ah.`amount`,
    ah.`currency`,
    '开发 mock 账户资产清单导入',
    'manual',
    @mock_source_ref_id,
    @mock_user_id
FROM `asset_holding` ah
JOIN `asset_account` aa ON aa.`id` = ah.`account_id`
WHERE ah.`user_id` = @mock_user_id
  AND aa.`account_name` COLLATE utf8mb4_unicode_ci LIKE _utf8mb4'开发Mock-%' COLLATE utf8mb4_unicode_ci
  AND NOT EXISTS (
      SELECT 1
      FROM `asset_holding_change` ahc
      WHERE ahc.`holding_id` = ah.`id`
        AND ahc.`source_ref_id` COLLATE utf8mb4_unicode_ci = @mock_source_ref_id
        AND ahc.`change_type` COLLATE utf8mb4_unicode_ci = _utf8mb4'import' COLLATE utf8mb4_unicode_ci
  );

COMMIT;
