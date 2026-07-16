## Why

基金目录 callback 当前要等待 27,000 余条目录数据全部写入并提交后才返回 2xx，实际处理约 112 秒，超过 agent callback 等待窗口并导致连接超时。批量 upsert 已降低数据库往返，但 HTTP 请求仍与长事务绑定，需要把“接收 callback”和“保存基金目录”拆开。

## What Changes

- 仅将 `fund_catalog_refresh` callback 改为先完成鉴权、契约校验、任务校验和幂等接收，再尝试提交到专用异步执行器并返回 HTTP 202；执行器拒绝时在返回前记录失败状态。
- `processing_callback.process_status = processing` 表示 server 已接收并正在异步处理，`processing_task` 在业务写入完成前保持 `running`。
- 异步业务事务继续按每批最多 500 条写入目录；成功后更新 `processed` 和任务终态，异常时使用独立事务更新 `failed` 和 `callback_failed`。
- Agent 未回调超时扫描只关闭不存在任何 `processing_callback` 接收记录的非终态任务。
- 已进入 Server 异步处理且超过 10 分钟的基金目录 callback 只打印安全 `WARN`，不按时间直接改为失败。
- 更新基金目录 callback 共享契约和相关测试；申购状态、阶段收益和重仓 callback 继续同步处理。

## Capabilities

### New Capabilities

- `fund-catalog-async-callback-processing`: 定义基金目录 callback 的异步接收确认、状态流转、异常收口、幂等和超时观测语义。

### Modified Capabilities

- `fund-catalog-batch-persistence`: HTTP 确认边界从“目录事务提交”调整为“callback 接收记录提交并成功提交异步执行”。
- `fund-data-slice-refresh`: callback 超时扫描需要区分 Agent 未回调与 Server 已接收处理中的 callback。

## Impact

- 影响 `holdlens-server-trigger` 的基金目录 callback HTTP 状态码和异步执行入口。
- 影响 `holdlens-server-case` 的 callback 接收事务、异步业务事务、失败状态和慢任务告警编排。
- 影响 processing Domain Repository、Infrastructure Repository/DAO 和 MyBatis Mapper 的超时查询条件。
- 影响 `processing_callback.process_status` 语义和数据库初始化注释，不新增业务字段。
- 影响根目录 `contracts/holdlens-server/agent/fund-catalog-refresh-callback.md`；callback payload schema 保持 v1。
