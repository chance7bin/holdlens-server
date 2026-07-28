## Context

`allow-null-market-stock-targets` 已允许基金回调中的空 market 股票注册到 `stock_market_current`，并让这类记录进入股票行情刷新请求。实际运行中，当前 agent 无法处理 market 为空的股票查询，server 继续下发 `market: null` 会制造不可处理目标。

当前链路为：Case 层 `createAndDispatchStockQuotes` 从 `IStockMarketRepository.queryAllQuoteTargets()` 读取候选目标，创建 `stock_quote_refresh` 任务并传给 `IAgentStockQuoteRefreshPort`；Infrastructure Port 只负责把 `StockQuoteTargetEntity` 转成 agent 需要的 snake_case payload。

## Goals / Non-Goals

**Goals:**

- 股票行情刷新请求只下发 `stock_code` 和 `market` 都非空的目标。
- `stockCount` 与实际下发给 agent 的目标数量一致。
- 保留空 market 股票注册和持久化能力，继续作为待补全或诊断候选。
- 当过滤后无可下发目标时，不创建无法执行的股票行情刷新任务。

**Non-Goals:**

- 不在 server 推断、补齐或迁移空 market。
- 不回滚 `stock_market_current.market` 可空和空 market 幂等注册规则。
- 不改变股票行情回调保存规则；如果外部回调仍带空 market，server 现有接收能力不在本变更中收窄。
- 不改 agent 服务能力或外部接口地址。

## Decisions

### 在 Case 层过滤可下发目标

`createAndDispatchStockQuotes` 从仓储拿到候选后，先过滤出 `stock_code` 非空且 `market` 非空的目标，再用过滤后的列表创建任务、写入 `stockCount` 并下发 agent。

选择 Case 层过滤，而不是只在 Infrastructure Port 过滤，原因是“哪些目标构成本次任务”属于用例编排决策。若只在 Port 层过滤，任务参数中的 `stockCount` 可能仍记录数据库候选总数，和真实 agent 请求不一致。

### 空 market 记录继续保留在股票库

仓储查询和数据库结构不需要回退。`queryAllQuoteTargets` 仍可返回空 market 候选，Case 层决定当前 agent 能力下不下发。这样保留基金重仓诊断价值，也为后续 agent 支持空 market 或 server 增加 market 补全能力留出空间。

### Port 层保持轻量防腐转换

Infrastructure Port 继续负责字段命名和外部 HTTP 调用。实现时可在 `toStockQuoteRequestItems` 中防御性跳过空 market 项，但不能只依赖这一层完成过滤；Case 层必须保证传入 Port 的命令已经是本次真实任务目标集合。

### 过滤后空集合沿用现有非法参数失败语义

如果数据库候选存在但全部缺少 market，过滤后应按“股票刷新范围为空”语义拒绝创建任务。这样避免生成一个没有可执行目标的处理任务，也不引入新的任务状态。

## Risks / Trade-offs

- [空 market 股票短期无法自动刷新行情] -> 保留为空 market 候选记录，后续可通过 market 补全或 agent 能力升级另开变更处理。
- [历史 OpenSpec 曾要求发送 `market: null`] -> 本 change 明确修改同一能力的股票行情刷新场景，归档后以新契约为准。
- [只在 Port 层改动造成计数偏差] -> tasks 中要求同步更新 Case 测试，验证 `stockCount` 与 agent 实际目标数量一致。
- [空白字符串 market] -> 实现时按现有 `isBlank` 语义处理，空白 market 也不得下发。
