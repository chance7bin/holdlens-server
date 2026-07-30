# Design: 基金详情补全编排

## Context

基金净值历史已有长期事实表和只读 GET，但空数据只能永久显示为空。阶段业绩当前来自市场详情快照，缺少同类平均和同类排名字段，也没有可独立查询的规范化表。Agent 已有 `market_detail_data_refresh` 任务通道，适合扩展为缺失 slice 的受控采集入口。

本设计保持 `Trigger -> API -> Case -> Domain <- Infrastructure`：HTTP Trigger 只做协议绑定，Case 编排缺失检测、并发合并与派发，Domain 暴露任务和 slice 状态仓储接口，Infrastructure 实现数据库和 Agent Port。

## Goals / Non-Goals

### Goals

- 客户端可以显式请求补全基金净值历史和阶段业绩，并获得可轮询状态。
- 多个客户端、多个 Server 实例同时请求同一基金时，只创建一个有效刷新任务。
- 阶段业绩的基金收益、同类平均和排名来自同一上游快照并可审计查询。
- 查询接口保持无副作用；失败、空结果和进程崩溃不会形成永久锁死或请求风暴。

### Non-Goals

- 不由客户端直接调用 Agent。
- 不把五秒页面等待时间当作后台任务超时时间。
- 不为公开市场数据引入用户维度，也不改变资产、持仓或权限事实。
- 不在本次变更中实现定时全量刷新。

## Decisions

### 1. 使用独立显式补全 API

新增 `POST /api/funds/{fundCode}/detail-data/request-refresh`。该接口固定检查 `nav_history` 与 `period_performance`，只为确实缺失且当前可领取的 slice 创建任务。`GET /api/funds/{fundCode}/nav-history` 和新的阶段业绩 GET 继续只读，因此页面读取、缓存预取和健康检查不会意外触发外部采集。

响应返回整体 `ready/refreshing/unavailable`、每个 slice 的 `available/refreshing/empty/failed` 状态和 `retryAfterMs=1000`。客户端可以据此串行轮询只读接口，但后台任务生命周期不受客户端五秒等待上限约束。

### 2. 持久化 slice 状态是跨实例并发闸门

新增 `market_detail_slice_state`，以 `(fund_code, slice_type)` 唯一，记录 `status`、`active_task_id`、`last_attempt_at`、`last_success_at` 和安全错误摘要。Case 在一个事务中创建或锁定两个 slice 状态行，检查实际事实数据、已有 processing task、冷却窗口，再为本次可领取的缺失 slice 创建单个 `market_detail_data_refresh` task，并把对应状态标记为 `refreshing`。

行锁和唯一约束负责跨 Server 实例合并；Agent 进程内去重只作为第二层保护。派发在事务提交后执行，派发失败时把 task 和仍指向它的 slice 状态更新为失败。`refreshing` 超过可配置任务超时后会在下一次显式补全请求中收敛，默认十分钟；`empty/failed` 默认十分钟冷却后才允许重新采集。状态与真实表冲突时，已存在的事实数据优先判定为 `available`。

### 3. 阶段业绩使用规范化同源快照

新增 `fund_period_performance`，以 `(fund_code, period)` 唯一，保存：

- `fund_return`
- `peer_average`
- `peer_rank`
- `peer_total`
- `rank_change`
- `as_of`

支持固定期间 `1m/3m/6m/1y/3y`。同一次 callback 的数据来自同一上游响应；只有完整、合法的期间行才会 upsert。较旧 `as_of` 的回调不得覆盖较新快照。

新增 `GET /api/funds/{fundCode}/period-performance`，只查询该基金最新 `as_of` 的单一快照并按固定期间顺序返回可用行；旧快照中未被新快照覆盖的残留期间不得混入响应。尚无数据时返回 HTTP 200 和空 rows，便于客户端使用统一缺失语义。

### 4. callback 按 slice 独立收敛状态

扩展 callback payload 接收可选 `fund_period_performance`。每个请求过的 slice 在独立事务中完成校验、upsert 和 slice 状态更新：

- 有合法数据：`available`；
- 上游可信空结果：`empty`；
- slice 校验或持久化失败：`failed`；
- 其他 slice 成功不被回滚。

重复 callback 继续依赖 task 与幂等键，不重复数据或推进状态。只有 `active_task_id` 仍匹配 callback task 的状态行才被更新，避免迟到回调覆盖更新任务的状态。

### 5. 领域边界与数据安全

任务创建、状态领取和结果收敛位于 Case/Domain；MyBatis DAO/XML、数据库锁和 Agent HTTP 调用位于 Infrastructure。新增数据只包含公开基金代码和规范化行情字段，不保存 Agent 凭据、完整原始 payload、账户或持仓事实。日志只记录 task id、slice、脱敏异常类型和安全摘要。

## Data Flow

1. Client 先调用两个只读 GET。
2. 任一数据缺失时，Client 调用显式补全 POST。
3. Server 在事务中锁定 slice 状态并只创建一个缺失 slice 任务。
4. Server 提交后通过既有 Agent Port 派发；并发请求读到 `refreshing` 并复用同一任务。
5. Agent 回调后，Server 分 slice 保存事实与状态。
6. Client 在五秒内每秒串行读取只读 GET；超时仅停止页面等待，不取消后台任务。

## Migration and Rollback

- 以新的可重复执行 SQL 迁移创建两张表及索引，不修改既有事实表语义。
- 发布顺序为 Agent -> Server 数据库与 API -> Client，保证新 slice 被 Agent 识别后 Server 才会派发。
- 回滚 Client 后只会停止显式补全请求；Server 与新表可继续保留。
- 回滚 Server 代码前停止新请求，新增表保留不影响旧版本读取。

## Test Strategy

- Domain/Case：首个请求领取、并发请求合并、已有数据跳过、冷却与超时回收、派发失败。
- Callback：阶段业绩 upsert、旧快照保护、空/失败状态、部分成功、重复 callback。
- Trigger：显式参数名、HTTP 状态和响应结构。
- Repository：唯一约束、行锁查询、固定期间排序和长期净值容量。
- 回归：原有基金净值、股票价格和公司资料契约不变。
