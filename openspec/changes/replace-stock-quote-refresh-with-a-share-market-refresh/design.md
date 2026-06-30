## Context

当前 server 股票刷新链路由 `AgentFundRefreshController` 和 `AgentFundRefreshCaseImpl` 承接：server 从 `stock_market_current` 读取 `stock_code + market` 目标，创建 `stock_quote_refresh` 任务，下发 agent `/tasks/stock-quote-refresh`，再通过 `/internal/agent/stock-quote-refresh/callback` 保存 `trade_date`、`daily_return` 和 `quote_time`。

本次设计将该链路替换为 A 股全量行情刷新：server 不再负责维护待刷新股票列表并下发给 agent，而是创建一个 `a_share_market_refresh` 任务，由 agent 自行调用 AKShare `stock_zh_a_spot_em()` 获取沪深京 A 股全量当前行情并一次性回调 server。server 仍是长期事实源，负责处理任务状态、幂等、字段校验和 `stock_market` 落库。

本变更不直接关联 `docs/requirements/**/prd-*.md`。需求来源为本次用户讨论收敛的 A 股全量行情同步和股票表重构方案。

## Goals / Non-Goals

**Goals:**

- 用 `a_share_market_refresh` 替换 server 侧旧 `stock_quote_refresh` 跨项目任务契约。
- 将 `stock_market_current` 重构为 `stock_market`，承载 A 股全量当前行情字段。
- 将 `market` 收敛为非空业务市场枚举，A 股统一为 `A_SHARE`。
- 使用 `stock_code + market` 作为 `stock_market` 唯一业务键，删除 `market_key` 兼容列。
- 保存 AKShare `stock_zh_a_spot_em()` 的有效数值字段，并使用 `refreshed_at` 表达本批刷新时间。
- 一次性接收 agent 全量回调，server 内部按批次 upsert。
- 对外读侧字段同步从 `dailyReturn` 改为 `changePercent`，不保留兼容字段。

**Non-Goals:**

- 不在 server 中调用 AKShare 或其他公开行情源。
- 不保留旧 `stock_quote_refresh` server 任务、回调和调度能力。
- 不保存历史行情、分钟行情或逐笔行情。
- 不实现分片回调、结果文件下载或跨任务合并。
- 不在第一版对历史存在但本次未返回的股票自动标记缺失或删除。

## Decisions

### 1. 替换而不是兼容旧 `stock_quote_refresh`

server 侧删除或停用旧股票刷新任务创建接口、旧回调接口、旧调度扫描和旧契约文档引用。新增 `a_share_market_refresh` 作为唯一 server <-> agent 股票市场数据刷新任务。

备选方案是同时保留旧 `stock_quote_refresh` 和新增全量任务。该方案会让同一张股票表存在两套写入入口、两套状态语义和两套字段命名，容易制造刷新顺序、幂等和测试复杂度。本次选择破坏性替换。

### 2. `stock_market` 表表达当前全市场行情

新表名使用 `stock_market`，表达公开股票市场当前行情事实，而不是刷新目标集合。字段方向如下：

- 身份字段：`stock_code`、`market`、`exchange_code`、`provider_market_code`、`stock_name`
- 行情数值：`latest_price`、`change_percent`、`change_amount`、`volume`、`turnover_amount`、`amplitude`、`high_price`、`low_price`、`open_price`、`previous_close`、`volume_ratio`、`turnover_rate`、`pe_dynamic`、`pb_ratio`、`total_market_value`、`circulating_market_value`、`speed`、`five_minute_change`、`sixty_day_change_percent`、`year_to_date_change_percent`
- 状态与时间：`status`、`refreshed_at`、`create_time`、`update_time`

`trade_date` 删除，因为 `stock_zh_a_spot_em()` 返回结果中没有明确交易日字段。`quote_time` 改名为 `refreshed_at`，语义为 agent 生成或回调本批数据的时间。

### 3. 契约用 string/null，server 入库用数值类型

agent 回调中的价格、百分比、成交量、金额和市值字段使用 string/null，避免 JSON 浮点精度、NaN 和大数字序列化问题。server 在 Trigger/Case 映射边界将可解析值转换为 `BigDecimal` 或 `Long`，不可解析或缺失值入库为 `NULL` 并记录安全 warning。

推荐数据库类型：

- 价格和涨跌额：`DECIMAL(20,4)`
- 百分比、比率和涨速：`DECIMAL(12,4)`
- 成交量：`BIGINT`
- 成交额和市值：`DECIMAL(24,4)`

所有百分比字段均按百分点保存，例如 `1.25` 表示 `1.25%`，不是 `0.0125`。

### 4. `market`、`exchange_code`、`provider_market_code` 分离

`market` 是 HoldLens 业务市场枚举，A 股统一为 `A_SHARE`，并参与唯一约束。`exchange_code` 表达交易所归属，例如 `SH`、`SZ`、`BJ`，第一版可为空。`provider_market_code` 表达第三方数据源市场编码，例如东方财富 secid 市场编号，第一版可为空。

唯一约束为：

```sql
UNIQUE KEY uk_stock_market_code_market (stock_code, market)
```

### 5. 一次性全量回调，server 分批写库

第一版允许 agent 在一个 callback payload 中回传全量 A 股行情列表。server 接收后校验任务、幂等键、schema version、状态和市场字段，再按固定批次写入，例如每 500 条一次 DAO upsert。

备选方案是分片回调或 agent 输出文件供 server 拉取。当前 A 股全量记录数量在可控范围内，先采用简单直接的一次性回调；未来 payload 过大时另开 change 引入分片协议。

### 6. `status` 先入表但不做反向缺失标记

`status` 第一版支持 `active` 和 `missing_from_refresh`。本次回调中出现的股票写为 `active`。历史存在但本次未返回的股票暂不更新，避免 AKShare 接口临时缺失、分页失败或停牌等情况导致误标记。

### 7. 读侧字段一起破坏性改名

组合查询和对外 DTO 不再返回旧 `dailyReturn`，统一改为 `changePercent`。如需要展示刷新时间，读取 `refreshedAt`，不再暴露 `quoteTime`。

## Risks / Trade-offs

- [破坏性字段和接口变更] -> 通过 OpenSpec、contracts 和 ADR 明确不兼容范围；实现时同步更新 tests、DTO 和前端调用方。
- [一次性 payload 过大] -> 第一版接受全量回调，server 内部分批写库；后续如遇限制再引入分片回调。
- [AKShare 字段缺失或 NaN] -> 契约允许 string/null，server 解析失败写 NULL 并记录安全 warning。
- [市场/交易所信息第一版不完整] -> `market=A_SHARE` 保证业务市场稳定，`exchange_code` 和 `provider_market_code` 可后续补齐。
- [旧调度残留误触发] -> 实现任务中必须删除或停用旧 `stock_quote_refresh` 调度与手动入口，并覆盖回归测试。

## Migration Plan

当前项目仍以初始化 SQL 为主，本次按破坏性重构处理，不做在线数据迁移。实现时调整 `docs/dev-ops/mysql/sql/holdlens.sql`，同步 Java PO、DAO、Mapper XML、Repository、Case、Trigger DTO 和测试。

发布或本地验证时需要重新初始化或手工迁移股票表。回滚策略是回退本 change 对应代码和 SQL，旧 `stock_quote_refresh` 契约不在本变更中保持双写兼容。

## Open Questions

当前无待确认事项。后续如果需要港股、美股或个股补刷，应新增独立市场刷新能力或重新设计单股刷新契约，不复用本次 A 股全量任务的字段假设。
