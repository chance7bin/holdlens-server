## ADDED Requirements

### Requirement: 创建单资产市场详情刷新任务

系统 SHALL 提供受控任务创建入口，为一个已存在的基金或股票创建 `market_detail_data_refresh` 任务，并 SHALL 只允许与资产类型匹配的 slice 和 period。

#### Scenario: 创建股票详情任务

- **WHEN** 调用方提交已存在股票、`price_history/company_profile` 和合法 periods
- **THEN** 系统 SHALL 持久化 processing task 并向 agent 派发单资产任务
- **AND** 美股价格历史 dispatch SHALL 使用 `stock_market.provider_market_code`
- **AND** 接口 SHALL 返回 HTTP 202 和 server task id

#### Scenario: 拒绝无效引用或 slice

- **WHEN** 资产不存在、asset kind/ref 冲突或 slice 不适用于资产类型
- **THEN** 系统 SHALL 在创建和派发任务前拒绝请求

#### Scenario: 客户端查询不触发刷新

- **WHEN** 客户端查询基金历史、股票历史或公司资料
- **THEN** 系统 MUST NOT 自动创建 processing task

### Requirement: 幂等处理市场详情 callback

系统 SHALL 接收 `market-detail-data-refresh-result/v1` callback，校验任务身份、schema、引用、slice 和幂等键，并 SHALL 使重复 callback 不产生重复数据或状态推进。

#### Scenario: 重复 callback

- **WHEN** 同一任务以相同幂等键重复回调
- **THEN** 系统 SHALL 返回幂等成功结果
- **AND** 历史点、bar 和公司资料 MUST NOT 重复

#### Scenario: 未知或不匹配任务

- **WHEN** callback 使用未知任务、错误任务类型、错误 assetRef 或错误幂等键
- **THEN** 系统 SHALL 拒绝写入详情表

#### Scenario: 未请求 slice 的空数组兼容

- **WHEN** callback 对未请求的集合 slice 携带空数组且没有任何数据项
- **THEN** 系统 SHALL 按未携带该 slice 数据处理
- **AND** 系统 MUST NOT 因空数组拒绝整个 callback

### Requirement: 独立保存合法 slice

系统 SHALL 对基金净值、股票价格和公司资料分别校验并批量 upsert。部分失败时，合法 slice SHALL 被保存，失败 slice MUST NOT 产生伪造或部分脏数据。

系统 SHALL 接受最多 10000 点的完整基金净值历史，使超过 5000 个交易日记录的长期基金不因存续时间而被拒绝。

#### Scenario: 基金净值幂等 upsert

- **WHEN** callback 包含同一基金和日期的有效净值点
- **THEN** 系统 SHALL 按 `fund_code + nav_date` 创建或更新单条记录

#### Scenario: 股票 bar 幂等 upsert

- **WHEN** callback 包含同一股票、市场、粒度和时间的有效 bar
- **THEN** 系统 SHALL 创建或更新单条 bar
- **AND** 系统 SHALL 保留合法数值 0

#### Scenario: 部分成功

- **WHEN** 一个股票任务的价格历史成功而公司资料失败
- **THEN** 系统 SHALL 保存有效价格 bar
- **AND** 系统 SHALL 把 processing task 终态记录为部分失败语义
- **AND** 系统 MUST NOT 写入伪造公司资料

#### Scenario: slice 持久化失败诊断

- **WHEN** 任一合法 slice 在独立事务中保存失败
- **THEN** 系统 SHALL 记录 task id、slice、脱敏异常类型和安全错误摘要
- **AND** 日志 MUST NOT 包含 callback 鉴权值、凭据或完整原始 payload

### Requirement: 查询基金净值历史

系统 SHALL 提供 `GET /api/funds/{fundCode}/nav-history`，按 `1m/3m/1y/all` 返回升序净值点。

#### Scenario: 基金有历史数据

- **WHEN** 基金存在且请求区间内有净值记录
- **THEN** 系统 SHALL 返回区间、最新日期和升序 points

#### Scenario: 基金暂无历史数据

- **WHEN** 基金存在但没有净值记录
- **THEN** 系统 SHALL 返回 HTTP 200、`asOf=null` 和空数组

### Requirement: 查询股票价格历史

系统 SHALL 提供 `GET /api/stocks/price-history`，按 `intraday/5d/1m/1y` 返回响应声明粒度的升序 bar。

#### Scenario: 五日使用日线

- **WHEN** 调用方查询 `5d` 且持久化数据为 day bar
- **THEN** 响应 SHALL 返回 `granularity=day`
- **AND** 系统 MUST NOT 伪装为 minute 数据

#### Scenario: 股票历史为空

- **WHEN** 股票存在但窗口内没有 bar
- **THEN** 系统 SHALL 返回 HTTP 200 和空数组

### Requirement: 查询股票公司资料

系统 SHALL 提供 `GET /api/stocks/company-profile`，返回允许字段为空的最新资料。

#### Scenario: 公司资料不完整

- **WHEN** 股票存在且资料只有部分字段
- **THEN** 系统 SHALL 返回已有字段
- **AND** 缺失行业、摘要、简介或网站 SHALL 为 `null`

#### Scenario: 尚未采集公司资料

- **WHEN** 股票存在但没有 profile 记录
- **THEN** 系统 SHALL 返回 HTTP 200 和字段为空的资料对象

### Requirement: 市场详情 HTTP 参数绑定不依赖编译器参数名

系统 SHALL 为历史与公司资料查询接口的 `@RequestParam`、`@PathVariable` 显式声明协议字段名。

#### Scenario: 使用标准 Maven 产物查询市场详情

- **WHEN** 应用编译产物不包含 `MethodParameters` 元数据
- **THEN** 基金历史、股票历史和公司资料接口 SHALL 仍按契约字段正确绑定参数

### Requirement: 保护长期事实源和敏感信息

系统 SHALL 只保存规范化公开市场数据和脱敏任务诊断，MUST NOT 保存 agent 凭据、cookie、完整原始响应或用户资产事实。

#### Scenario: callback 包含诊断

- **WHEN** server 记录 warning 或 error summary
- **THEN** 记录 SHALL 不包含 callback token、HTTP 凭据、完整上游响应或个人资产数据
