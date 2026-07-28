## Context

`FundSliceRefreshCaseImpl` 当前把 callback 幂等记录、基金目录批量 upsert、诊断日志和任务终态放在同一个事务中，Controller 只能在整个事务完成后返回。全量目录已有 55 个批次，单次 callback 实际约 112 秒，HTTP 连接会先于业务事务结束。

现有 `processing_task` 能表达任务总体状态，`processing_callback` 能以 `(server_task_id, idempotency_key)` 去重并记录处理状态。本次复用这两张表，不保存完整 callback payload；异步进程异常后的补偿方式是人工重新调用现有基金目录调度 HTTP 接口创建新任务。

## Goals / Non-Goals

**Goals:**

- 基金目录 callback 在完成必要校验、幂等接收记录持久化和异步任务提交后快速返回 HTTP 202。
- 保持目录批量写入、单次业务事务原子性和目录切片字段覆盖规则不变。
- 异步异常可靠写入现有 processing 表，允许失败后人工重新调度全新任务。
- Agent 未回调超时和 Server 慢处理使用不同判断条件与观测语义。

**Non-Goals:**

- 不异步化申购状态、阶段收益和重仓 callback。
- 不持久化完整 callback payload，不实现原 payload 重放、自动重试或死信队列。
- 不实现 JVM 启动恢复、租约、心跳或自动终止慢 SQL。
- 不根据 Server 异步处理时长直接写 `callback_failed`。
- 不调整 agent callback timeout、callback payload schema 或基金目录每批 500 条规则。

## Decisions

### 1. Case 层拆分接收事务和业务事务

Controller 仍只负责鉴权、请求转换和响应封装。Case 先校验任务、schema、状态和幂等键，在短事务中插入 `processing_callback(process_status=processing)`，并把非终态任务保持或推进到 `running`。短事务提交后向基金目录专用异步执行器提交业务处理；正常提交时 Controller 返回 HTTP 202 和当前 `running` 任务结果。

重复 idempotency key 不再次提交异步任务，只返回当前任务状态。其他基金切片 callback 保持现有同步路径。

### 2. 使用专用单线程有界执行器

基金目录刷新本身禁止同类型非终态任务并发，因此使用单线程、有限队列和拒绝时抛异常的专用执行器，避免共享线程池的 `CallerRunsPolicy` 把 112 秒处理退回 HTTP 请求线程。执行器拒绝提交时，Case 使用独立事务将 callback 和任务标记为失败，并以 HTTP 202 返回最新 `callback_failed` 状态，停止 agent 重试旧结果并等待人工重新调度。

执行器仅保存本进程中的 command，不承诺 JVM 重启后的 payload 恢复。该限制与首期“失败后重新抓取最新目录”的人工补偿策略一致。

### 3. 异步业务事务保持原子写入

异步线程重新读取任务，在一个 `TransactionExecutor.required()` 事务中执行原有目录校验、每批最多 500 条 upsert、warning 保存、任务终态和 callback `processed` 更新。任一批失败时业务事务整体回滚。

异常捕获位于业务事务之外，并通过 `requiresNew()` 把 callback 更新为 `failed`、任务更新为 `callback_failed`。日志只包含任务 ID、数量、批次、耗时和异常类型，不记录 callback payload 或逐条基金。

### 4. Agent 未回调扫描必须排除已接收 callback

现有超时查询只判断任务非终态和 `processing_task.update_time`，无法区分未回调与 Server 正在处理。查询和条件更新均增加 `NOT EXISTS processing_callback`，只把真正没有接收记录且超过配置窗口的任务写为 `callback_failed`。

`NOT EXISTS` 同时放在候选查询和最终条件更新中，避免 callback 接收事务与扫描任务竞争时仅依赖过期候选结果。接收短事务通过 `SELECT ... FOR UPDATE` 锁定任务行，再把任务更新为 `running` 并插入 callback；扫描与接收竞争时由同一任务行串行化，防止已被超时收口的任务又被覆盖回 `running`。

### 5. Server 慢处理只告警

定时 Job 使用 `processing_callback.create_time` 查询 `fund_catalog_refresh` 且 `process_status=processing` 的记录。超过默认 10 分钟时逐任务打印有界 `WARN` 并保持 callback `processing`、task `running`，不执行状态更新。

慢处理阈值作为现有 callback timeout 配置下的独立参数，扫描仍复用现有 cron。正常异常由异步线程捕获；本次不尝试根据时间推断线程或数据库事务已经终止。

## Risks / Trade-offs

- [HTTP 202 后 JVM 退出会丢失内存 command] -> 首期不持久化 payload；任务会保留 `processing/running` 供人工排查，本 change 不实现启动恢复。
- [慢任务会重复打印 WARN] -> 扫描频率默认每 5 分钟，重复告警用于持续暴露卡住状态；日志保持有界且不包含 payload。
- [异步提交成功不等于目录落库成功] -> 契约明确 202 只表示 Server 已接收并提交本地异步处理，最终结果以 `processing_task` 为准。
- [异步失败后 agent 不再重试原 callback] -> 失败状态落库，运维调用现有 `/api/agent/fund-catalog-refresh/schedule-runs` 创建全新任务并重新抓取最新数据。
- [超时扫描与 callback 接收竞争] -> 候选查询和条件更新均使用 `NOT EXISTS processing_callback`；相关 Repository/SQL 测试固定该约束。

## Migration Plan

1. 更新共享 callback 契约和 processing 状态注释。
2. 发布 Server，并确认基金目录 callback 返回 HTTP 202、任务先进入 `running`。
3. 观察异步完成日志，确认最终任务进入 `succeeded`、`partial_failed` 或 `failed`。
4. 使用测试数据验证 Agent 未回调超过窗口会 `callback_failed`，已有 callback 的慢处理只打印 WARN。
5. 回滚时恢复旧 Server 版本；现有 `processing` 字符串不需要数据库字段迁移，但回滚前应确认没有正在异步处理的目录 callback。

## Open Questions

当前无待确认事项。异步范围、慢任务只告警、Agent 未回调条件、人工重新调度以及暂不做启动恢复均已由用户确认。
