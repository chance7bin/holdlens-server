## Context

本 change 以 `docs/decisions/adr-003-schema-modify.md` 为输入。当前实现已经落地 `agent-async-fund-refresh`，server 可创建基金刷新任务、下发 agent、接收回调、保存基金详情快照和 warning，并提供任务查询。

现有代码和 DDL 中的主要待收敛点：

- `processing_task` 同时保存 `status`、`callback_diagnostic_status`、`fund_code_count`、`source_type`、`source_ref_id`、`agent_task_ref`。
- `AgentFundRefreshPort` 会从 agent accepted response 中提取 `agent_task_id` 或 `task_id` 并写入 `agentTaskRef`。
- `agent_warning` 实际承载 `info`、`warning`、`error` 等处理诊断日志，但表名和字段仍以 warning、快照、基金代码、来源章节/行号为中心。
- `fund_detail_snapshot.source_type` 当前固定表达 agent 来源，不参与查询、状态流转、幂等或展示。

## Goals / Non-Goals

**Goals:**

- 按 ADR-003 收敛 server 持久化结构和对应代码模型。
- 保持 server 是长期业务事实源和写入决策方，agent 只输出结构化结果。
- 保持任务主状态只由 `processing_task.status` 表达，回调失败诊断不再形成第二状态字段。
- 让任务特有的低频上下文进入 `task_params_json` 安全摘要，而不是新增或保留任务类型专用列。
- 让处理诊断记录统一进入 `processing_log`，并用 `module` + `event` 表达可聚合分类。
- 定义兼容、迁移、回滚、安全和测试边界。

**Non-Goals:**

- 不新增处理任务类型、调度系统、执行尝试表或处理日志检索 API。
- 不重新设计账户、资产、持仓、权限或审计模型。
- 不让 server 直接抓取公开基金数据。
- 不把 agent warning 原始 payload 作为长期事实源完整归档。
- 不设计按任务参数 JSON 高频查询的索引能力；如未来需要按参数筛选任务，另开 OpenSpec change。

## Decisions

### `processing_task` 保持通用任务主表定位

`processing_task` 只保留任务身份、类型、主状态、错误摘要、时间和安全任务参数摘要。基金刷新专用的 `fund_code_count` 替换为 `task_params_json`，当前基金刷新可写入：

```json
{
  "fundCodeCount": 2,
  "trigger": "system"
}
```

`task_params_json` 是任务参数摘要，不是原始请求归档。它不得保存真实账户名称、资产金额、完整持仓、token、cookie、回调鉴权、外部 API key 或原始请求全文。

### 回调失败由任务主状态表达

删除 `callback_diagnostic_status`。agent 回调失败的任务终态继续使用 `processing_task.status = callback_failed` 表达；可展示或排查的错误摘要使用 `error_summary`；单次回调处理、幂等键和处理状态继续由 `processing_callback` 表达。

### server 与 agent 统一使用 `server_task_id`

删除 `source_type`、`source_ref_id` 和 `agent_task_ref`。`server_task_id` 是 server 与 agent 之间的全链路唯一任务标识，用于任务查询、agent 回调关联、回调幂等、快照来源关联和处理日志来源关联。

如果需要记录手动触发、调度批次或导入批次等低频排查上下文，写入 `task_params_json` 的安全摘要。未来如需要 agent 内部 job id、重试实例或执行尝试，需要另行设计 `processing_task_attempt`、`dispatch_attempt` 或 `processing_log` 扩展，而不是恢复 `agent_task_ref`。

### `processing_log` 只承载诊断与可观测信息

将 `agent_warning` 收敛为 `processing_log`。目标字段为：

- `id`
- `source_ref_id`
- `module`
- `event`
- `message`
- `severity`
- `create_time`
- `update_time`

