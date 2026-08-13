CREATE TABLE IF NOT EXISTS `bookkeeping_category` (
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

CREATE TABLE IF NOT EXISTS `bookkeeping_user_category_config` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `category_id` BIGINT NOT NULL COMMENT '类别ID',
    `status` VARCHAR(20) NOT NULL COMMENT 'ENABLED/DISABLED',
    `sort_order` INT NOT NULL COMMENT '用户类别顺序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `category_id`),
    KEY `idx_bookkeeping_category_config_order` (`user_id`, `status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户记账类别配置';

INSERT IGNORE INTO `bookkeeping_category`
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

INSERT IGNORE INTO `bookkeeping_user_category_config`
    (`user_id`, `category_id`, `status`, `sort_order`)
SELECT
    e.user_id,
    c.id,
    'ENABLED',
    c.default_sort_order
FROM `bookkeeping_entry` e
JOIN `bookkeeping_category` c ON c.code = e.category_code
WHERE e.status = 'ACTIVE'
  AND c.default_enabled = 0;
