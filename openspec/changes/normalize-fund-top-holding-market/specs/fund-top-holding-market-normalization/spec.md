## ADDED Requirements

### Requirement: 软校验基金重仓业务市场

Server SHALL 把基金重仓 `market` 视为业务市场，只接受 `A_SHARE`、`US_STOCK` 或 null。未知值 SHALL 降级为空并产生安全诊断，MUST NOT 因此拒绝持仓行或整个 callback。

#### Scenario: 接受规范业务市场

- **WHEN** callback 持仓行的 `market` 为 `A_SHARE` 或 `US_STOCK`
- **THEN** Server SHALL 原样保存业务市场
- **AND** 详情查询 SHALL 可按 `stock_code + market` 关联股票行情事实

#### Scenario: 软降级未知市场

- **WHEN** callback 持仓行的 `market` 不是支持的业务市场
- **THEN** Server SHALL 保存该持仓行并把业务市场置空
- **AND** Server SHALL 记录 `unsupported_holding_market` warning
- **AND** Server SHALL 保持原本成功的任务为 `succeeded`
- **AND** callback MUST NOT 仅因该字段进入失败状态

### Requirement: 保存基金重仓供应商市场码

Server SHALL 在基金重仓当前快照中独立保存可选 `provider_market_code`，并 MUST NOT 用它参与股票业务身份关联或向 Client 基金详情 DTO 暴露。

#### Scenario: 保存双市场字段

- **WHEN** callback 行同时包含规范 `market` 和 `provider_market_code`
- **THEN** Repository SHALL 原子保存两个字段
- **AND** 同期快照内容比较 SHALL 包含两个字段

### Requirement: 一次性迁移历史市场值

Server SHALL 提供前向数据库 migration，保留历史供应商市场码并把已支持的原始码转换为业务市场，运行时代码 MUST NOT 提供原始码兼容映射。

#### Scenario: 迁移已支持的历史市场码

- **WHEN** 历史 `fund_top_holding.market` 为 `0/1/105/106/107`
- **THEN** migration SHALL 先把原值保存到 `provider_market_code`
- **AND** SHALL 分别把业务 `market` 转为 `A_SHARE` 或 `US_STOCK`

#### Scenario: 迁移暂不支持的历史市场码

- **WHEN** 历史市场码无法映射到当前业务市场
- **THEN** migration SHALL 保留原值到 `provider_market_code`
- **AND** SHALL 把业务 `market` 置空
