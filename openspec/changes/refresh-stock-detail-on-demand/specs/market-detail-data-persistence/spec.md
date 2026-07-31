## MODIFIED Requirements

### Requirement: 创建单资产市场详情刷新任务

系统 SHALL 提供受控任务创建入口，为一个已存在的基金或股票创建 `market_detail_data_refresh` 任务，并 SHALL 只允许与资产类型匹配的 slice 和 period。股票详情按需入口 SHALL 通过持久化活动键和 slice 状态创建或复用任务。

#### Scenario: 创建股票详情任务

- **WHEN** 调用方提交已存在股票、`price_history/company_profile` 和合法 periods
- **THEN** 系统 SHALL 持久化 processing task 并向 agent 派发单资产任务
- **AND** 美股价格历史 dispatch SHALL 使用 `stock_market.provider_market_code`
- **AND** A 股价格历史 dispatch SHALL 使用业务 `exchange_code`
- **AND** 接口 SHALL 返回 HTTP 202 和 server task id

#### Scenario: 拒绝无效引用或 slice

- **WHEN** 资产不存在、asset kind/ref 冲突或 slice 不适用于资产类型
- **THEN** 系统 SHALL 在创建和派发任务前拒绝请求

#### Scenario: 客户端查询不触发刷新

- **WHEN** 客户端查询基金历史、股票历史或公司资料
- **THEN** 系统 MUST NOT 自动创建 processing task

#### Scenario: 股票详情确保动作触发刷新

- **WHEN** 客户端显式 POST 股票详情确保动作且持久化状态表明 slice 未采集或允许重试
- **THEN** 系统 SHALL 创建或复用一个股票详情任务
- **AND** MUST NOT 通过 GET 查询产生副作用。

### Requirement: 独立保存合法 slice

系统 SHALL 对基金净值、股票价格和公司资料分别校验并批量 upsert。部分失败时，合法 slice SHALL 被保存，失败 slice MUST NOT 产生伪造或部分脏数据。股票详情按需任务 SHALL 仅在 callback task 仍匹配 slice active task 时保存事实并收敛状态。

系统 SHALL 接受最多 10000 点的完整基金净值历史，使超过 5000 个交易日记录的长期基金不因存续时间而被拒绝。

#### Scenario: 基金净值幂等 upsert

- **WHEN** callback 包含同一基金和日期的有效净值点
- **THEN** 系统 SHALL 按 `fund_code + nav_date` 创建或更新单条记录

#### Scenario: 股票 bar 幂等 upsert

- **WHEN** callback 包含同一股票、市场、粒度和时间的有效 bar
- **THEN** 系统 SHALL 创建或更新单条 bar
- **AND** 系统 SHALL 保留合法数值 0

#### Scenario: 股票可信空结果

- **WHEN** callback 为请求的完整价格 periods 返回空 bars 或公司资料字段全空
- **THEN** 系统 SHALL 把对应股票 slice 状态保存为 empty
- **AND** MUST NOT 伪造 bar 或公司字段。

#### Scenario: 部分成功

- **WHEN** 一个股票任务的价格历史成功而公司资料失败
- **THEN** 系统 SHALL 保存有效价格 bar
- **AND** 系统 SHALL 把 processing task 终态记录为部分失败语义
- **AND** 系统 MUST NOT 写入伪造公司资料

#### Scenario: 旧任务回调

- **WHEN** callback task 已不再匹配股票 slice 的 active task
- **THEN** 系统 MUST NOT 写入该 slice 事实或推进当前 slice 状态。

#### Scenario: slice 持久化失败诊断

- **WHEN** 任一合法 slice 在独立事务中保存失败
- **THEN** 系统 SHALL 记录 task id、slice、脱敏异常类型和安全错误摘要
- **AND** 日志 MUST NOT 包含 callback 鉴权值、凭据或完整原始 payload
