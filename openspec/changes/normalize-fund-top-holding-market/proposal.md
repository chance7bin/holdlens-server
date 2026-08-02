## Why

`fund_top_holding.market` 当前保存 Agent 透传的供应商市场码，而 `stock_market.market` 使用 `A_SHARE`、`US_STOCK` 业务枚举。基金详情按 `stock_code + market` 批量关联行情时无法命中，造成有行情的重仓股仍显示涨跌幅缺失。

## What Changes

- 接收 Agent 规范业务市场和新增的 `provider_market_code`。
- Server 只软校验 `market`：接受 `A_SHARE`、`US_STOCK` 或空值；其他值归空并记录 warning，不拒绝持仓行或整次 callback。
- `fund_top_holding` 新增 `provider_market_code`，Repository/PO/MyBatis 同步持久化，详情 API 不暴露供应商码。
- 提供一次性数据库迁移：保留历史原始市场码并把 `0/1` 转为 `A_SHARE`、`105/106/107` 转为 `US_STOCK`，其他值归空。
- Server 运行时不维护供应商码兼容映射；错误或未知业务市场只做软降级。

## Capabilities

### New Capabilities

- `fund-top-holding-market-normalization`：定义基金重仓业务市场校验、原始码持久化和历史迁移语义。

### Modified Capabilities

无。

## Impact

- 影响 API callback DTO、Trigger 映射、Case、基金聚合、Repository/PO、MyBatis XML、基线 SQL 和前向 migration。
- 同步根仓库 Agent -> Server callback 契约。
- 不改变 Client API 字段；完成迁移并发布 Agent/Server 后，现有 `stock_code + market` 行情关联可直接命中。
