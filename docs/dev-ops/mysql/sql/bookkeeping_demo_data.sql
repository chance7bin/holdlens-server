-- 独立记账演示数据。
-- 默认写入 user_id=1；如需更换用户，只修改下一行。
-- 脚本可重复执行：相同 user_id + request_id 会更新并恢复为 ACTIVE，不会重复插入。

SET @bookkeeping_demo_user_id = 1;

START TRANSACTION;

INSERT INTO `bookkeeping_entry` (
    `user_id`,
    `request_id`,
    `type`,
    `category_code`,
    `amount`,
    `currency`,
    `entry_date`,
    `note`,
    `status`
) VALUES
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-001', 'INCOME',  'SALARY',            10800.00, 'CNY', '2024-01-15', '演示数据｜2024年1月工资', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-002', 'EXPENSE', 'HOUSING',            3200.00, 'CNY', '2024-01-18', '演示数据｜2024年住房支出', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-003', 'INCOME',  'INVESTMENT_INCOME',    680.00, 'CNY', '2024-06-10', '演示数据｜2024年投资收益', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-004', 'EXPENSE', 'MEDICAL',              560.00, 'CNY', '2024-06-22', '演示数据｜2024年医疗支出', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-005', 'INCOME',  'BONUS',               2600.00, 'CNY', '2024-12-20', '演示数据｜2024年年终奖金', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-006', 'EXPENSE', 'ENTERTAINMENT',         420.00, 'CNY', '2024-12-28', '演示数据｜2024年娱乐支出', 'ACTIVE'),

    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-007', 'INCOME',  'SALARY',             11600.00, 'CNY', '2025-01-10', '演示数据｜2025年1月工资', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-008', 'EXPENSE', 'HOUSING',             3500.00, 'CNY', '2025-01-16', '演示数据｜2025年1月房租', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-009', 'INCOME',  'PART_TIME',           1200.00, 'CNY', '2025-03-08', '演示数据｜2025年兼职收入', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-010', 'EXPENSE', 'SHOPPING',              860.00, 'CNY', '2025-03-18', '演示数据｜2025年购物支出', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-011', 'INCOME',  'BUSINESS',            2100.00, 'CNY', '2025-05-12', '演示数据｜2025年经营收入', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-012', 'EXPENSE', 'HOME',                  730.00, 'CNY', '2025-05-20', '演示数据｜2025年家居支出', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-013', 'INCOME',  'INVESTMENT_INCOME',    960.00, 'CNY', '2025-07-11', '演示数据｜2025年投资收益', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-014', 'EXPENSE', 'MEDICAL',               480.00, 'CNY', '2025-07-24', '演示数据｜2025年医疗支出', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-015', 'INCOME',  'BONUS',                3200.00, 'CNY', '2025-09-18', '演示数据｜2025年奖金', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-016', 'EXPENSE', 'TRANSPORT',             880.00, 'CNY', '2025-09-26', '演示数据｜2025年交通出行', 'ACTIVE'),

    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-017', 'INCOME',  'SALARY',             12800.00, 'CNY', '2026-01-10', '演示数据｜2026年1月工资', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-018', 'EXPENSE', 'HOUSING',             3650.00, 'CNY', '2026-01-17', '演示数据｜2026年1月住房', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-019', 'INCOME',  'SALARY',             12800.00, 'CNY', '2026-02-10', '演示数据｜2026年2月工资', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-020', 'EXPENSE', 'CLOTHING',            1280.00, 'CNY', '2026-02-14', '演示数据｜2026年2月服饰', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-021', 'INCOME',  'SALARY',             13200.00, 'CNY', '2026-03-10', '演示数据｜2026年3月工资', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-022', 'EXPENSE', 'MEDICAL',               760.00, 'CNY', '2026-03-21', '演示数据｜2026年3月医疗', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-023', 'INCOME',  'BUSINESS',            3500.00, 'CNY', '2026-04-10', '演示数据｜2026年4月经营收入', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-024', 'EXPENSE', 'HOME',                 1680.00, 'CNY', '2026-04-19', '演示数据｜2026年4月家居', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-025', 'INCOME',  'SALARY',             13500.00, 'CNY', '2026-05-10', '演示数据｜2026年5月工资', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-026', 'EXPENSE', 'BEAUTY',                920.00, 'CNY', '2026-05-25', '演示数据｜2026年5月美妆', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-027', 'INCOME',  'INVESTMENT_INCOME',   1880.00, 'CNY', '2026-06-10', '演示数据｜2026年6月投资收益', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-028', 'EXPENSE', 'SPORT',                1180.00, 'CNY', '2026-06-18', '演示数据｜2026年6月运动', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-029', 'INCOME',  'SALARY',             13800.00, 'CNY', '2026-07-10', '演示数据｜2026年7月工资', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-030', 'EXPENSE', 'ENTERTAINMENT',         860.00, 'CNY', '2026-07-22', '演示数据｜2026年7月娱乐', 'ACTIVE'),

    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-031', 'INCOME',  'SALARY',             15000.00, 'CNY', '2026-08-01', '演示数据｜8月工资', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-032', 'EXPENSE', 'HOUSING',             3800.00, 'CNY', '2026-08-01', '演示数据｜8月住房', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-033', 'EXPENSE', 'HOME',                  460.00, 'CNY', '2026-08-01', '演示数据｜家居用品', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-034', 'EXPENSE', 'FOOD',                   58.60, 'CNY', '2026-08-02', '演示数据｜周末聚餐', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-035', 'EXPENSE', 'SHOPPING',              299.00, 'CNY', '2026-08-02', '演示数据｜日常购物', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-036', 'INCOME',  'BONUS',                 800.00, 'CNY', '2026-08-02', '演示数据｜项目奖金', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-037', 'EXPENSE', 'TRANSPORT',              32.00, 'CNY', '2026-08-03', '演示数据｜通勤交通', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-038', 'EXPENSE', 'FOOD',                   45.00, 'CNY', '2026-08-03', '演示数据｜工作午餐', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-039', 'INCOME',  'PART_TIME',             600.00, 'CNY', '2026-08-03', '演示数据｜兼职收入', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-040', 'EXPENSE', 'VEGETABLE',              86.00, 'CNY', '2026-08-04', '演示数据｜购买蔬菜', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-041', 'EXPENSE', 'FRUIT',                  42.00, 'CNY', '2026-08-04', '演示数据｜购买水果', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-042', 'EXPENSE', 'COMMUNICATION',         129.00, 'CNY', '2026-08-04', '演示数据｜手机话费', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-043', 'EXPENSE', 'SHOPPING',              699.00, 'CNY', '2026-08-05', '演示数据｜数码配件', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-044', 'EXPENSE', 'MEDICAL',               238.00, 'CNY', '2026-08-05', '演示数据｜门诊药品', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-045', 'INCOME',  'REIMBURSEMENT',         238.00, 'CNY', '2026-08-05', '演示数据｜医疗报销', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-046', 'EXPENSE', 'FOOD',                   36.00, 'CNY', '2026-08-06', '演示数据｜今日午餐', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-047', 'EXPENSE', 'SNACK',                  18.50, 'CNY', '2026-08-06', '演示数据｜下午茶', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-048', 'EXPENSE', 'SPORT',                 199.00, 'CNY', '2026-08-06', '演示数据｜运动场地', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-049', 'EXPENSE', 'ENTERTAINMENT',         120.00, 'CNY', '2026-08-06', '演示数据｜电影票', 'ACTIVE'),
    (@bookkeeping_demo_user_id, 'demo-bookkeeping-20260806-050', 'INCOME',  'BUSINESS',             1200.00, 'CNY', '2026-08-06', '演示数据｜经营收入', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `type` = VALUES(`type`),
    `category_code` = VALUES(`category_code`),
    `amount` = VALUES(`amount`),
    `currency` = VALUES(`currency`),
    `entry_date` = VALUES(`entry_date`),
    `note` = VALUES(`note`),
    `status` = 'ACTIVE',
    `update_time` = CURRENT_TIMESTAMP;

COMMIT;

-- 验证演示数据数量，应返回 50。
SELECT COUNT(*) AS `active_demo_entry_count`
FROM `bookkeeping_entry`
WHERE `user_id` = @bookkeeping_demo_user_id
  AND `request_id` LIKE 'demo-bookkeeping-20260806-%'
  AND `status` = 'ACTIVE';
