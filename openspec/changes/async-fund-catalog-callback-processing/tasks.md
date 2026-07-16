## 1. 契约与状态语义

- [x] 1.1 更新基金目录 callback 共享契约，明确 HTTP 202、异步最终状态、失败补偿和超时语义。
- [x] 1.2 更新 `processing_callback.process_status` 初始化 SQL 注释，补充 `processing` 状态。

## 2. 异步 callback 实现

- [x] 2.1 增加基金目录专用单线程有界执行器，拒绝时不得回退到 HTTP 请求线程执行。
- [x] 2.2 在 Case 层拆分 callback 接收短事务和异步目录业务事务，保持其他基金切片同步路径不变。
- [x] 2.3 异步失败或执行器拒绝时，以独立事务更新 callback `failed` 和任务 `callback_failed`。
- [x] 2.4 基金目录 callback Controller 返回 HTTP 202，重复幂等 callback 不重复提交任务。

## 3. 超时与观测

- [x] 3.1 Agent 未回调候选查询和条件更新增加 `NOT EXISTS processing_callback`。
- [x] 3.2 增加默认 10 分钟的基金目录 `processing` 慢任务扫描，只打印安全 WARN，不修改状态。
- [x] 3.3 补充 callback 接收、异步开始/完成/异常、慢处理和执行器拒绝的有界日志。

## 4. 测试与验证

- [x] 4.1 补充 Case 测试，覆盖快速接收、异步成功、异步失败、重复幂等和慢处理不改状态。
- [x] 4.2 补充 Trigger 测试，覆盖基金目录 HTTP 202 和其他基金 callback 语义不变。
- [x] 4.3 补充 Repository/Mapper 测试，固定 Agent 未回调 `NOT EXISTS` 和慢处理查询条件。
- [x] 4.4 使用 JDK 17 串行运行相关测试和 server Maven 聚合测试。
- [x] 4.5 运行 `openspec validate --strict async-fund-catalog-callback-processing`，并核对任务状态、安全与兼容性。
