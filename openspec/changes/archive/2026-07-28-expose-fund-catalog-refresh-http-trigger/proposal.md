## Why

基金基本信息全量刷新目前只能由 cron 调度触发。排查基金目录数据、补刷基础信息或验证 server-agent 链路时，需要通过 HTTP 立即触发现有基金目录刷新流程。

## What Changes

- 新增基金目录全量刷新的手动 HTTP 触发接口。
- 手动触发直接调用现有基金目录刷新 Case，不受 cron 调度开关影响，并保持同类型非终态任务跳过规则不变。
- 接口只返回触发调用结果，不改变基金目录刷新任务、agent 下发和回调契约。

## Capabilities

### New Capabilities

- `fund-catalog-refresh-http-trigger`: 支持通过 HTTP 手动触发现有基金目录全量刷新调度。

### Modified Capabilities

- 无。

## Impact

- 影响模块：`holdlens-server-api`、`holdlens-server-trigger`。
- 影响接口：新增 `POST /api/agent/fund-catalog-refresh/schedule-runs`。
- 影响安全边界：新增可触发全量基金目录刷新任务的 HTTP 入口；本次沿用现有 `/api/agent/**` 接口边界，不新增鉴权机制。
- 影响运行：后端服务改动后需要用户重新运行 application 才能访问新接口。
- 不影响数据库结构、agent 请求和回调数据契约。
