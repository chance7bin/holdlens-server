## Context

当前刷新链路由 `AgentFundRefreshController` 提供 HTTP 入口，Case 层 `AgentFundRefreshCaseImpl` 负责创建 `processing_task`、下发 agent、处理回调和落库。基金详情刷新已经支持传入基金代码列表；股票行情刷新目前从 `stock_market_current` 读取候选目标后创建单个 `stock_quote_refresh` 任务，并且会过滤掉 `stock_code` 或 `market` 为空的目标，避免向 agent 下发不可处理的 `market: null`。

`holdlens-server-trigger/src/main/java/.../trigger/job` 当前没有实际调度任务实现。本次新增定时触发入口时，应继续保持 Trigger 轻量：调度器只决定何时触发、如何分页和何时停止；业务任务创建、任务状态流转、agent 下发、回调落库仍由 Case 层完成。

本变更不直接关联 `docs/requirements/**/prd-*.md`。需求来源为本次用户讨论收敛的低频自动刷新需求。

## Goals / Non-Goals

**Goals:**

- 为基金详情刷新新增可配置、默认关闭的定时分批触发能力。
- 为股票行情刷新新增可配置、默认关闭的定时分批触发能力。
- 股票手动刷新接口支持调用方显式传入 `stockCode + market` 列表。
- 每个批次创建独立 `processing_task`，复用现有回调和幂等处理模型。
- 定时任务发现同类型非终态任务时跳过本轮，避免重复下发。
- 定时任务在批次创建结果异常时停止本轮，避免 agent 或网络异常时继续放大请求。

**Non-Goals:**

- 不新增调度批次表、父任务、执行尝试表或跨轮游标持久化。
- 不按最近更新时间、行情交易日或业务过期规则过滤候选目标。
- 不新增 `allowNetwork` 配置开关，继续沿用当前刷新任务下发时 `allowNetwork = true`。
- 不改变基金手动刷新接口的请求契约。
- 不让 HTTP 入口提供全表刷新能力；全表分页刷新由定时任务负责。
- 不读取或依赖 agent 生成的 Markdown 文件。

## Decisions

### 1. 每批创建独立 processing task

定时任务每读取一页候选目标，就调用 Case 层创建一个独立的刷新任务。基金批次复用 `createAndDispatch(FundRefreshCreateCommand)`；股票批次新增带命令参数的股票任务创建能力，由命令携带去重后的 `stockCode + market` 列表。

备选方案是创建一个大任务并在内部多次下发 agent。该方案需要新增子批次状态、回调聚合、失败补偿和幂等归属设计，超出当前 `processing_task` 主表模型，也与 ADR 中“执行尝试另行建模”的方向不一致。

### 2. 定时任务作为 Trigger 层入口

新增调度类放在 trigger job 包中，负责读取配置、并发前置检查、分页拉取候选、调用 Case 创建批次任务和记录安全日志。调度类不直接调用 agent port，不直接写 `processing_task`，不处理回调落库。

### 3. 使用 `id > lastId ORDER BY id LIMIT batchSize` 扫描全表

`fund_detail_item` 和 `stock_market_current` 都有自增 `id`，适合作为 keyset 分页游标。每轮调度从 `lastId = 0` 开始，分页扫描到无更多候选为止。本次不设置 `max-batches-per-run`，因此不需要跨轮游标持久化。

备选方案 `OFFSET` 分页在表更新时容易跳过或重复，且随数据量增长性能退化。备选方案“每轮限批 + 持久化游标”更适合大规模数据，但当前更新频率不高，本次不引入额外状态表。

### 4. 股票手动刷新必须传 `stockCode + market`

股票手动接口新增请求体，要求 `stocks` 非空，且每项 `stockCode`、`market` 都非空。去重键为 `stockCode + market`。手动传入的股票列表不强制要求已存在于 `stock_market_current`，回调成功后仍通过现有 upsert 能力保存当前行情。

不支持只传 `stockCode` 后由 server 反查市场，因为当前唯一语义和 agent payload 都依赖市场字段；遇到空 market 或同代码多市场时，自动推断会制造歧义。

### 5. 同类型非终态任务存在时跳过本轮

基金定时任务启动前检查是否存在 `fund_detail_refresh` 的非终态任务；股票定时任务启动前检查是否存在 `stock_quote_refresh` 的非终态任务。若存在，则记录安全日志并跳过本轮。

该策略比允许重叠运行更保守。当前落库多为 upsert，重复刷新不一定破坏最终数据，但会放大 agent 压力，使任务诊断和回调顺序变差。

### 6. 批次失败即停止本轮

如果 Case 抛异常，或返回任务状态不是 `created`、`running`、`dispatched`，调度器停止本轮。已经创建的前置批次不回滚，因为这些任务可能已经下发给 agent，并会通过现有回调链路完成或失败。

### 7. 配置默认关闭

新增配置建议为：

```yaml
holdlens:
  agent:
    fund-refresh-schedule:
      enabled: false
      cron: "0 20 2 * * ?"
      batch-size: 20
    stock-refresh-schedule:
      enabled: false
      cron: "0 40 2 * * ?"
      batch-size: 50
```

默认关闭可以避免本地或新环境启动后立即产生外部 agent 调用。需要运行时由环境配置显式开启。

## Risks / Trade-offs

- [全表扫描耗时随数据量增长] -> 当前更新频率不高，先通过 batch size 控制单批 payload；未来数据量增大时再设计持久化游标或过期过滤。
- [定时任务跳过可能导致刷新延迟] -> 跳过策略保护 agent 和任务模型一致性；下一轮 cron 会再次尝试。
- [批次失败停止导致后续目标本轮未刷新] -> 下次全表扫描可补偿；停止能避免外部异常时继续放大请求。
- [股票手动接口契约变更] -> 新请求体使目标语义明确；基金手动接口保持兼容。
- [默认关闭导致部署后不自动刷新] -> 这是有意的运行安全选择，开启需要显式配置。

## Migration Plan

本变更不需要数据库表结构迁移。发布后默认不启用定时任务；开启前应配置 cron 和 batch size，并确认 agent 服务可用。若需要回滚，可关闭 enabled 配置并停止新调度触发，已创建的 processing task 仍按现有回调机制自然完成或失败。

## Open Questions

当前无待确认事项。后续如数据量增长，需要单独评估是否引入调度游标状态、过期过滤或调度批次表。
