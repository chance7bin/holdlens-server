## 当前行为

`AgentFundRefreshCaseImpl` 中三类回调方法均使用 `@Transactional(rollbackFor = Exception.class)` 包裹完整处理流程：

1. 查询并校验 `processing_task`。
2. 校验 schema version 和 idempotency key。
3. 保存 `processing_callback` 幂等记录。
4. 成功或部分失败回调写入业务结果。
5. 更新 `processing_task.status` 为业务终态。
6. 标记 callback 处理成功。

如果第 4 步业务落库失败，catch 中虽然尝试标记 callback 处理失败，但异常继续抛出，事务整体回滚，导致 `processing_task.status` 仍为 `running`，`processing_callback` 失败记录也不会可靠保留。

## 设计决策

### 1. server 处理回调失败使用 `callback_failed`

当任务已识别、幂等键存在，且失败发生在 server 处理回调阶段时，任务终态使用 `callback_failed`。

选择原因：

- `failed` 表达 agent 刷新任务本身失败，或明确的失败业务结果。
- `callback_failed` 表达回调链路或 server 回调处理失败，更符合“agent 已回调但 server 未能可靠保存结果”的语义。
- `callback_failed` 已是 `processing_task` 的终态，不会继续阻塞新的同类型任务创建。

### 2. 回调处理事务与失败终态记录分离

回调业务处理应在可回滚事务中执行，避免业务结果部分写入后提交。处理失败后，再用独立事务记录：

- `processing_task.status = callback_failed`
- 安全 `error_summary`
- 可保存时的 `processing_callback.process_status = failed`

这样业务结果可回滚，任务终态仍可持久化。

### 3. 不扩大非法回调处理范围

无法识别任务、任务类型不匹配、缺少幂等键等校验失败仍按现有业务错误拒绝，不强行终态化未知任务。

schema version 不支持时，任务已识别，继续标记为 `failed`，并确保该状态不会被同一失败事务回滚。

### 4. 分层边界

- Trigger 层不变，只负责接收请求和委派 Case。
- Case 层保留回调编排和事务边界控制。
- 业务无关的事务模板细节抽到 Case 层 `TransactionExecutor` 支持组件，回调用例只表达 `required` / `requiresNew` 事务语义。
- Domain 层仍只定义任务实体、状态值对象和 Repository 接口。
- Infrastructure 层继续只通过 Mapper XML 更新本地数据，不承载业务判断。

## 风险与回滚

- 风险：如果 `processing_task` 自身不可写，失败终态仍无法保存。这属于基础任务表不可用，无法通过业务代码补偿。
- 风险：失败终态后，agent 重试同一幂等键不会再次写入业务结果；需要重新创建任务触发新一轮刷新。
- 回滚：恢复原回调事务包裹方式即可，但会重新出现 running 占坑风险。

## 安全影响

错误摘要必须继续使用安全摘要，限制长度并清理换行；不得写入凭据、账户、持仓金额或原始回调全文。
