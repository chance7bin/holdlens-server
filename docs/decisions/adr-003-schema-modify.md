# ADR-003 Server 表结构收敛

## 1. 背景

本 ADR 记录 server 侧数据库表结构收敛事项。每个表的待删除字段、取舍原因、影响和后续动作独立维护，避免不同表的结构决策互相混淆。

## 2. `processing_task`

### 2.1 `callback_diagnostic_status`

#### 2.1.1 背景

`processing_task` 是 server 编排 agent 异步基金刷新任务的任务记录表。当前表中同时存在 `status` 和 `callback_diagnostic_status`：

- `status` 表示任务主状态，包含 `created`、`dispatched`、`running`、`succeeded`、`partial_failed`、`failed`、`dispatch_failed`、`callback_failed` 等状态。
- `callback_diagnostic_status` 原本用于保留回调链路诊断状态。

按当前实现，`callback_diagnostic_status` 仅在 agent 回调状态为 `callback_failed` 时被设置为同名值，且不参与任务状态流转、查询条件、幂等判断或基金数据落库决策。也就是说，当前它没有提供区别于 `status = callback_failed` 的独立业务语义。

#### 2.1.2 决策

- 后续调整当前库表结构时，计划删除 `processing_task.callback_diagnostic_status` 字段。
- 回调失败的任务终态继续由 `processing_task.status = callback_failed` 表达。
- 可展示或排查的错误摘要继续使用 `processing_task.error_summary`。
- 单次回调处理过程和幂等状态继续由 `processing_callback` 记录。

#### 2.1.3 备选方案

- 方案 A：保留 `callback_diagnostic_status`，后续扩展更多回调诊断枚举。
- 方案 B：删除 `callback_diagnostic_status`，使用已有 `status`、`error_summary` 和 `processing_callback` 表达回调结果与诊断信息。

#### 2.1.4 取舍原因

- 选择方案 B，因为当前字段只重复保存 `callback_failed`，会让任务主状态和诊断状态产生双重事实来源。
- `status` 已经能表达任务进入回调失败终态，`error_summary` 能保存安全错误摘要，`processing_callback` 能记录回调幂等键、回调状态和处理状态。
- 删除冗余字段可以降低 DTO、PO、Mapper 和表结构的维护成本，避免后续调用方误以为该字段承载独立状态机。

#### 2.1.5 影响

- 正向影响：
  - `processing_task` 表结构更简单。
  - 任务状态语义集中在 `status`，减少状态解释歧义。
  - 后续 API、领域对象、PO 和 MyBatis 映射可同步收敛。
- 负向影响：
  - 删除字段前需要确认没有前端、脚本或外部调用方依赖 `callbackDiagnosticStatus`。
  - 如果未来需要更细粒度的回调诊断枚举，需要通过新的 OpenSpec change 重新设计诊断模型。
- 后续动作：
  - 在对应 OpenSpec change 中定义删除字段的范围、兼容策略和验收标准。
  - 删除代码中的 `callbackDiagnosticStatus` DTO、领域实体、PO 和 Mapper 映射。
  - 调整数据库初始化 SQL 和迁移脚本。
  - 运行相关任务创建、回调成功、部分失败、回调失败和重复回调测试。

### 2.2 `fund_code_count`

#### 2.2.1 背景

`processing_task` 的定位应是通用处理任务记录表，而不是基金刷新专用任务表。当前 `fund_code_count` 表示基金刷新任务中的基金代码数量，便于任务列表展示、失败排查和评估任务规模；但字段名直接绑定 `fund_detail_refresh` 场景。如果后续同一张表承载 OCR、导入、同步、重算等任务，`fund_code_count` 会让通用任务表暴露基金业务字段，增加字段解释成本。

任务记录仍需要保存可排查的任务上下文摘要，但该上下文不应通过不断增加任务类型专用列来表达，也不应保存完整原始请求、账户金额、完整持仓、token、回调鉴权等敏感或不必要信息。

#### 2.2.2 决策

