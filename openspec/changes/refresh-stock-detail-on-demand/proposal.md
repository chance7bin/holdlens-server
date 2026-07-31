## Why

股票详情查询在价格历史或公司资料尚未落库时只会返回空结果，客户端无法区分“未采集”和“可信空”。同时并发缺数请求可能重复创建并派发相同资产任务，需要由 Server 提供可查询、可复用的按需刷新任务。

## What Changes

- 新增股票详情数据确保入口：已有完整数据时直接返回就绪状态，缺数时创建或复用一个资产级刷新任务。
- 新增任务状态查询，明确 `processing/ready/empty/partial_failed/failed` 及各数据 slice 状态。
- 使用 MySQL 唯一活动键和租约实现跨实例 single-flight，不引入 Redis 互斥锁。
- 使用条件状态推进避免派发响应覆盖已完成 callback，并保护重试/迟到 callback 的任务状态。
- 价格历史按近一年一次补采，Server 向 Agent 请求 `5d/1m/1y` 和公司资料，不请求分时数据。

## Capabilities

### New Capabilities

- `stock-detail-on-demand-refresh`: 覆盖股票详情缺数判断、任务确保、状态查询和资产级并发合并。

### Modified Capabilities

- `market-detail-data-persistence`: 补充股票详情按需刷新与可信空状态的持久化语义。
- `processing-schema`: 为处理任务增加可释放的活动业务键和租约字段。

## Impact

- 影响市场详情 Case/Domain/Infrastructure/Trigger、Agent 调度 Port、MyBatis Mapper、处理任务表结构、HTTP 契约和测试。
- 不引入 Redis 依赖；GET 保持无副作用，任务确保使用 POST。
