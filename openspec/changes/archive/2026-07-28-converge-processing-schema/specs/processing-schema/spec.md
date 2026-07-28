## ADDED Requirements

### Requirement: 收敛处理任务主表字段
server SHALL use `processing_task` as a generic processing task table focused on task identity, task type, main status, safe parameter summary, error summary, and timestamps.

#### Scenario: 创建基金刷新任务时保存安全参数摘要
- **WHEN** server 创建 `fund_detail_refresh` 处理任务
- **THEN** server SHALL 保存 `server_task_id`、`task_type`、`status`、`task_params_json`、`create_time` 和 `update_time`
- **AND** `task_params_json` SHALL include safe summary fields such as `fundCodeCount`
- **AND** `task_params_json` SHALL NOT include account names, holding amounts, full portfolio composition, tokens, callback credentials, API keys, or full raw requests

#### Scenario: 不再保存基金刷新专用任务数量列
- **WHEN** server 持久化处理任务
- **THEN** `processing_task` SHALL NOT persist `fund_code_count`
- **AND** task query API SHALL NOT expose `fundCodeCount` as a compatibility field

#### Scenario: 不再保存任务来源结构化列
- **WHEN** server 持久化处理任务
- **THEN** `processing_task` SHALL NOT persist `source_type`
- **AND** `processing_task` SHALL NOT persist `source_ref_id`
- **AND** task query API SHALL NOT expose `sourceType` or `sourceRefId` as compatibility fields
- **AND** low-frequency trigger or batch context SHALL be stored only as a safe summary in `task_params_json` when needed

### Requirement: 使用 server task id 作为跨组件任务标识
server SHALL use `server_task_id` as the single cross-component task identifier for server-agent fund refresh dispatch, callback correlation, idempotency, snapshot source reference, and processing log source reference.

#### Scenario: 下发 agent 任务
- **WHEN** server dispatches a fund refresh task to agent
- **THEN** the dispatch request SHALL include `server_task_id`
- **AND** server SHALL NOT require an independent agent task reference for future callback correlation

#### Scenario: agent accepted response returns task id
- **WHEN** agent returns `agent_task_id`, `task_id`, or equivalent accepted task reference
- **THEN** server SHALL NOT persist it in `processing_task`
- **AND** task query API SHALL NOT expose `agentTaskRef` as a compatibility field
- **AND** server SHALL continue to correlate callbacks by `server_task_id`

### Requirement: 回调失败由任务主状态表达
server SHALL express callback failure through `processing_task.status`, `processing_task.error_summary`, and callback processing records instead of a separate diagnostic status column.

#### Scenario: agent reports callback failed
- **WHEN** server records a task terminal callback failure
- **THEN** server SHALL set `processing_task.status` to `callback_failed`
- **AND** server SHALL preserve a safe error summary when available
- **AND** server SHALL NOT persist `callback_diagnostic_status`
- **AND** task query API SHALL NOT expose `callbackDiagnosticStatus` as a compatibility field

#### Scenario: 查询回调失败任务
- **WHEN** a caller queries a task that ended with callback failure
- **THEN** server SHALL expose the main status as `callback_failed`
- **AND** server SHALL NOT require a separate callback diagnostic status to interpret the terminal state

### Requirement: 使用处理日志表达诊断信息
server SHALL persist processing diagnostics in `processing_log` rather than `agent_warning`. `processing_log` SHALL contain only `id`, `source_ref_id`, `module`, `event`, `message`, `severity`, `create_time`, and `update_time`.

#### Scenario: 保存 agent 回调诊断日志
- **WHEN** agent callback includes a refresh warning, error, or informational diagnostic
- **THEN** server SHALL persist a `processing_log` row
- **AND** `source_ref_id` SHALL reference the related `processing_task.server_task_id`
- **AND** `module` and `event` SHALL contain machine-recognizable classification
- **AND** `severity` SHALL preserve the diagnostic level
- **AND** server SHALL NOT require legacy callback diagnostic fields `code` or `fund_code`

#### Scenario: 不保存基金或快照专用日志字段
- **WHEN** server persists a processing diagnostic
- **THEN** `processing_log` SHALL NOT persist `snapshot_id`
- **AND** `processing_log` SHALL NOT persist `fund_code`
- **AND** `processing_log` SHALL NOT persist `source_type`
- **AND** `processing_log` SHALL NOT persist `warning_type`
- **AND** `processing_log` SHALL NOT persist `code`
- **AND** `processing_log` SHALL NOT persist `source_section`
- **AND** `processing_log` SHALL NOT persist `source_row_number`

#### Scenario: 通过处理任务定位快照上下文
- **WHEN** server needs to inspect snapshots related to a processing diagnostic
- **THEN** server SHALL use `processing_log.source_ref_id` to match `fund_detail_snapshot.source_ref_id`
- **AND** server SHALL NOT depend on a direct `snapshot_id` stored in `processing_log`

### Requirement: 收敛基金详情快照来源字段
server SHALL keep fund detail snapshots focused on public fund detail facts and use `source_ref_id` for processing task linkage.

#### Scenario: 保存基金详情快照
- **WHEN** server persists a fund detail snapshot from agent callback
- **THEN** `fund_detail_snapshot.source_ref_id` SHALL reference the related `processing_task.server_task_id`
- **AND** `fund_detail_snapshot` SHALL NOT persist `source_type`

#### Scenario: 查询基金详情
- **WHEN** server queries latest fund detail snapshots for portfolio composition
- **THEN** query behavior SHALL NOT depend on `fund_detail_snapshot.source_type`

### Requirement: 保护任务参数和诊断隐私
server SHALL prevent sensitive personal financial data and credentials from being stored in task parameter summaries, processing diagnostics, error summaries, or logs.

#### Scenario: 保存任务和诊断摘要
- **WHEN** server writes `task_params_json`, `processing_log.message`, `error_summary`, or application logs for this flow
- **THEN** the content SHALL NOT include real account names, holding amounts, full portfolio composition, API keys, tokens, cookies, callback credentials, or raw request bodies
- **AND** the content MAY include task id, task type, fund code count, module, event, status, and safe error summaries
