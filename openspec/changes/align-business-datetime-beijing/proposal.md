## Why

当前业务库大量使用 MySQL `DATETIME` 字段表达创建时间、更新时间、任务时间和行情时间。`DATETIME` 本身不保存时区，但 server 的 JDBC URL 目前配置为 `serverTimezone=UTC`，agent 股票行情回调虽然已经传入 `2026-06-26T16:08:09+08:00` 形式的北京时间，server 仍会通过 `Instant`/`Date` 将其归一化为 UTC 瞬时时间，导致 `stock_market_current.quote_time` 入库为 `2026-06-26 08:08:09`。

HoldLens 的核心用户场景面向本地持仓、基金和股票行情查看，业务库中的 `DATETIME` 应统一按北京时间展示和存储，避免同一张表中出现由 MySQL 当前时区生成的北京时间与 Java/JDBC 写入的 UTC 时间混用。

## What Changes

- 明确业务库所有 MySQL `DATETIME` 字段的语义：按 `Asia/Shanghai` 本地时间存储和展示，不表达 UTC instant，也不保存 offset。
- 调整 server JDBC、应用运行时和 MyBatis/JDBC 写入路径，使 Java 写入和 MySQL `CURRENT_TIMESTAMP`/`NOW()` 生成的时间保持北京时间语义一致。
- 调整 agent 回调中的 `quote_time` 接收方式：server 应使用 offset-aware 解析，先尊重输入中的 `+08:00` 或其他 offset，再转换为 `Asia/Shanghai` 的本地时间写入 `DATETIME`。
- 评估并收敛业务 `DATETIME` 对应 Java 类型，优先在持久化边界使用 `java.time.LocalDateTime`，避免 `java.util.Date`/`Instant` 在 JDBC 层触发隐式时区换算。
- 明确历史数据处理策略：不迁移已经按 UTC 写入的历史 `stock_market_current.quote_time`，通过数据库重刷获得新行情时间。

## Capabilities

### New Capabilities

- `business-datetime-timezone`: server 统一业务库 `DATETIME` 的北京时间语义，并正确保存 agent 回调中的股票行情时间。

### Modified Capabilities

- `fund-stock-refresh-persistence`: 股票行情回调保存 `stock_market_current.quote_time` 时，应按北京时间本地时间入库。
- `processing-schema`: `processing_task`、`processing_log`、`processing_callback` 等处理类表的 `create_time`、`update_time` 也应遵守统一北京时间 `DATETIME` 语义。

## Impact

- 影响 app 配置：`application-dev.yml`、`application-test.yml`、`application-prod.yml` 中 JDBC 时区配置需要与北京时间语义对齐。
- 影响 agent 回调用例编排：`AgentFundRefreshCaseImpl` 中 `quote_time` 解析不应继续使用 `Instant.parse` 直接转 `Date` 入库。
- 影响 Domain/Infrastructure 时间类型：涉及 `quote_time`、`create_time`、`update_time` 等 `DATETIME` 映射的 Entity/PO/DTO 需要评估是否从 `Date` 收敛为 `LocalDateTime`。
- 影响数据库初始化和运维文档：需要说明 MySQL 容器、应用容器、JDBC 连接和业务 `DATETIME` 的统一时区要求。
- 影响测试：需要覆盖 `2026-06-26T16:08:09+08:00` 入库后保持 `2026-06-26 16:08:09` 的行为。

## Non-Goals

- 不把 MySQL `DATETIME` 改为 `TIMESTAMP`。
- 不要求业务库保存 offset 或时区 ID。
- 不改变 agent 回调中的 `generated_at` 跨系统契约；当前不要求将其保存到基金当前详情业务表。
- 不改变 `DATE` 字段语义，例如 `trade_date`、`returns_as_of`、`top_holdings_as_of`。
- 不修改 agent 的行情抓取逻辑；agent 已按契约传出带 `+08:00` offset 的 `quote_time`。

## Resolved Decisions

- 不迁移历史 `stock_market_current.quote_time` 数据；当前环境直接通过数据库重刷获得新的北京时间行情时间。
- API 响应时间暂不统一改成北京时间字符串；本 change 只保证数据库存储和后端读取语义，后续如需统一 API 时间格式再单独开 change。
- `generated_at` 作为跨系统回调字段时保留 offset/instant 输入语义；如后续新增业务落库字段，再按北京时间本地化保存。
