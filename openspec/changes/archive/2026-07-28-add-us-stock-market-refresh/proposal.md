## Why

agent 已提供 `holdlens.hlshare.stock.stock_us_spot_em()` 美股全量实时行情接口。server 当前只有 A 股全量行情刷新任务 `/api/agent/a-share-market-refresh/tasks`，无法创建、下发、接收和保存美股全量行情结果。

为保持“server 是长期事实源，agent 是数据处理运行时”的边界，需要在 server 侧补齐美股全量行情刷新全流程，并与 agent 和根目录 `contracts/` 对齐。

## What Changes

- 新增 `us_stock_market_refresh` processing task。
- 新增手动创建接口 `POST /api/agent/us-stock-market-refresh/tasks`。
- 新增 agent 下发端口，调用 `/tasks/us-stock-market-refresh`，schema version 为 `us-stock-market-refresh-task/v1`。
- 新增内部回调接口 `/internal/agent/us-stock-market-refresh/callback`，接收 `us-stock-market-refresh-result/v1`。
- 将回调中的 `market = US_STOCK` 股票写入 `stock_market`，继续以 `stock_code + market` 唯一。
- 扩展 `stock_market` 多市场字段语义，至少覆盖美股 `pe_ratio` 和 `listing_date`，避免复用 A 股 `pe_dynamic` 表达美股市盈率。
- 将 warning 和字段解析异常写入 `processing_log`，不把真实账户、持仓、凭据或完整资产明细写入任务摘要或日志。

## Impact

- 影响模块：`holdlens-server-api`、`holdlens-server-trigger`、`holdlens-server-case`、`holdlens-server-domain`、`holdlens-server-infrastructure`、`holdlens-server-app`。
- 影响契约：新增美股 task create、dispatch、callback 三份 contracts。
- 影响数据库：`stock_market` 需要支持 `US_STOCK` 和美股特有字段。
- 影响测试：新增或扩展 Controller、Case、Port/Repository 映射和 Mapper 测试。
- 非目标：不在 server 调用外部美股行情源，不保存历史行情，不实现分片回调，不改造 A 股现有任务为通用任务。
