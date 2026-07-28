## Why

当前基金详情和股票行情的全量刷新调度只能等待 cron 触发。排查数据、补刷行情或验证 agent 链路时，需要一个可控的手动入口来立即执行现有调度逻辑，避免临时改 cron 或复制批量扫描逻辑。

## What Changes

- 新增基金详情刷新调度的手动 HTTP 触发接口。
- 新增股票行情刷新调度的手动 HTTP 触发接口。
- 手动触发复用现有 `AgentRefreshScheduleJob` 调度方法，保持 enabled 开关、batch-size、非终态任务跳过、批次异常停止等行为一致。
- 手动触发接口只返回触发受理结果，不新增调度运行记录表，不改变现有 agent 任务和回调契约。

## Capabilities

### New Capabilities

- `manual-agent-refresh-schedule-runs`: 支持通过 HTTP 手动触发现有基金详情和股票行情刷新调度运行。

### Modified Capabilities

- 无。当前 `openspec/specs` 下暂无已归档能力；本变更在 change 内新增能力规格，并复用 active change `schedule-agent-refresh-batches` 已定义的调度行为。

## Impact

- 影响模块：`holdlens-server-api`、`holdlens-server-trigger`。
- 影响接口：新增 `POST /api/agent/fund-detail-refresh/schedule-runs` 和 `POST /api/agent/stock-quote-refresh/schedule-runs`。
- 影响安全边界：新增可触发批量 agent 任务的 HTTP 入口；本次按现有 `/api/agent/**` 接口风格实现，不额外引入认证鉴权机制。
- 影响运行：后端服务改动后需要用户重新运行 application 才能访问新接口。
- 需求来源：用户询问定时任务时间后，明确要求“写一个能手动触发这两个定时任务的接口”；不直接关联 `docs/requirements/**/prd-*.md`，无需更新 PRD 状态。
