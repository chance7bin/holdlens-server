## Context

Server 已有纯查询的股票价格历史/公司资料接口和底层市场详情任务，但缺少面向 Client 的缺数确保与任务状态接口。`processing_task` 只有随机任务 ID，无法跨实例合并同一资产的并发任务；股票也没有持久化 slice 状态，因此查询空结果无法区分未采集、可信空和失败。

## Goals / Non-Goals

**Goals:**

- 提供 body 只含 `assetRef` 的股票详情确保动作和无副作用的任务状态查询。
- 以 MySQL 事实实现跨实例资产级 single-flight、租约恢复及条件状态推进。
- 持久化 `price_history/company_profile` 的 available/refreshing/empty/failed 状态。
- 只向 Agent 下发缺失 slice；价格历史固定请求 `5d/1m/1y`，并传递业务 exchange code。

**Non-Goals:**

- 不引入 Redis、消息队列或多来源降级策略。
- 不让 GET 查询自动创建任务。
- 不改变 Client 直接查询价格历史和公司资料的响应形状。

## Decisions

1. 新增 `POST /api/stocks/detail-data/ensure` 与 `GET /api/stocks/detail-data/tasks/{serverTaskId}`。POST 负责检查事实、创建/复用任务；GET 只读取任务与 slice 状态。
2. `processing_task` 增加可空 `active_key`、`lease_until`，对 `active_key` 建唯一索引。股票详情键为 `stock-detail:{assetRef}`；终态更新必须清空键和租约。插入冲突时读取已存在任务并返回同一 taskId，不使用 Redis 互斥锁。
3. 新增 `stock_detail_slice_state`，以 `asset_ref + slice_type` 唯一，保存状态、active task、最近尝试/成功和安全错误摘要。callback 仅能更新仍指向自身的状态行，过期任务不得覆盖当前状态或事实。
4. 创建与 claim 在事务内执行，Agent 派发在事务外执行。派发结果通过 `WHERE status IN (non-terminal)` 条件更新，避免 callback 已完成后被旧 `dispatched` 状态覆盖。
5. 租约默认沿用市场详情超时配置。新的 POST 遇到过期活动任务时条件标记失败并释放活动键，然后竞争创建新任务；GET 只按时间派生展示状态，不执行写入。
6. A 股派发使用 `stock_market.exchange_code`；历史存量为空时由 Server 按 A 股业务代码规则补出 `SH/SZ/BJ`，但 Provider symbol 格式仍由 Agent 适配器负责。

## Risks / Trade-offs

- [新增唯一索引和状态表需要数据库迁移] → 提供向前迁移脚本，字段均可空且不影响既有任务；回滚应用时新列/表可保留。
- [活动任务在进程崩溃后暂时占键] → 租约到期后由下一次 ensure 安全回收。
- [callback 与新任务竞态] → task 条件更新、slice active task 条件和事务共同阻止旧任务推进当前状态。
- [公开市场数据接口没有用户维度] → 入口只接受已存在的公开股票 assetRef，不接收账户或持仓字段，避免扩大个人数据边界。

## Migration Plan

1. 先执行新增列、唯一索引和 `stock_detail_slice_state` 的向前迁移。
2. 部署兼容新字段的 Server，再部署 Agent 和 Client。
3. 回滚时可回退应用代码；新增可空列与状态表不影响旧版本，待确认无活动任务后再由单独迁移清理。

## Open Questions

无。
