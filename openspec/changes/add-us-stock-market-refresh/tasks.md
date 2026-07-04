## 1. 契约与设计

- [x] 1.1 确认根目录 ADR `docs/decisions/adr-003-us-stock-market-refresh-boundary.md` 与本 change 范围一致。
- [x] 1.2 与 agent change `add-us-stock-market-refresh-task` 对齐 schema version、状态枚举、字段命名、错误语义和幂等规则。
- [x] 1.3 更新根目录 `contracts/` 美股任务创建、agent 下发和回调契约。

## 2. API 与 Case 编排

- [x] 2.1 新增美股任务创建 Request/Service 方法/Controller 入口。
- [x] 2.2 新增美股回调 Request/Command/Controller 入口。
- [x] 2.3 在 Case 层创建 `us_stock_market_refresh` 任务，拒绝同类型非终态并下发 agent。
- [x] 2.4 在 Case 层处理美股回调幂等、状态流转、warning 保存和重复回调。

## 3. Domain 与 Infrastructure

- [x] 3.1 新增或扩展 Agent Port，向 `/tasks/us-stock-market-refresh` 下发任务。
- [x] 3.2 扩展 `StockMarketEntity` 支持 `MARKET_US_STOCK`、`peRatio`、`listingDate`。
- [x] 3.3 扩展 `stock_market` DDL、PO、Mapper XML 和 Repository 映射，支持 `US_STOCK` 和美股特有字段。
- [x] 3.4 配置 `holdlens.agent.us-stock-market-refresh-url` 和 `holdlens.agent.us-stock-market-callback-url`。

## 4. 测试与验证

- [x] 4.1 覆盖创建美股任务并下发 agent 的 Case 测试。
- [x] 4.2 覆盖并发非终态任务拒绝。
- [x] 4.3 覆盖成功、部分失败、失败、重复回调和 unsupported schema。
- [x] 4.4 覆盖 `pe_ratio`、`listing_date`、`provider_market_code`、数值解析失败 warning。
- [x] 4.5 运行相关 Maven 模块测试。
- [x] 4.6 运行 `openspec validate --strict add-us-stock-market-refresh`。

## 5. 安全检查

- [x] 5.1 确认创建请求、`task_params_json`、`processing_log.message`、`error_summary` 不包含真实账户、持仓、资产金额、token、cookie、API key 或其他凭据。
- [x] 5.2 确认 server 不直接访问外部行情接口。
