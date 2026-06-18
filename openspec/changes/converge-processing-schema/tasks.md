## 1. 契约与范围确认

- [x] 1.1 确认 ADR-003 与本 change 覆盖范围一致，并与 agent change `align-server-task-processing-contract` 对齐。
- [x] 1.2 确认兼容策略：`fundCodeCount` 不兼容输出，旧任务字段 `sourceType`、`sourceRefId`、`agentTaskRef`、`callbackDiagnosticStatus` 不保留。
- [x] 1.3 确认 schema 调整方式：不做在线迁移，直接修改初始化 SQL，后续重新执行初始化脚本和导入脚本。
- [x] 1.4 确认 agent 诊断契约：agent 不输出旧 `code` / `fund_code` 字段，server 不做旧字段兼容映射。

## 2. `processing_task` schema 收敛

- [x] 2.1 调整数据库初始化 SQL：删除 `callback_diagnostic_status`、`fund_code_count`、`source_type`、`source_ref_id`、`agent_task_ref`，新增 `task_params_json`；不新增在线迁移脚本。
- [x] 2.2 调整 `ProcessingTaskEntity`、`ProcessingTaskPO`、Repository、DAO、Mapper XML 和相关测试，使任务参数摘要通过 `taskParamsJson` 保存。
- [x] 2.3 调整任务创建逻辑，把基金代码数量和安全触发来源摘要写入 `task_params_json`，不保存原始请求或敏感上下文。
- [x] 2.4 调整任务状态逻辑，回调失败只由 `status = callback_failed`、`error_summary` 和 `processing_callback` 表达。
- [x] 2.5 调整 agent 下发逻辑，不再读取或保存 agent 独立任务引用。

## 3. `processing_log` schema 收敛

- [x] 3.1 调整数据库初始化 SQL，将 `agent_warning` 直接替换为 `processing_log`，目标字段为 `id`、`source_ref_id`、`module`、`event`、`message`、`severity`、`create_time`、`update_time`。
- [x] 3.2 重命名或新增 `ProcessingLogPO`、DAO、Mapper XML 和 Repository 映射，删除 `snapshotId`、`fundCode`、`sourceType`、`warningType`、`code`、`sourceSection`、`sourceRowNumber`。
- [x] 3.3 调整 agent 回调 warning 映射，只接收并保存 `module` + `event`，不兼容旧 `code` / `fund_code` 字段。
- [x] 3.4 调整查询或内部使用逻辑，快照上下文通过 `source_ref_id` 关联 `fund_detail_snapshot.source_ref_id`，不依赖 `snapshot_id`。

## 4. `fund_detail_snapshot` schema 收敛

- [x] 4.1 调整数据库初始化 SQL，删除 `fund_detail_snapshot.source_type`；不新增在线迁移脚本。
- [x] 4.2 调整 `FundDetailSnapshotPO`、`FundDetailSnapshotAggregate`、Repository、Mapper XML 和回调转换逻辑，删除 `sourceType` 字段。
- [x] 4.3 确认快照来源上下文通过 `source_ref_id = processing_task.server_task_id` 保持可追踪。

## 5. API、Case 与集成契约

- [x] 5.1 调整 API DTO、请求/响应、Controller 转换和 Case 命令/结果模型，直接移除 `fundCodeCount` 和旧任务字段兼容输出。
- [x] 5.2 调整 agent port 响应解析，不再要求或保存 `agent_task_id` / `task_id`。
- [x] 5.3 调整 agent 回调请求 DTO，支持 `module` + `event` 诊断字段，并移除旧 `code` / `fund_code` 字段兼容。
- [x] 5.4 补充或更新任务创建、任务查询、下发成功/失败、回调成功、部分失败、回调失败、重复回调、快照保存和处理日志保存测试。

## 6. 质量门

- [x] 6.1 运行相关 Maven 模块测试或全量测试。
- [x] 6.2 运行 `openspec validate --strict converge-processing-schema`。
- [x] 6.3 从产品、工程、QA、发布、安全五个视角做轻量评审，确认没有真实资产明细、账户标识或凭据进入 `task_params_json`、`processing_log`、`error_summary` 或日志。
