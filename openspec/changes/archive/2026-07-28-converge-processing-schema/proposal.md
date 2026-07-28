## Why

ADR-003 已明确 server 侧当前处理任务、处理诊断日志和基金详情快照表存在字段语义重复、基金场景耦合和跨系统双任务 ID 问题。server 作为长期业务事实源，需要把这些结构收敛到稳定、可审计、可迁移的数据库与业务模型中，避免后续 OCR、导入、同步、重算等任务继续沿用基金刷新专用字段。

## What Changes

- 收敛 `processing_task`：删除 `callback_diagnostic_status`、`source_type`、`source_ref_id`、`agent_task_ref`，将 `fund_code_count` 替换为安全任务参数摘要 `task_params_json`。
- 统一任务标识：server 与 agent 的基金刷新链路以 `server_task_id` 作为唯一全链路任务标识，不再持久化独立 agent 任务引用。
- 将 `agent_warning` 收敛为 `processing_log`：保留处理诊断日志定位，删除基金和快照专用字段，使用 `module` + `event` 表达机器可识别分类。
- 收敛 `fund_detail_snapshot`：删除冗余 `source_type`，通过 `source_ref_id` 关联 `processing_task.server_task_id` 获取来源上下文。
- 调整 API、Case、Domain、Infrastructure、Mapper、DDL 和迁移脚本，使表结构、领域模型和对外契约一致。

## Capabilities

### New Capabilities

- `processing-schema`: server 处理任务、处理日志和基金详情快照的 schema 收敛能力。

### Modified Capabilities

- `agent-async-fund-refresh`: 基金刷新任务查询、下发、回调保存和诊断日志保存需要使用收敛后的字段与契约。
- `infra-db-mapping`: 数据库初始化 SQL、迁移脚本、PO、DAO 和 MyBatis XML 需要与收敛后的表结构一致。

## Impact

- 影响 server 数据库：`processing_task`、`agent_warning`/`processing_log`、`fund_detail_snapshot` 表结构和索引。
- 影响 server 分层模型：API DTO、Case 命令/结果、Domain 实体/聚合、Infrastructure PO/DAO/Repository、MyBatis XML。
- 影响 agent 集成契约：server 不再依赖或保存 agent 独立任务引用；回调 warning 需要映射为 `module` + `event` 诊断日志。
- 影响兼容策略：需要明确旧字段删除、`fundCodeCount` 响应兼容、表重命名或迁移、旧数据映射和回滚方式。
- 影响安全：`task_params_json` 和 `processing_log.message` 只能保存安全、必要、可展示或可排查摘要，不能保存账户金额、真实账户名称、完整持仓、token、回调鉴权或原始请求全文。