- 后续调整当前库表结构时，计划将 `processing_task.fund_code_count` 收敛为通用任务参数摘要字段，建议字段名为 `task_params_json`。
- `task_params_json` 只保存安全、必要、可展示或可排查的任务参数摘要，不保存完整原始请求或敏感上下文。
- 当前基金刷新任务可在 `task_params_json` 中保存 `fundCodeCount` 等摘要信息，而不是继续在通用任务表中保留基金专用列。
- `processing_task` 的主状态、任务类型、错误摘要和时间字段继续保持结构化列；任务类型特有的补充上下文放入参数摘要 JSON。

#### 2.2.3 备选方案

- 方案 A：继续保留 `fund_code_count`，接受 `processing_task` 对基金刷新场景的字段耦合。
- 方案 B：删除 `fund_code_count`，不再保存任务规模摘要。
- 方案 C：将 `fund_code_count` 重命名为 `target_count` 或 `item_count`，用单一数字表达任务目标数量。
- 方案 D：将 `fund_code_count` 收敛为 `task_params_json`，以安全摘要形式保存不同任务类型的补充参数。

#### 2.2.4 取舍原因

- 选择方案 D，因为 `task_params_json` 能让 `processing_task` 保持通用任务记录表定位，同时保留任务排查所需上下文。
- 方案 B 会丢失任务规模摘要，不利于排查刷新失败、调度异常和批量任务影响面。
- 方案 C 比 `fund_code_count` 更通用，但只能表达单一数量；不同任务类型可能需要记录文件名、行数、页数、对象数、批次号等不同摘要，继续增加结构化列会让表结构膨胀。
- `task_params_json` 必须作为任务参数摘要，而不是原始请求归档；敏感信息和不必要的业务事实不应写入该字段。

#### 2.2.5 影响

- 正向影响：
  - `processing_task` 表结构从基金刷新场景解耦，更适合作为通用处理任务记录表。
  - 后续新增任务类型时，可通过参数摘要表达少量任务特有上下文，减少新增专用列。
  - 当前基金刷新仍可保留 `fundCodeCount` 这类排查信息。
- 负向影响：
  - 删除或替换字段前需要确认没有前端、脚本或外部调用方依赖 `fundCodeCount` 响应字段。
  - 如果 API 仍需要返回 `fundCodeCount`，需要从 `task_params_json` 解析并兼容输出，避免外部契约突变。
  - JSON 字段不适合作为高频查询条件；如未来需要按任务参数筛选，应通过新的 OpenSpec change 设计结构化索引字段。
- 后续动作：
  - 在对应 OpenSpec change 中定义 `fund_code_count` 到 `task_params_json` 的迁移范围、兼容策略和验收标准。
  - 调整数据库初始化 SQL 和迁移脚本。
  - 调整 `ProcessingTaskEntity`、`ProcessingTaskPO`、Mapper、Case 结果模型、API DTO 和相关转换逻辑。
  - 明确 `task_params_json` 的安全写入约束，并补充任务创建、任务查询和基金刷新回归测试。

### 2.3 `source_type` / `source_ref_id` / `agent_task_ref`

#### 2.3.1 背景

当前 `processing_task` 同时保存 `server_task_id`、`source_type`、`source_ref_id` 和 `agent_task_ref`：

- `server_task_id` 是 server 生成的全链路任务标识，已经用于任务查询、agent 回调关联、回调幂等和快照来源关联。
- `source_type` 表示任务创建来源类型，当前主要为系统级任务来源，不参与查询、状态流转、幂等判断或回调处理。
- `source_ref_id` 表示任务创建来源引用，当前实现没有按该字段查询任务，也没有稳定的上游批次、导入单或调度记录模型与之关联。
- `agent_task_ref` 表示 agent 返回的任务引用，但当前 agent 任务请求已经携带 `server_task_id`，回调也使用同一个 `server_task_id` 关联 server 任务。

如果 agent 侧直接使用 server 下发的 `server_task_id` 作为任务标识，那么 `agent_task_ref` 会变成同一任务的第二个 ID，增加跨系统排查时的解释成本。`source_type` 和 `source_ref_id` 也更适合作为任务参数摘要或上游专门模型表达，而不是作为通用任务表的一等结构化列长期保留。

#### 2.3.2 决策