`source_ref_id` 当前对应 `processing_task.server_task_id`。删除 `snapshot_id`、`fund_code`、`source_type`、`warning_type`、`code`、`source_section`、`source_row_number`。需要快照上下文时，通过 `fund_detail_snapshot.source_ref_id = processing_log.source_ref_id` 查询；基金定位信息可写入安全 message，机器聚合依赖 `module` 和 `event`。

### `module` + `event` 替代单个 warning code

server 保存 agent 回调 warning 时，应把来源分类拆成 `module` 和 `event`。例如 agent 回调中的 provider 失败可映射为：

```json
{
  "module": "fund_refresh",
  "event": "provider_fund_failed",
  "severity": "error"
}
```

如果 agent 暂时仍发送旧 `code` 字段，server 实现阶段可在内部兼容解析并映射为 `module = fund_refresh`、`event = <code>`；兼容窗口和移除时机需要在实现任务中确认。

### `fund_detail_snapshot` 聚焦基金公开快照事实

删除 `fund_detail_snapshot.source_type`。快照来源上下文通过 `source_ref_id` 关联 `processing_task.server_task_id` 获得。未来如果引入非 agent 来源的基金详情快照，需要另开 change 重新设计来源模型，而不是保留当前恒定字段。

## Compatibility / Migration

- 当前阶段不做历史库在线迁移兼容，直接调整数据库初始化 SQL。用户会重新执行初始化脚本和导入脚本。
- `processing_task.fund_code_count` 不再作为结构化列存在，也不在任务查询 API 中兼容输出 `fundCodeCount`。
- `agent_warning` 不做旧表数据迁移，初始化 SQL 直接改为创建 `processing_log`。
- `processing_log` 只保留目标字段；`snapshot_id`、`fund_code`、`source_type`、`warning_type`、`code`、`source_section`、`source_row_number` 不进入目标表。
- 旧响应字段 `sourceType`、`sourceRefId`、`agentTaskRef`、`callbackDiagnosticStatus` 不保留兼容输出。

## Security / Privacy

- `task_params_json`、`processing_log.message`、`error_summary` 和应用日志不得保存真实个人资产明细、账户标识、完整持仓、token、cookie、API key、回调鉴权或原始请求全文。
- API 和日志只能暴露任务 ID、任务类型、基金代码数量、状态、模块、事件、错误摘要等安全排查信息。
- 本 change 不改变用户隔离或权限模型；若实现触及账户资产查询，需要继续保持 `portfolio` 事实源和用户隔离。

## Rollout / Rollback

- 当前阶段通过重新执行初始化脚本和导入脚本完成 schema 收敛，不设计在线迁移脚本。
- 回滚方式是回退初始化 SQL 与代码后重新初始化环境。
- 因不兼容输出旧字段，实现和验收时需要同步更新调用方与测试断言。

## Risks / Trade-offs

- [旧调用方依赖旧字段] -> 本 change 明确不兼容输出旧字段，实现阶段需要同步更新前端、脚本或测试调用方。
- [JSON 摘要被误用为事实源] -> 在模型、写入逻辑和测试中限制 `task_params_json` 只保存安全摘要。
- [日志表重命名影响查询] -> 迁移脚本、DAO、Mapper 和测试必须同时更新。
- [agent 仍发送旧 warning code] -> server 可短期兼容旧字段映射，agent change 负责输出目标字段。

## Open Questions

当前无。

## Resolved Decisions

- `fundCodeCount` 不兼容输出；任务查询 API 不再返回该字段，基金代码数量仅作为 `task_params_json` 内部安全摘要保存。
- 旧任务字段 `sourceType`、`sourceRefId`、`agentTaskRef`、`callbackDiagnosticStatus` 不保留兼容输出。
- `agent_warning` 到 `processing_log` 不做在线迁移；直接修改初始化 SQL，用户会重新执行初始化脚本和导入脚本。
- agent 不输出旧 `code` / `fund_code` 诊断字段；server 不设计旧字段兼容窗口。
