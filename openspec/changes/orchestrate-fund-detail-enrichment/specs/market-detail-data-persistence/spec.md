# market-detail-data-persistence Delta Specification

## MODIFIED Requirements

### Requirement: 创建单资产市场详情刷新任务

系统 SHALL 提供受控任务创建入口，为一个已存在的基金或股票创建 `market_detail_data_refresh` 任务，并 SHALL 只允许与资产类型匹配的 slice 和 period。基金任务 SHALL 接受 `nav_history`、`period_performance` 的任意非空组合。

#### Scenario: 创建股票详情任务

- **WHEN** 调用方提交已存在股票、`price_history/company_profile` 和合法 periods
- **THEN** 系统 SHALL 持久化 processing task 并向 agent 派发单资产任务
- **AND** 美股价格历史 dispatch SHALL 使用 `stock_market.provider_market_code`
- **AND** 接口 SHALL 返回 HTTP 202 和 server task id

#### Scenario: 创建基金组合 slice 任务

- **WHEN** 受控调用方提交已存在基金和 `nav_history`、`period_performance` 的任意非空组合
- **THEN** 系统 SHALL 创建一个包含所请求 slice 的 `market_detail_data_refresh` task
- **AND** 基金任务的 `periods` SHALL 为空
- **AND** `period_performance` SHALL 固定采集并返回 `1m/3m/6m/1y/3y`

#### Scenario: 拒绝无效引用或 slice

- **WHEN** 资产不存在、asset kind/ref 冲突、slice 不适用于资产类型或 periods 非法
- **THEN** 系统 SHALL 在创建和派发任务前拒绝请求

#### Scenario: 客户端只读查询不触发刷新

- **WHEN** 客户端查询基金净值、基金阶段业绩、股票历史或公司资料
- **THEN** 系统 MUST NOT 自动创建 processing task

### Requirement: 幂等处理市场详情 callback

系统 SHALL 接收 `market-detail-data-refresh-result/v1` callback，校验任务身份、schema、引用、slice 和幂等键，并 SHALL 使重复 callback 不产生重复数据或状态推进。基金 callback SHALL 可携带 `fund_nav_history` 和 `fund_period_performance` 的任意已请求组合。

#### Scenario: 重复 callback

- **WHEN** 同一任务以相同幂等键重复回调
- **THEN** 系统 SHALL 返回幂等成功结果
- **AND** 历史点、阶段业绩、bar 和公司资料 MUST NOT 重复

#### Scenario: 未知或不匹配任务

- **WHEN** callback 使用未知任务、错误任务类型、错误 assetRef 或错误幂等键
- **THEN** 系统 SHALL 拒绝写入详情表和 slice 状态表

#### Scenario: 未请求 slice 的空数组兼容

- **WHEN** callback 对未请求的集合 slice 携带空数组且没有任何数据项
- **THEN** 系统 SHALL 按未携带该 slice 数据处理
- **AND** 系统 MUST NOT 因空数组拒绝整个 callback

#### Scenario: 迟到 callback 不覆盖新任务状态

- **WHEN** callback 对应的 task id 不再是 slice 状态的 `activeTaskId`
- **THEN** 系统 MAY 幂等保存不旧于当前事实的数据
- **AND** 系统 MUST NOT 覆盖新任务的 slice 状态

### Requirement: 独立保存合法 slice

系统 SHALL 对基金净值、基金阶段业绩、股票价格和公司资料分别校验并批量 upsert。部分失败时，合法 slice SHALL 被保存，失败 slice MUST NOT 产生伪造或部分脏数据。

系统 SHALL 接受最多 10000 点的完整基金净值历史，使超过 5000 个交易日记录的长期基金不因存续时间而被拒绝。基金阶段业绩 SHALL 只接受 `1m/3m/6m/1y/3y`，并 MUST NOT 用较旧 `asOf` 覆盖较新快照。

#### Scenario: 基金净值幂等 upsert

- **WHEN** callback 包含同一基金和日期的有效净值点
- **THEN** 系统 SHALL 按 `fund_code + nav_date` 创建或更新单条记录

#### Scenario: 基金阶段业绩幂等 upsert

- **WHEN** callback 包含同一基金、同一来源快照和合法期间的阶段业绩
- **THEN** 系统 SHALL 按 `fund_code + period` 创建或更新单条记录
- **AND** 基金收益、同类平均、同类排名、样本总数和排名变化 SHALL 作为同一行保存

#### Scenario: 旧阶段业绩快照迟到

- **WHEN** 已保存记录的 `asOf` 晚于 callback 的 `asOf`
- **THEN** 系统 MUST NOT 用迟到 callback 覆盖该记录