- 后续调整当前库表结构时，计划删除 `processing_task.source_type`、`processing_task.source_ref_id` 和 `processing_task.agent_task_ref` 字段。
- `server_task_id` 作为 server 与 agent 之间的全链路唯一任务标识。agent 接收任务后应直接使用该 ID 作为任务标识、幂等键前缀和回调关联 ID。
- 如需记录任务触发来源、调度批次、导入批次等低频排查上下文，优先写入 `task_params_json` 的安全摘要，而不是保留 `source_type` / `source_ref_id` 结构化列。
- 如未来需要表达 agent 侧多次执行尝试、重试实例、内部 job id 或调度执行明细，应通过新的 OpenSpec change 设计 `processing_task_attempt`、`dispatch_attempt` 或 `processing_log`，不再把执行实例引用混入任务主表。

#### 2.3.3 备选方案

- 方案 A：继续保留 `source_type`、`source_ref_id` 和 `agent_task_ref`，接受任务主表存在来源字段和跨系统第二任务 ID。
- 方案 B：删除 `source_type` 和 `source_ref_id`，但保留 `agent_task_ref` 作为 agent 侧任务引用。
- 方案 C：删除 `source_type`、`source_ref_id` 和 `agent_task_ref`，统一使用 `server_task_id` 串联 server、agent、回调、快照和处理日志。

#### 2.3.4 取舍原因

- 选择方案 C，因为当前第一阶段任务链路已经以 `server_task_id` 为唯一关联锚点，额外保留 `agent_task_ref` 会形成同一任务的双 ID。
- `source_type` 当前基本没有提供独立业务语义；如果来源只是系统调度或手动触发等摘要信息，可放入 `task_params_json`。
- `source_ref_id` 当前没有稳定查询闭环和上游引用模型，作为结构化索引字段会提前固化不清晰的来源模型。
- 任务主表应聚焦任务身份、类型、状态、错误摘要和时间。来源上下文、执行尝试和诊断日志应分别由参数摘要、尝试表或处理日志表达。

#### 2.3.5 影响

- 正向影响：
  - `processing_task` 表结构更聚焦，减少来源字段和跨系统任务 ID 的重复解释成本。
  - server 与 agent 统一使用 `server_task_id`，任务查询、回调幂等和快照关联链路更直接。
  - 后续如果出现执行尝试或内部 job id，可单独建模，避免任务主表继续膨胀。
- 负向影响：
  - 删除字段前需要确认没有前端、脚本或外部调用方依赖 `sourceType`、`sourceRefId` 或 `agentTaskRef` 响应字段。
  - 如果当前 agent 已经生成并依赖独立任务 ID，需要先调整 agent 契约，使其接受并使用 `server_task_id` 作为任务标识。
  - 如果未来需要按来源引用高频查询任务，不能只依赖 JSON 摘要，需要通过新的 OpenSpec change 设计结构化来源模型和索引。
- 后续动作：
  - 在对应 OpenSpec change 中定义删除字段的范围、兼容策略和验收标准。
  - 调整 server 与 agent 的任务契约，明确 agent 使用 `server_task_id` 作为任务标识。
  - 删除代码中的 `sourceType`、`sourceRefId`、`agentTaskRef` DTO、领域实体、PO 和 Mapper 映射。
  - 删除数据库初始化 SQL 和迁移脚本中的相关字段与索引。
  - 运行任务创建、下发、回调成功、部分失败、重复回调、任务查询和快照关联测试。

## 3. `agent_warning` / `processing_log`

### 3.1 背景

`agent_warning` 原本用于保存解析、刷新、OCR、导入或 agent 处理过程中的 warning。随着异步基金刷新链路落地，当前实现暴露出几个结构语义问题：

- 表名叫 `agent_warning`，但字段 `severity` 允许表达 `info`、`warning`、`error`，实际承载的是处理链路诊断日志，而不只是 warning。
- 当前基金刷新场景中，`agent_warning.source_ref_id` 保存 `serverTaskId`，`agent_warning.snapshot_id` 又指向由同一个 `serverTaskId` 生成的 `fund_detail_snapshot`，二者在定位链路上存在重复。
- `snapshot_id` 是基金详情快照的局部业务关联，不适合作为解析、OCR、导入、agent 报告等通用处理日志字段。
- `fund_code` 只对基金刷新场景有意义，会让通用处理日志表被基金业务字段绑定。
- `source_type` 当前基本恒为 `agent`，而表名本身已经表达 agent 语义；如果未来承接多种处理来源，也可以通过处理任务或来源引用查上下文，不需要在日志表中重复保存。
- `warning_type` 和 `code` 都在表达分类。若继续保留单个 `code`，需要依赖字符串前缀，例如 `fund_refresh.top_holdings_missing`；拆成 `module` 和 `event` 后，查询与聚合更直接。
- `source_section` 和 `source_row_number` 只从 agent 回调请求透传到落库，没有形成稳定的查询条件、接口展示或排查闭环。

