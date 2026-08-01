## Why

server 当前共有十个 cron 定时入口，但只有基金目录、基金重仓和基金资产配置三个任务提供 HTTP 手动触发能力。其余任务在排查、补刷和验证链路时仍需等待 cron 或临时调整配置，操作方式不一致。

需求来源为用户直接提出“server 的几个定时任务统一开放 HTTP 接口调用”，不依赖 `docs/requirements/**/prd-*.md`，无需新建或修改 PRD。

## What Changes

- 为基金申购状态刷新、基金切片回调超时处理、A 股全市场刷新、美股全市场刷新、活跃基金详情刷新、活跃 A 股详情刷新和活跃美股详情刷新补齐 `POST .../schedule-runs` 手动入口。
- 所有手动入口直接委托 cron 所调用的既有 Case，用 HTTP 入口语义绕过对应的 `enabled` 开关。
- 手动入口复用既有目标选择、任务去重、交易日判断、批次处理、超时关闭和慢回调告警规则。
- 已存在的三个手动入口 URL、响应和行为保持不变。
- 不新增请求参数，不修改 cron、配置默认值、数据库、agent 契约或任务状态机。

成功标准：十个 cron 定时任务均有对应的 POST 手动入口；新增入口在 cron 开关关闭时仍调用对应 Case，并返回统一的成功空响应。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `manual-refresh-trigger-independence`: 将手动 HTTP 调度覆盖范围扩展到 server 当前全部 cron 定时任务。

## Impact

- 影响模块：`holdlens-server-api`、`holdlens-server-trigger`。
- 新增七个 POST API；无请求体，响应继续使用 `Response<Void>`。
- 不涉及数据库迁移、跨项目契约、外部依赖或配置变更。
- HTTP 入口沿用现有 Controller 的访问边界；本次不新增鉴权或审计机制。
