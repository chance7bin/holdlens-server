## ADDED Requirements

### Requirement: 批量加入自选资产

系统 SHALL 提供批量加入自选资产能力，允许调用方通过 `POST /api/watchlist/assets/batch-add` 为指定用户提交基金或股票资产代码列表。成功处理的资产 SHALL 进入用户维度自选资产集合，但 MUST NOT 创建当前持仓。

#### Scenario: 批量加入基金和股票自选资产

- **WHEN** 调用方提交包含已存在基金和股票的合法批量添加请求
- **THEN** 系统 SHALL 为该用户创建或保持对应自选资产
- **AND** 系统 MUST NOT 写入 `asset_holding`
- **AND** 系统 MUST NOT 写入 `asset_holding_change`

#### Scenario: 已存在自选资产幂等成功

- **WHEN** 调用方重复提交该用户已经加入自选的资产
- **THEN** 系统 SHALL 将该输入项视为成功
- **AND** 响应的 `invalidItems` MUST NOT 包含该输入项

#### Scenario: 请求内重复资产幂等成功

- **WHEN** 同一批量添加请求中重复提交归一化后身份相同的资产
- **THEN** 系统 SHALL 将重复输入项视为成功
- **AND** 响应的 `invalidItems` MUST NOT 包含重复输入项

### Requirement: 批量添加响应只暴露无效项

系统 SHALL 在批量添加响应中只返回 `invalidItems`。未出现在 `invalidItems` 中的输入项 SHALL 表示处理后已经处于已加入自选状态。系统 MUST NOT 在响应中暴露新建数量、已存在数量、刷新任务标识、刷新调度状态或刷新失败摘要。

#### Scenario: 全部输入合法

- **WHEN** 调用方提交的所有输入项均合法
- **THEN** 响应 SHALL 包含空的 `invalidItems`
- **AND** 响应 MUST NOT 包含新建数量
- **AND** 响应 MUST NOT 包含已存在数量
- **AND** 响应 MUST NOT 包含刷新任务标识

#### Scenario: 部分输入无效

- **WHEN** 调用方提交的部分输入项无效
- **THEN** 响应 SHALL 在 `invalidItems` 中列出每个无效输入项
- **AND** 每个无效项 SHALL 包含 0 基请求数组下标 `index`
- **AND** 每个无效项 SHALL 包含原始 `assetKind`、`assetCode`、`market`、`reasonCode` 和 `reason`
- **AND** 未列入 `invalidItems` 的输入项 SHALL 处于已加入自选状态

### Requirement: 自选资产身份沿用现有 asset_info 唯一键

系统 SHALL 使用当前 `asset_info` 表结构支持的 `user_id + asset_code + asset_kind` 作为自选资产唯一身份。本变更 MUST NOT 调整 `asset_info` 表结构或唯一索引。`market` SHALL 允许为空，但不作为本变更中的自选资产唯一键组成部分。

#### Scenario: 同用户同代码同类型幂等

- **WHEN** 同一用户重复添加相同 `assetCode` 和 `assetKind` 的资产
- **THEN** 系统 SHALL 保持单条自选资产记录
- **AND** 重复添加 SHALL 幂等成功

#### Scenario: 同用户同代码同类型不同市场仍按现有唯一键幂等

- **WHEN** 同一用户添加相同 `assetCode` 和 `assetKind` 但 `market` 不同的资产
- **THEN** 系统 SHALL 按现有 `asset_info` 唯一键保持单条自选资产记录

#### Scenario: 空 market 幂等

- **WHEN** 同一用户多次添加相同 `assetCode`、`assetKind` 且 `market` 为空的资产
- **THEN** 系统 SHALL 只保留一条自选资产记录

### Requirement: 公开资产必须已存在

系统 SHALL 在写入自选资产前校验目标公开资产已经存在。基金 MUST 已存在于 `fund_detail_item`；股票 MUST 已存在于 `stock_market_current`，并按 `stock_code + market` 组合匹配。

#### Scenario: 基金不存在

- **WHEN** 调用方提交 `fund_detail_item` 中不存在的基金代码
- **THEN** 系统 MUST NOT 写入对应 `asset_info`
- **AND** 响应 SHALL 在 `invalidItems` 中包含该输入项
- **AND** 该无效项 `reasonCode` SHALL 表达基金不存在

#### Scenario: 股票不存在

- **WHEN** 调用方提交 `stock_market_current` 中不存在的 `stockCode + market` 组合
- **THEN** 系统 MUST NOT 写入对应 `asset_info`
- **AND** 响应 SHALL 在 `invalidItems` 中包含该输入项
- **AND** 该无效项 `reasonCode` SHALL 表达股票不存在

#### Scenario: 空 market 股票按空 market 匹配

- **WHEN** 调用方提交 `market` 为空的股票
- **THEN** 系统 SHALL 使用空 market 查询 `stock_market_current`
- **AND** 只有 `stock_market_current` 中存在同一 `stockCode` 且 market 为空的记录时，该输入项才 SHALL 视为有效

### Requirement: 批量添加不得修改公开数据或触发刷新

系统 SHALL 在批量加入自选时只写入或保持 `asset_info`。系统 MUST NOT 写入或更新 `fund_detail_item`，MUST NOT 写入或更新 `stock_market_current`，MUST NOT 创建或触发基金详情刷新任务，MUST NOT 创建或触发股票行情刷新任务。

#### Scenario: 添加基金不注册刷新目标

- **WHEN** 调用方添加已存在基金到自选
- **THEN** 系统 SHALL 写入或保持对应 `asset_info`
- **AND** 系统 MUST NOT 写入或更新 `fund_detail_item`
- **AND** 系统 MUST NOT 创建基金详情刷新任务

#### Scenario: 添加股票不注册刷新目标

- **WHEN** 调用方添加已存在股票到自选
- **THEN** 系统 SHALL 写入或保持对应 `asset_info`
- **AND** 系统 MUST NOT 写入或更新 `stock_market_current`
- **AND** 系统 MUST NOT 创建股票行情刷新任务

### Requirement: 资产名称不得代码占位

系统 SHALL 允许批量添加请求不提供资产名称。系统 MUST NOT 使用资产代码填充或占位资产名称。

#### Scenario: 未提供资产名称

- **WHEN** 调用方提交已存在资产代码但未提供资产名称
- **THEN** 系统 SHALL 创建或保持自选资产
- **AND** 系统 MUST NOT 将资产代码作为资产名称写入
