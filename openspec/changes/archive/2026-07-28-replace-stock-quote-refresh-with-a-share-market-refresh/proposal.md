## Why

当前 `stock_quote_refresh` 链路由 server 扫描 `stock_market_current` 中的股票目标，再把 `stock_code + market` 列表分批下发给 agent 刷新。现在需要改为由 agent 直接调用 AKShare `stock_zh_a_spot_em()` 全量拉取沪深京 A 股实时行情，并由 server 作为长期事实源统一保存到重新设计后的 `stock_market` 表。

本变更同时修正现有股票表语义：旧表只保存 `daily_return`、`trade_date` 和 `quote_time` 等摘要字段，无法承载 AKShare 全量 A 股行情字段，也把“刷新目标”和“当前行情”混在同一旧契约中。

## What Changes

- **BREAKING**：废弃 server 侧旧 `stock_quote_refresh` 跨项目契约，包括旧任务创建接口、旧 agent 回调接口和按 `stock_market_current` 分批下发股票列表的调度链路。
- **BREAKING**：将 `stock_market_current` 重命名并重构为 `stock_market`，删除 `trade_date`、`daily_return`、`quote_time` 和 `market_key`。
- **BREAKING**：`stock_market` 唯一身份改为 `stock_code + market`，其中 `market` 为非空业务市场枚举，A 股全量同步统一写 `A_SHARE`。
- 新增 `exchange_code` 和 `provider_market_code` 字段，分别表达交易所归属代码和第三方数据源市场编码，二者不参与唯一约束。
- 新增 AKShare `stock_zh_a_spot_em()` 有效行情字段：最新价、涨跌幅、涨跌额、成交量、成交额、振幅、最高、最低、今开、昨收、量比、换手率、市盈率-动态、市净率、总市值、流通市值、涨速、5分钟涨跌、60日涨跌幅、年初至今涨跌幅。
- 新增 `refreshed_at` 字段，表达 agent 生成或回调本批 A 股行情的时间，不表达交易所逐笔行情时间。
- 新增 `status` 字段，第一版对本次返回股票写 `active`，暂不把历史存在但本次未返回的股票标记为缺失。
- 新增 `a_share_market_refresh` processing task、server 手动创建接口和 agent 回调处理能力。
- 新接口为 `POST /api/agent/a-share-market-refresh/tasks`，server 下发 agent `POST /tasks/a-share-market-refresh`，agent 回调 `/internal/agent/a-share-market-refresh/callback`。
- agent 回调一次性携带全量 A 股列表；server 校验任务和幂等键后按批次 upsert `stock_market`。
- 对外读侧同步移除旧 `dailyReturn` 语言，改为 `changePercent` 等新行情字段，不做兼容字段保留。
- 本次只创建 OpenSpec、契约和 ADR 规划产物，不修改生产代码；实现需等待用户明确授权。

## Capabilities

### New Capabilities

- `a-share-market-refresh-persistence`: server 创建 A 股全量行情刷新任务，接收 agent 全量回调，并将当前 A 股行情保存到 `stock_market`。

### Modified Capabilities

- 无。当前 `openspec/specs` 下暂无已归档能力；本变更在 change 内新增能力规格。

## Impact

- 影响模块：`holdlens-server-api`、`holdlens-server-trigger`、`holdlens-server-case`、`holdlens-server-domain`、`holdlens-server-infrastructure`、`holdlens-server-app`。
- 影响数据库：重构股票当前市场数据表为 `stock_market`，新增多项数值行情字段和 `refreshed_at`。
- 影响接口：新增 A 股全量行情刷新任务创建和回调接口，废弃旧 `stock_quote_refresh` server 契约。
- 影响查询：基金重仓或股票相关读侧需从 `stock_market.change_percent` 等新字段取值，不再使用 `daily_return`。
- 影响契约：需要与根目录 `contracts/` 和 agent change `add-a-share-market-refresh-task` 对齐 schema version、字段类型、状态和错误语义。
- 影响兼容：不兼容旧 `stock_market_current` 表结构、旧 `stock_quote_refresh` 回调 payload 和旧 `dailyReturn` API 字段。
- 需求来源：本次用户讨论收敛的 A 股全量行情同步方案；不直接关联 `docs/requirements/**/prd-*.md`，无需更新 PRD 状态。

## Non-Goals

- 不在 server 直接调用 AKShare、东方财富或其他外部行情接口。
- 不保存股票历史行情、分钟行情或逐笔行情。
- 不实现分片回调或结果文件拉取；第一版使用一次性全量回调。
- 不在第一版对“本次未返回”的历史股票做自动下架或缺失标记。
- 不保留旧 `stock_quote_refresh` 作为 server 跨项目任务契约。
