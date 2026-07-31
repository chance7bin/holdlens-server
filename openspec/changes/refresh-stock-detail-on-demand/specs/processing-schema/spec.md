## MODIFIED Requirements

### Requirement: 收敛处理任务主表字段

server SHALL use `processing_task` as a generic processing task table focused on task identity, task type, main status, safe parameter summary, optional active business key and lease, error summary, and timestamps. `active_key` and `lease_until` SHALL only be populated for workflows requiring persistent single-flight.

#### Scenario: 创建基金刷新任务时保存安全参数摘要

- **WHEN** server 创建 `fund_detail_refresh` 处理任务
- **THEN** server SHALL 保存 `server_task_id`、`task_type`、`status`、`task_params_json`、`create_time` 和 `update_time`
- **AND** `task_params_json` SHALL include safe summary fields such as `fundCodeCount`
- **AND** `task_params_json` SHALL NOT include account names, holding amounts, full portfolio composition, tokens, callback credentials, API keys, or full raw requests

#### Scenario: 股票详情任务保存活动键

- **WHEN** server 创建股票详情按需刷新任务
- **THEN** server SHALL 保存唯一 `active_key` 和 `lease_until`
- **AND** active key SHALL 只包含公开 assetRef 派生值，不得包含用户、账户或持仓数据。

#### Scenario: 终态任务释放活动键

- **WHEN** processing task 更新为任一终态
- **THEN** server SHALL 在同一 SQL 更新中把 `active_key` 和 `lease_until` 置为 null。

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
