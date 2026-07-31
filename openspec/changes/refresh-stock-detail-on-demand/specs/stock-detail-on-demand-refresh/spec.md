## ADDED Requirements

### Requirement: 确保股票详情数据

Server SHALL 提供 `POST /api/stocks/detail-data/ensure`，请求体只接受一个已存在股票的 `assetRef`。已有价格历史和公司资料时 SHALL 直接返回 ready；缺失时 SHALL 创建或复用市场详情任务，并只请求缺失 slice。价格历史任务 SHALL 固定请求 `5d/1m/1y`，MUST NOT 请求分时。

#### Scenario: 详情数据已经完整

- **WHEN** 股票已有 day bar 且已有可信公司资料状态
- **THEN** Server SHALL 返回 `status=ready`、`serverTaskId=null`
- **AND** MUST NOT 创建或派发任务

#### Scenario: 首次访问缺少详情数据

- **WHEN** 股票缺少价格历史或公司资料且没有有效活动任务
- **THEN** Server SHALL 创建一个资产级刷新任务并返回 `status=refreshing`
- **AND** 价格历史 SHALL 请求 `5d/1m/1y`
- **AND** A 股 SHALL 向 Agent 下发业务 exchange code

#### Scenario: 可信空在冷却期内

- **WHEN** 某 slice 最近一次成功结果明确为空且仍在刷新冷却期内
- **THEN** Server SHALL 返回该 slice 为 `empty`
- **AND** MUST NOT 立即重复派发同一 slice

### Requirement: 查询股票详情刷新状态

Server SHALL 提供无副作用的 `GET /api/stocks/detail-data/tasks/{serverTaskId}`，返回任务对应 assetRef、整体状态、建议轮询间隔和 `price_history/company_profile` 的独立状态。

#### Scenario: 查询进行中任务

- **WHEN** task 为非终态且租约有效
- **THEN** Server SHALL 返回 `status=refreshing`、原 `serverTaskId` 和 `retryAfterMs`
- **AND** GET MUST NOT 创建、重试或更新任务

#### Scenario: 查询终态任务

- **WHEN** task 已完成
- **THEN** Server SHALL 使用持久化 slice 状态返回 `ready/empty/partial_failed/failed`
- **AND** 每个 slice SHALL 返回 `available/empty/failed` 中的对应状态

#### Scenario: 查询未知或非股票详情任务

- **WHEN** task id 不存在或不属于股票详情按需刷新
- **THEN** Server SHALL 拒绝请求
- **AND** MUST NOT 暴露其他任务参数或诊断。

### Requirement: 合并并发股票详情刷新

Server SHALL 以 MySQL 唯一活动键和租约实现同一 assetRef 的跨线程、跨实例 single-flight，MUST NOT 依赖进程内锁或 Redis 锁保证正确性。

#### Scenario: 同资产并发 ensure

- **WHEN** 多个请求并发确保同一股票详情
- **THEN** 最多 SHALL 有一个非终态任务持有该资产活动键
- **AND** 所有请求 SHALL 返回同一个活动 task id 或已就绪结果
- **AND** Agent MUST NOT 因这些并发请求被重复派发。

#### Scenario: 活动任务租约过期

- **WHEN** ensure 发现活动任务租约已过期且任务仍为非终态
- **THEN** Server SHALL 条件标记旧任务失败并释放活动键
- **AND** SHALL 允许一个新任务竞争取得该键。

#### Scenario: 终态释放活动键

- **WHEN** 任务进入 succeeded、partial_failed、failed、dispatch_failed 或 callback_failed
- **THEN** Server SHALL 原子清空活动键和租约
- **AND** 后续 ensure SHALL 能创建新任务。

### Requirement: 防止旧任务覆盖新状态

Server SHALL 使用条件任务状态推进和 slice active task 匹配保护 callback 与派发竞态；旧任务或重复 callback MUST NOT 覆盖更新任务的状态与市场详情事实。

#### Scenario: callback 先于派发响应完成

- **WHEN** callback 已把任务推进为终态后派发调用才返回 accepted
- **THEN** Server MUST NOT 把终态降级回 dispatched。

#### Scenario: 迟到 callback 对应旧活动任务

- **WHEN** callback 的 task id 已不再是股票 slice 的 active task
- **THEN** Server SHALL 保持幂等响应
- **AND** MUST NOT 更新该 slice 状态或覆盖当前股票详情事实。