本表应定位为处理链路的诊断与可观测记录，而不是业务事实源，也不驱动任务状态变更。任务主状态仍以 `processing_task` 为准。

### 3.2 决策

- 后续调整表结构时，计划将 `agent_warning` 收敛为 `processing_log`。
- `processing_log` 只记录处理链路中的诊断与可观测信息，不作为业务事实源，不驱动任务状态变更。
- `source_ref_id` 继续作为处理链路引用，当前基金刷新场景中对应 `processing_task.server_task_id`。
- 删除 `snapshot_id`。需要快照上下文时，通过 `fund_detail_snapshot.source_ref_id = processing_log.source_ref_id` 查询。
- 删除 `fund_code`。基金相关错误信息可写入 `message`；需要机器聚合时，使用 `module` 和 `event`。
- 删除 `source_type`。来源上下文优先由 `processing_task` 或其他来源引用表表达，日志表不重复维护。
- 删除 `warning_type` 和 `code`，改为 `module` 与 `event` 两个字段共同表达机器可识别事件。
- 保留 `severity`，用于表达 `info`、`warning`、`error` 等处理日志级别。因为存在多个级别，表名不再使用 `warning`。
- 删除 `source_section` 和 `source_row_number`。如果未来需要章节、行号、文件页码等细粒度定位，应通过新的 OpenSpec change 重新设计结构化上下文字段。
- 目标字段收敛为：`id`、`source_ref_id`、`module`、`event`、`message`、`severity`、`create_time`、`update_time`。

### 3.3 备选方案

- 方案 A：保留 `agent_warning` 表名，只删除 `source_section` 和 `source_row_number`。
- 方案 B：保留 `agent_warning` 表名，并继续保存 `snapshot_id`、`fund_code`、`source_type`、`warning_type`、`code` 等字段。
- 方案 C：将表改为 `processing_event`，用事件表表达处理链路中发生的结构化事件。
- 方案 D：将表改为 `processing_event_log`，明确表达处理事件日志。
- 方案 E：将表改为 `processing_log`，表达处理链路诊断日志，并将分类收敛为 `module` 和 `event`。

### 3.4 取舍原因

- 选择方案 E。
- `processing_log` 比 `agent_warning` 更准确，因为该表会记录 `info`、`warning`、`error` 等不同级别的处理诊断信息。
- `processing_log` 比 `event_log` 边界更清楚，避免被误解为全系统事件日志、审计日志或领域事件日志。
- `processing_log` 比 `processing_event` 更弱化业务事件语义，避免调用方误以为该表参与状态流转或业务事实沉淀。
- `snapshot_id` 可以通过 `source_ref_id` 查询基金详情快照获得，不需要在日志表中重复保存。
- `fund_code` 是基金业务字段，不适合放进通用处理日志表。基金定位信息可以放入 `message`，机器侧分类依赖 `module` 和 `event`。
- `source_type` 与表名或处理任务上下文重复，保留会增加字段解释成本。
- `module` 和 `event` 拆分后，比单个带前缀的 `code` 更便于查询、统计和复用。例如 `module = fund_refresh`，`event = top_holdings_missing`。
- `severity` 有助于展示、筛选和排查优先级，因此保留；但保留后表名不应继续使用 `warning`。

### 3.5 影响

- 正向影响：
  - 表结构从基金刷新局部场景中解耦，更适合作为解析、刷新、OCR、导入和 agent 处理的统一诊断日志。
  - 处理状态仍集中在 `processing_task`，诊断信息集中在 `processing_log`，减少事实来源混淆。
  - `module` 和 `event` 便于按模块、事件类型做查询和统计。
  - 删除 `snapshot_id`、`fund_code`、`source_type`、`warning_type`、`code`、`source_section` 和 `source_row_number` 后，DTO、领域对象、PO、Mapper 和 DDL 字段维护成本降低。
