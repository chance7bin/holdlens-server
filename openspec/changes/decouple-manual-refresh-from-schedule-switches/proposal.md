## Why

现有基金目录、基金重仓和基金资产配置的手动 HTTP 入口直接复用带 `enabled` 判断的定时 Job 方法，导致关闭自动调度时也无法手动补刷。定时开关应只控制 cron 自动触发，运维或排查使用的 HTTP 手动入口应保持可用。

需求来源为用户直接提出的 server 调度语义调整，不关联 `docs/requirements/**/prd-*.md`；同时按用户要求，当前 application 配置中的相关定时开关统一保持关闭。

## What Changes

- 将基金目录、基金重仓和基金资产配置的 HTTP 手动触发与定时 Job 开关解耦。
- HTTP 手动触发直接路由到既有刷新 Case，并将触发来源记录为 `manual`。
- cron 入口继续读取各自 `enabled` 开关；关闭时不创建或派发任务。
- 保留批次大小校验、同类型非终态任务跳过、目标选择和派发等既有 Case 规则。
- 将 `application.yml` 与 `application-dev.yml` 中现有基金刷新及回调超时调度的 `enabled` 配置统一设为 `false`。
- 不新增接口、不修改 URL、请求或响应结构，不改变回调接口行为。

成功标准：所有相关定时开关关闭时 cron 不触发刷新或回调超时处理，而三个现有 HTTP 手动刷新接口仍能调用对应 Case。

## Capabilities

### New Capabilities

- `manual-refresh-trigger-independence`: 规定手动 HTTP 刷新入口独立于 cron 定时开关，并约束默认 application 调度开关状态。

### Modified Capabilities

- 无。当前 `openspec/specs/` 下没有已归档的对应能力；本变更通过新能力纠正现有 active change 中的旧约定。

## Impact

- 影响模块：`holdlens-server-trigger`、`holdlens-server-app`。
- 影响接口：现有 `POST /api/agent/fund-catalog-refresh/schedule-runs`、`POST /api/agent/fund-top-holding-refresh/schedule-runs`、`POST /api/agent/fund-asset-allocation-refresh/schedule-runs` 的开关语义发生变化，但 HTTP 契约不变。
- 影响配置：基金目录、申购状态、区间收益、重仓、资产配置及回调超时调度的 `enabled` 默认值与开发环境覆盖值均为 `false`。
- 不涉及数据库、跨项目契约、权限模型或外部依赖变更。
