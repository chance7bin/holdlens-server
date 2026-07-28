## Context

当前 `AgentRefreshScheduleController` 通过调用 `AgentRefreshScheduleJob` 的 `@Scheduled` 方法实现三个 HTTP 手动入口。Job 方法先读取 `enabled`，因此 HTTP 与 cron 被错误绑定：开发环境关闭自动调度后，手动补刷也会静默跳过。

现有调用链遵循 `Trigger -> Case -> Domain <- Infrastructure`。`IFundSliceRefreshCase` 已承载非终态任务跳过、目标选择、批次切分、任务创建和 agent 派发；`scheduleTopHoldings` 与 `scheduleAssetAllocations` 也已校验 `batchSize`。本次无需改变业务规则、事务边界、数据库或外部 agent 契约。

## Goals / Non-Goals

**Goals:**

- 让调度 `enabled` 只控制 cron 自动入口。
- 让三个现有 HTTP 手动入口在调度关闭时仍能调用对应刷新 Case。
- 区分任务参数中的 `manual` 与 `schedule` 触发来源。
- 将通用配置和 dev 配置中的六个现有调度开关保持为关闭状态。
- 用单元测试分别验证 cron 开关和 HTTP 手动调用语义。

**Non-Goals:**

- 不新增申购状态、区间收益或回调超时的手动 HTTP 接口。
- 不改变同类型非终态任务跳过、批次大小校验、目标筛选、回调或任务状态机。
- 不修改接口 URL、请求体、响应结构、权限或审计策略。
- 不启动或重启 application，不进行真实 agent 派发验证。

## Decisions

### 1. HTTP Controller 直接委托现有 Case

`AgentRefreshScheduleController` 改为依赖 `IFundSliceRefreshCase`，分别调用 `scheduleCatalog("manual")`、`scheduleTopHoldings("manual", batchSize)` 和 `scheduleAssetAllocations("manual", batchSize)`。重仓与资产配置的 `batch-size` 继续读取现有配置项，并由 Case 保持最终合法性校验。

该选择符合轻量 Trigger 只路由到 Case 的边界，也避免 HTTP 入口借用带 cron 语义的 Job。备选方案是在 Job 中新增绕过开关的公开方法；这会让 Job 同时承担 cron 与 HTTP 入口语义，方法命名和误用风险更高。另一个备选方案是新增共享触发服务，但当前编排已经在 Case 中存在，新增抽象没有收益。

### 2. Job 保持现有定时开关职责

`AgentRefreshScheduleJob` 的五个刷新方法和一个回调超时方法继续在执行 Case 前判断各自 `enabled`。本次不改变 cron 表达式、zone、batch-size 或日志策略，避免扩大调度行为变更范围。

### 3. 手动触发来源记录为 `manual`

HTTP 入口传递 `manual`，cron 入口继续传递 `schedule`。这只修正既有任务参数中的来源语义，不改变幂等、任务状态或派发协议。

### 4. 配置默认关闭

`application.yml` 已将六个开关设为 `false`；`application-dev.yml` 同步改为 `false`，防止 dev profile 覆盖后自动启用。测试同时加载两份 YAML 并断言全部开关关闭，避免后续只修改一处造成回归。

### 5. 兼容现有 HTTP 契约

三个接口仍返回 `Response<Void>`。手动调用若因非终态任务或空目标被 Case 跳过，继续沿用既有成功响应和日志语义；本次只解耦开关，不引入新的响应状态。

## Risks / Trade-offs

- [关闭 dev 自动调度后不再自动刷新或清理超时回调] -> 这是用户明确要求；需要自动执行时再显式开启对应开关并重启应用。
- [HTTP 手动入口可在调度关闭时产生任务] -> 这是目标行为；保留 Case 的非终态任务保护和批次校验，权限增强不在本次范围。
- [Controller 读取 batch-size 配置] -> 配置只用于入口参数，合法性仍由 Case 统一判断，不在 Trigger 复制业务规则。
- [旧 active change 描述了手动入口受开关控制] -> 以本 change 的能力规格明确覆盖当前行为，并同步修正直接涉及现有三个接口的旧约定。

## Migration Plan

1. 先部署配置和代码变更；应用重启后六个定时入口均保持关闭。
2. 通过单元测试验证关闭状态下 Job 不调用 Case、HTTP Controller 仍调用 Case。
3. 如需回滚代码，可恢复 Controller 对 Job 的依赖；如需恢复自动调度，仅按环境显式开启目标开关，不需要数据库迁移。

## Open Questions

当前无待确认事项，可以按已获授权的方案实施。
