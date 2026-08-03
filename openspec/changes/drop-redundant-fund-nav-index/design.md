## Context

基金净值历史按 `(fund_code, nav_date)` 幂等写入并按同一前缀查询。当前表同时维护完全相同列顺序的唯一索引和普通索引，后者不能提供额外查询能力，却会在每次净值 upsert 时增加索引维护。

## Goals / Non-Goals

**Goals:**

- 只保留能够保证幂等性并满足当前查询的唯一复合索引。
- 为已部署数据库提供最小、可重复执行的前向迁移。
- 防止基线 DDL 再次引入重复索引。

**Non-Goals:**

- 不修改净值记录、表字段、查询 SQL、事务边界或 callback 行为。
- 不删除任何业务数据。
- 不调整其他表的潜在索引问题。

## Decisions

### 1. 保留唯一索引，删除相同列组合的普通索引

保留 `uk_fund_nav_history_code_date (fund_code, nav_date)`，它同时满足同基金日期唯一性、按基金过滤、日期范围和升序遍历。删除 `idx_fund_nav_history_code_date`。不改为主键，避免无关的数据模型变更。

### 2. 使用独立前向迁移

新增独立迁移，通过 `information_schema.statistics` 判断索引是否存在，再使用 prepared statement 执行 `ALTER TABLE ... DROP INDEX`，避免依赖当前 MySQL 不支持的 `DROP INDEX IF EXISTS` 语法，也不改写历史迁移。新建库基线不再声明普通索引。该顺序同时适用于存量库和新库，并允许迁移重复执行。

### 3. 用结构测试守护基线与迁移意图

测试验证基线只声明唯一索引、新迁移只删除目标普通索引且不包含数据或表删除操作。它不以测试代替生产库的 `SHOW INDEX` 发布前核验。

## Risks / Trade-offs

- [部署库索引名被人工修改或索引已删除] → 迁移先查询 `information_schema.statistics`，不存在时执行无副作用语句；发布前仍建议执行 `SHOW INDEX FROM fund_nav_history` 核验。
- [未知查询依赖普通索引名] → 索引列与唯一索引完全相同；应用代码不应依赖物理索引名。
- [DDL 期间短暂元数据锁] → 在低峰发布，迁移只操作单个二级索引且不触碰数据。

## Migration Plan

1. 发布前在目标库执行 `SHOW INDEX FROM fund_nav_history`，确认两个索引的列顺序均为 `(fund_code, nav_date)`。
2. 执行前向迁移删除普通索引。
3. 验证唯一索引仍存在，并执行净值写入和查询回归。
4. 如需回滚，执行 `CREATE INDEX idx_fund_nav_history_code_date ON fund_nav_history (fund_code, nav_date)`；该操作可恢复索引，不影响数据。

## Open Questions

无。
