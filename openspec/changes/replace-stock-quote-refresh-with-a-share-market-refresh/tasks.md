## 1. 契约与设计

- [x] 1.1 更新根目录 `contracts/`，新增 A 股全量刷新任务创建、agent 下发和回调契约。
- [x] 1.2 将旧 `stock_quote_refresh` 契约在文档中标记为废弃，不再作为 active server 集成契约。
- [x] 1.3 确认根目录 ADR 已记录从旧股票列表刷新切换为 A 股全量刷新。

## 2. 数据库与领域模型

- [x] 2.1 将初始化 SQL 中 `stock_market_current` 重构为 `stock_market`。
- [x] 2.2 删除 `trade_date`、`daily_return`、`quote_time`、`market_key` 字段和旧唯一键。
- [x] 2.3 新增 `market` 非空业务市场字段、`exchange_code`、`provider_market_code`、`status`、`refreshed_at` 和 AKShare 有效行情字段。
- [x] 2.4 将唯一约束调整为 `stock_code + market`。
- [x] 2.5 更新 Domain 实体、Repository 接口、PO、DAO 和 Mapper XML，SQL 继续放在 MyBatis XML。

## 3. A 股全量刷新任务

- [x] 3.1 新增 `a_share_market_refresh` task type、任务创建命令和 Case 编排。
- [x] 3.2 新增 `POST /api/agent/a-share-market-refresh/tasks` 手动创建入口。
- [x] 3.3 新增 agent port 下发 `a-share-market-refresh-task/v1` 请求。
- [x] 3.4 新增 `/internal/agent/a-share-market-refresh/callback` 回调 DTO、鉴权、幂等和状态处理。
- [x] 3.5 回调成功或部分成功时分批 upsert `stock_market`，并保存安全 warning。

## 4. 废弃旧 server 股票刷新契约

- [x] 4.1 删除或停用旧 `POST /api/agent/stock-quote-refresh/tasks` active 行为。
- [x] 4.2 删除或停用旧 `/internal/agent/stock-quote-refresh/callback` active 行为。
- [x] 4.3 删除或停用按 `stock_market_current` 分页创建 `stock_quote_refresh` 的调度逻辑。
- [x] 4.4 清理旧 `StockQuoteEntity`、`StockQuoteTargetEntity` 或将其重命名为新模型，避免继续使用 `dailyReturn`/`quoteTime` 语言。

## 5. 读侧与 API 字段调整

- [x] 5.1 将基金重仓组合查询中的旧 `dailyReturn` 输出改为 `changePercent`。
- [x] 5.2 将旧 `quoteTime` 输出改为 `refreshedAt`。
- [x] 5.3 更新相关 API DTO、Controller 映射和测试夹具，不保留旧字段兼容。

## 6. 测试与验证

- [x] 6.1 补充 Case 测试：任务创建、并发非终态任务拦截、成功回调、部分失败回调、重复回调幂等。
- [x] 6.2 补充 Repository/Mapper 测试：`stock_code + market` upsert、数值字段解析、NULL 字段处理、批量写入。
- [x] 6.3 补充 Trigger/API 测试：新接口映射、旧接口不再作为 active 股票刷新链路。
- [x] 6.4 补充读侧测试：返回 `changePercent`/`refreshedAt`，不返回旧 `dailyReturn`/`quoteTime`。
- [x] 6.5 运行受影响 Maven 模块测试。
- [x] 6.6 运行 `openspec validate --strict replace-stock-quote-refresh-with-a-share-market-refresh`。
- [x] 6.7 做安全检查，确认任务参数、warning、error summary 和日志不包含账户资产明细、凭据或敏感信息。
