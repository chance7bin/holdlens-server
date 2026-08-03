-- 基金净值历史唯一索引已覆盖同列组合查询，删除重复普通索引以降低批量 upsert 的索引维护成本。

SET @holdlens_drop_fund_nav_index_sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE `fund_nav_history` DROP INDEX `idx_fund_nav_history_code_date`',
        'SELECT 1'
    )
    FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'fund_nav_history'
      AND `index_name` = 'idx_fund_nav_history_code_date'
);

PREPARE holdlens_drop_fund_nav_index_stmt FROM @holdlens_drop_fund_nav_index_sql;
EXECUTE holdlens_drop_fund_nav_index_stmt;
DEALLOCATE PREPARE holdlens_drop_fund_nav_index_stmt;
