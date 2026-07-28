## Why

首期客户端需要展示自选首页、统一搜索和股票当前详情。当前 server 只有旧字段版批量加入自选和基金详情接口，没有自选列表、统一搜索、股票详情，也会迫使客户端理解股票 `market`。本变更以根目录 `contracts/holdlens-server/client/` 和更新后的 ADR-004 为输入，为客户端提供稳定的市场资产业务 API。

本变更不直接关联现有 `docs/requirements/**/prd-*.md`；需求来源是首期四页面设计和已确认的跨项目契约。

## What Changes

- 集中生成和解析 `assetRef`，客户端只传递 `assetKind + assetRef`。
- 新增 `GET /api/watchlist/assets`，聚合用户自选关系与基金/股票当前公开数据。
- 新增 `GET /api/assets/search`，统一搜索基金、A 股和美股并返回 `watchlisted`。
- 扩展 `POST /api/watchlist/assets/batch-add` 接受 `assetRef`，同时保留旧字段兼容路径。
- 新增 `GET /api/stocks/detail`，读取 `stock_market` 当前行情和用户自选状态。
- 使用 `null` 和空数组表达缺失数据，不增加模块级状态字段。

## Capabilities

### New Capabilities

- `client-market-asset-apis`：面向客户端的自选列表、市场资产搜索、assetRef 添加和股票当前详情。

## Impact

- 影响 API、Trigger、Case、Domain Repository 和 Infrastructure 查询映射。
- 更新 ADR-004 和根目录客户端契约。
- 不调整数据库表结构，不写持仓，不触发 agent 刷新。
- 需要覆盖用户隔离、搜索边界、assetRef 校验、旧请求兼容和空值语义测试。