- 负向影响：
  - 删除字段和重命名表前，需要确认没有前端、脚本或外部调用方依赖 `agent_warning`、`snapshot_id`、`fund_code`、`source_type`、`warning_type`、`code`、`source_section` 和 `source_row_number`。
  - 按基金代码直接查询 warning 的能力会被移除；如需基金维度排查，应先通过处理任务或快照查询上下文，或在后续 OpenSpec change 中设计专门查询。
  - 如果未来需要结构化上下文，例如基金代码、文件名、页码、章节或行号，需要重新评估是否增加 `context_json` 或专门的上下文字段。
- 后续动作：
  - 在对应 OpenSpec change 中定义删除字段的范围、兼容策略和验收标准。
  - 将 `agent_warning` 表、DAO、PO、Mapper 和相关领域对象重命名或迁移为 `processing_log`。
  - 删除代码中的 `snapshotId`、`fundCode`、`sourceType`、`warningType`、`code`、`sourceSection`、`sourceRowNumber` 字段映射。
  - 新增 `module` 和 `event` 字段，并把现有 warning code 映射为对应模块与事件。
  - 调整数据库初始化 SQL 和迁移脚本。
  - 运行相关 agent 回调、处理日志保存、基金详情刷新和任务查询测试。

## 4. `fund_detail_snapshot`

### 4.1 背景

`fund_detail_snapshot` 用于保存基金公开详情快照。当前表中存在 `source_type` 和 `source_ref_id`：

- `source_type` 表示快照来源类型，DDL 注释中包含 `agent`、`api_sync` 等来源。
- `source_ref_id` 表示来源引用，当前 agent 异步基金刷新场景中对应 `processing_task.server_task_id`。

按当前实现，基金详情快照只由 agent 回调链路写入，`source_type` 固定为 `agent`。查询最新基金详情时只依据基金代码、快照状态和生成时间，不按 `source_type` 过滤；幂等、任务状态、回调诊断和快照定位也不依赖该字段。也就是说，当前 `source_type` 没有提供区别于 `processing_task` 和 `source_ref_id` 的独立业务语义。

### 4.2 决策

- 后续调整当前库表结构时，计划删除 `fund_detail_snapshot.source_type` 字段。
- 快照来源上下文继续通过 `fund_detail_snapshot.source_ref_id` 关联 `processing_task.server_task_id` 获得。
- 如果未来引入非 agent 来源的基金详情快照，应通过新的 OpenSpec change 重新设计来源模型，而不是提前保留当前冗余字段。

### 4.3 备选方案

- 方案 A：保留 `source_type`，继续作为未来 `agent`、`api_sync` 等来源的预留字段。
- 方案 B：删除 `source_type`，来源上下文统一通过 `source_ref_id` 和对应处理任务表达。

### 4.4 取舍原因

- 选择方案 B，因为当前 `source_type` 基本恒为 `agent`，不参与查询、状态流转、幂等判断或基金详情展示。
- `source_ref_id` 已能定位到处理任务，处理任务中已有任务类型、来源、状态、时间和错误摘要，继续保留快照级 `source_type` 会形成重复解释成本。
- 删除该字段可以减少 DDL、PO、Mapper、领域聚合和用例转换中的字段维护成本。

### 4.5 影响

- 正向影响：
  - `fund_detail_snapshot` 表结构更聚焦于快照事实本身。
  - 来源语义集中到处理任务链路，减少快照表和任务表之间的重复上下文。
  - 后续 PO、Mapper、领域聚合和回调转换可同步收敛。
- 负向影响：
  - 删除字段前需要确认没有前端、脚本或外部调用方依赖 `fund_detail_snapshot.source_type`。
  - 如果未来确实存在多种基金详情快照来源，需要重新设计来源区分方式。
- 后续动作：
  - 在对应 OpenSpec change 中定义删除字段的范围、兼容策略和验收标准。
  - 删除数据库初始化 SQL 和迁移脚本中的 `fund_detail_snapshot.source_type`。
  - 删除代码中的 `FundDetailSnapshotPO.sourceType`、`FundDetailSnapshotAggregate.sourceType`、Mapper 映射和 agent 回调构造赋值。
  - 运行相关 agent 回调、基金详情快照保存和账户基金详情查询测试。
