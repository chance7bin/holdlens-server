## Context

`AgentRefreshScheduleJob.runFundCatalogRefreshSchedule()` 已负责读取 `fund-catalog-refresh-schedule.enabled`，并在开关开启时调用 `IFundSliceRefreshCase.scheduleCatalog("schedule")`。Case 层会阻止同类型非终态任务重复创建，并复用现有 agent 下发及回调保存链路。

现有 `AgentRefreshScheduleController` 已提供基金重仓刷新调度的手动 HTTP 入口，API 层通过 `IAgentRefreshScheduleService` 描述接口契约。

## Goals / Non-Goals

**Goals:**

- 提供基金目录全量刷新的手动 HTTP 入口。
- 复用现有 Job 方法，保持 HTTP 与 cron 触发行为一致。
- 保持 Controller 只负责接收请求、记录日志、调用调度入口和封装响应。

**Non-Goals:**

- 不绕过 `fund-catalog-refresh-schedule.enabled` 开关。
- 不返回任务 ID、刷新数量或进度。
- 不改变基金目录刷新任务、agent 下发或回调契约。
- 不新增认证鉴权机制。

## Decisions

- 在 `IAgentRefreshScheduleService` 和 `AgentRefreshScheduleController` 中新增 `runFundCatalogRefreshSchedule()`。
- 使用 `POST /api/agent/fund-catalog-refresh/schedule-runs`，与现有基金切片任务命名保持一致。
- Controller 直接调用同名 Job 方法并返回 `Response.ok(null)`。
- 手动调用继续受调度开关控制；开关关闭时接口仍返回成功，但不创建任务。

## Risks / Trade-offs

- HTTP 成功仅表示调度入口执行完成，不表示 agent 已完成刷新；保持现有手动调度接口语义，避免引入新的运行记录模型。
- 接口可触发全量刷新；当前通过调度开关和同类型非终态任务跳过规则限制重复执行，鉴权增强不在本次范围内。