#### Scenario: 股票 bar 幂等 upsert

- **WHEN** callback 包含同一股票、市场、粒度和时间的有效 bar
- **THEN** 系统 SHALL 创建或更新单条 bar
- **AND** 系统 SHALL 保留合法数值 0

#### Scenario: 部分成功

- **WHEN** 一个任务的任一 slice 成功而另一 slice 失败
- **THEN** 系统 SHALL 保存有效 slice
- **AND** 系统 SHALL 把 processing task 终态记录为部分失败语义
- **AND** 系统 MUST NOT 写入失败 slice 的伪造数据

#### Scenario: slice 持久化失败诊断

- **WHEN** 任一合法 slice 在独立事务中保存失败
- **THEN** 系统 SHALL 记录 task id、slice、脱敏异常类型和安全错误摘要
- **AND** 日志 MUST NOT 包含 callback 鉴权值、凭据或完整原始 payload

## ADDED Requirements

### Requirement: 查询基金阶段业绩

系统 SHALL 提供 `GET /api/funds/{fundCode}/period-performance`，按 `1m/3m/6m/1y/3y` 固定顺序返回阶段业绩行。

#### Scenario: 返回同源阶段业绩

- **WHEN** 基金存在且已保存阶段业绩
- **THEN** 系统 SHALL 返回每个可用期间的基金收益、同类平均、同类排名、样本总数、排名变化和快照日期
- **AND** 同一行的比较字段 SHALL 来自同一来源快照

#### Scenario: 尚无阶段业绩

- **WHEN** 基金存在但没有阶段业绩记录
- **THEN** 系统 SHALL 返回 HTTP 200、`asOf=null` 和空 rows

### Requirement: 显式编排基金详情补全

系统 SHALL 提供 `POST /api/funds/{fundCode}/detail-data/request-refresh`，固定检查 `nav_history` 与 `period_performance`，为当前可领取的缺失 slice 创建至多一个任务，并返回整体和逐 slice 状态。

#### Scenario: 数据全部可用

- **WHEN** 基金净值和阶段业绩都已有长期事实
- **THEN** 系统 SHALL 返回 `ready`
- **AND** 系统 MUST NOT 创建或派发任务

#### Scenario: 首个缺失请求领取任务

- **WHEN** 一个或多个 slice 缺失、没有有效刷新任务且不在冷却窗口
- **THEN** 系统 SHALL 在事务中锁定状态、创建一个包含所有可领取 slice 的 processing task 并标记为 `refreshing`
- **AND** 事务提交后系统 SHALL 派发该任务
- **AND** 响应 SHALL 返回 `refreshing` 和 `retryAfterMs=1000`

#### Scenario: 并发请求合并

- **WHEN** 多个 Server 实例并发请求同一基金的同一缺失 slice
- **THEN** 系统 SHALL 通过持久化行锁和唯一约束只创建一个有效刷新任务
- **AND** 其他请求 SHALL 返回同一 active task 的 `refreshing` 状态

#### Scenario: 空或失败结果处于冷却期

- **WHEN** slice 最近得到可信空结果或失败结果且仍在可配置冷却窗口内
- **THEN** 系统 SHALL 返回对应 `empty` 或 `failed` 状态
- **AND** 系统 MUST NOT 重复派发任务

#### Scenario: 后台任务超时可恢复

- **WHEN** slice 长时间处于 `refreshing` 且对应任务超过可配置超时
- **THEN** 系统 SHALL 将旧任务和仍引用它的 slice 状态收敛为失败
- **AND** 冷却期结束后新请求 SHALL 可重新领取

#### Scenario: 派发失败

- **WHEN** 事务已提交但 Agent 派发失败
- **THEN** 系统 SHALL 将 task 和仍引用它的 slice 状态收敛为失败
- **AND** 日志 SHALL 只包含安全诊断

### Requirement: 市场详情 slice 状态与事实一致

系统 SHALL 持久化基金详情 slice 状态，并 SHALL 在 callback 的独立 slice 事务中与事实写入一起收敛。

#### Scenario: callback 保存有效数据

- **WHEN** 请求过的 slice 成功保存至少一条合法事实
- **THEN** 对应状态 SHALL 更新为 `available` 并记录成功时间

#### Scenario: callback 返回可信空结果

- **WHEN** 请求过的 slice 明确成功但没有数据项
- **THEN** 对应状态 SHALL 更新为 `empty`
- **AND** 系统 MUST NOT 伪造事实数据

#### Scenario: 状态与事实冲突

- **WHEN** 状态记录为 `empty/failed/refreshing` 但对应长期事实已经存在
- **THEN** 查询和补全编排 SHALL 以事实存在为准并收敛为 `available`
