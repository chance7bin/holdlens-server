## Why

账号资产第一版需要支持用户把已存在的基金或股票批量加入自选。当前系统已经有 `asset_info`、`fund_detail_item`、`stock_market_current` 和 agent 异步刷新链路，但用户自选能力应只维护用户维度关系，不应在同一个接口中注册公开数据刷新目标或触发刷新任务。

本变更不直接关联现有 `docs/requirements/**/prd-*.md`。需求来源为本次围绕账号资产功能的设计讨论，并已通过 `docs/decisions/adr-004-user-watchlist-fund-stock.md` 固化关键领域决策。

## What Changes

- 新增自选资产批量添加能力，接口路径为 `POST /api/watchlist/assets/batch-add`。
- 批量添加只写入或保持 `asset_info` 用户自选关系，不创建当前持仓，不写 `asset_holding` 或 `asset_holding_change`。
- 批量添加前必须校验公开数据表中已存在目标资产：
  - 基金必须存在于 `fund_detail_item`。
  - 股票必须存在于 `stock_market_current`，按 `stock_code + market` 组合匹配。
- 股票 `market` 暂时允许为空；空 market 只匹配 `stock_market_current` 中空 market 的既有记录。
- 本变更不调整 `asset_info` 表结构，继续沿用现有字段和 `user_id + asset_code + asset_kind` 唯一身份。
- 批量添加接口响应只返回 `invalidItems`；不暴露新建数、已存在数、刷新任务 ID 或刷新状态。
- 同一请求内重复提交同一资产按幂等成功处理，不进入 `invalidItems`。
- 批量添加不写入 `fund_detail_item` / `stock_market_current`，不注册刷新目标，不创建或触发基金详情刷新/股票行情刷新任务。
- 命名统一为 `WatchlistAsset` / `BatchAdd` 语义，避免继续使用持仓或导入暗示。

## Capabilities

### New Capabilities

- `watchlist-assets`: 用户将已存在的基金/股票批量加入自选。

### Modified Capabilities

- 无。

## Impact

- 影响 HTTP API：新增或调整自选资产批量添加接口及请求/响应 DTO，路径为 `POST /api/watchlist/assets/batch-add`。
- 影响 Case 层：新增或调整批量加入自选用例，负责归一化、公开资产存在性校验、请求内去重、`invalidItems` 构建和 `asset_info` 写入。
- 影响 Domain 层：保留用户自选资产模型，补充公开基金/股票存在性查询所需 Repository 语义。
- 影响 Infrastructure 层：保留 `asset_info` upsert/查询能力，补充或复用 `fund_detail_item`、`stock_market_current` 存在性查询能力；不得在本用例中 upsert 公开数据目标。
- 不影响数据库表结构：本变更暂不调整 `asset_info`、`fund_detail_item`、`stock_market_current` DDL。
- 影响测试：需要覆盖批量添加幂等、无效项返回、基金/股票不存在校验、空 market 股票匹配、不会注册刷新目标、不会触发刷新任务。
> 历史变更说明：本变更中的旧自选存储模型已由 `replace-account-holdings-with-asset-records` 替代；当前实现使用 `watchlist_item`，本文仅保留原变更审计上下文。
