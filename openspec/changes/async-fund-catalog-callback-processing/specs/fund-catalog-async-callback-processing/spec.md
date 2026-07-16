## ADDED Requirements

### Requirement: 基金目录 callback 异步接收确认

server SHALL 在基金目录 callback 完成鉴权、契约校验、任务校验和幂等接收记录提交后尝试提交异步任务并返回 HTTP 202，且 SHALL NOT 等待基金目录业务事务完成或回退到 HTTP 线程处理。

#### Scenario: 首次接收有效 callback
- **WHEN** agent 提交合法且此前未接收的基金目录 callback
- **THEN** server SHALL 保存 `processing_callback.process_status = processing`
- **AND** server SHALL 让对应 `processing_task` 保持或进入 `running`
- **AND** server SHALL 提交一次异步目录处理
- **AND** server SHALL 返回 HTTP 202 和当前任务状态

#### Scenario: 重复提交相同幂等键
- **WHEN** agent 再次提交相同 `server_task_id` 和 `idempotency_key`
- **THEN** server SHALL 返回当前任务状态
- **AND** server SHALL NOT 再次提交异步目录处理

#### Scenario: 接收校验或接收事务失败
- **WHEN** callback 鉴权、契约、任务、幂等接收记录或接收事务失败
- **THEN** server SHALL 返回对应非 2xx 响应
- **AND** server SHALL NOT 提交异步目录处理

### Requirement: 异步目录处理保持原子结果

server SHALL 在独立异步业务事务中按每批最多 500 条处理基金目录，并在事务完成后更新 callback 和任务终态。

#### Scenario: 异步处理成功
- **WHEN** 所有目录批次、warning 和状态更新成功提交
- **THEN** server SHALL 将 callback 更新为 `processed`
- **AND** server SHALL 将任务更新为 callback 结果对应终态

#### Scenario: 异步处理异常
- **WHEN** 任一目录批次或业务状态更新抛出异常
- **THEN** server SHALL 回滚本次目录业务事务
- **AND** server SHALL 使用独立事务将 callback 更新为 `failed`
- **AND** server SHALL 将任务更新为 `callback_failed`
- **AND** server SHALL 记录不含 payload 和逐条基金的异常日志

#### Scenario: 异步执行器拒绝任务
- **WHEN** callback 接收记录已经提交但专用执行器拒绝异步任务
- **THEN** server SHALL 将 callback 和任务更新为失败
- **AND** server SHALL NOT 在 HTTP 请求线程同步执行目录写入
- **AND** server SHALL 以 HTTP 202 返回最新失败任务状态，等待人工重新调度

### Requirement: Callback 超时区分未回调和慢处理

server SHALL 只把超过 Agent 回调窗口且不存在 `processing_callback` 接收记录的非终态任务标记为 `callback_failed`，并 SHALL NOT 根据 Server 异步处理时长直接改变任务状态。

#### Scenario: Agent 始终没有回调
- **WHEN** 基金切片任务超过配置的 Agent 回调窗口
- **AND** 该任务不存在任何 `processing_callback` 记录
- **THEN** server SHALL 将任务更新为 `callback_failed`
- **AND** error summary SHALL 表达 `agent callback timeout`

#### Scenario: Server 异步处理超过告警阈值
- **WHEN** 基金目录 callback 处于 `processing` 且持续超过默认 10 分钟
- **THEN** server SHALL 打印有界 WARN 日志
- **AND** callback SHALL 保持 `processing`
- **AND** task SHALL 保持 `running`

#### Scenario: 超时扫描遇到已接收 callback
- **WHEN** 非终态任务虽然超过 Agent 回调窗口但已经存在 `processing_callback`
- **THEN** Agent 未回调扫描 SHALL NOT 将该任务更新为 `callback_failed`

### Requirement: 基金目录异步日志保持安全

server SHALL 记录基金目录 callback 接收、异步开始、批次执行、完成、失败和慢处理日志，且不得打印完整 callback payload、逐条基金数据、鉴权 Header 或凭据。

#### Scenario: 观测异步生命周期
- **WHEN** callback 被接收、开始处理或完成
- **THEN** server SHALL 记录 server task ID、输入数量、批次数量、状态和耗时等必要摘要

#### Scenario: 观测慢处理
- **WHEN** callback 处理超过告警阈值
- **THEN** server SHALL 记录 server task ID、处理状态和阈值
- **AND** server SHALL NOT 仅因日志告警修改业务状态
