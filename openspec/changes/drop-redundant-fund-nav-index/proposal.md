## Why

`fund_nav_history` 为同一列组合同时维护唯一索引和普通索引。唯一索引已覆盖现有按基金和日期的查询，额外普通索引会增加净值 upsert 的索引维护开销。

## What Changes

- 从新建库基线 DDL 中移除重复的普通索引 `idx_fund_nav_history_code_date`。
- 提供前向迁移，仅删除该重复索引，保留唯一索引 `uk_fund_nav_history_code_date`。
- 补充结构测试，防止相同列组合的重复索引再次进入基线 DDL。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `market-detail-data-persistence`: 基金净值历史表保持唯一的 `(fund_code, nav_date)` 索引用于幂等写入和查询。

## Impact

- 影响数据库基线 DDL、前向迁移和数据库结构测试。
- 不改变基金净值的数据模型、查询接口或 callback 语义。
