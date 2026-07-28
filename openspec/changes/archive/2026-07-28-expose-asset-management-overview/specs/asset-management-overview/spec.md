## ADDED Requirements

### Requirement: 资产管理总览由服务端统一换算

系统 SHALL 提供资产管理总览查询，一次返回指定目标币种的全局汇总、当前用户可见目录和当前用户活跃资产记录。系统 SHALL 作为汇率选择与换算事实源，客户端 MUST NOT 为总览自行推导汇率。

#### Scenario: 默认按人民币查询总览

- **WHEN** 客户端只提交合法 `userId` 而未提交 `targetCurrency`
- **THEN** 系统 SHALL 使用 `CNY` 作为目标币种
- **AND** 响应 SHALL 同时包含全局汇总、目录列表和活跃记录列表

#### Scenario: 非活跃记录不进入总览

- **WHEN** 用户存在 `ARCHIVED` 或 `DELETED` 资产记录
- **THEN** 系统 MUST NOT 在总览记录列表、目录小计或全局汇总中包含这些记录

### Requirement: 目录返回递归目标币种小计

系统 SHALL 为每个可见目录返回其自身及全部后代目录中活跃记录的目标币种非负规模小计。一级目录 SHALL 递归包含二级目录，叶子目录 SHALL 包含直接记录；目录金额方向 MUST NOT 把目录展示小计变为负数。

#### Scenario: 一级目录汇总二级目录记录

- **WHEN** 一级目录有两个二级目录且其可换算活跃记录分别为 100 CNY 和 200 CNY
- **THEN** 一级目录 `convertedAmount` SHALL 为 300
- **AND** 两个二级目录 `convertedAmount` SHALL 分别为 100 和 200

#### Scenario: 负债目录保持非负规模

- **WHEN** `SUBTRACT` 目录内的可换算活跃记录为 300 CNY
- **THEN** 该目录 `convertedAmount` SHALL 为 300
- **AND** 全局汇总 SHALL 将 300 计入 `liabilityTotal` 并从 `netAsset` 扣减

### Requirement: 汇率缺失在记录与目录层级准确表达

系统 SHALL 在记录缺少必要汇率时返回空 `convertedAmount` 和 `MISSING_RATE`，并 SHALL 将该记录影响传播到自身目录及全部祖先目录。系统 MUST NOT 把无法换算的记录作为零金额计入已换算小计。

#### Scenario: 叶子目录部分记录缺汇率

- **WHEN** 叶子目录包含 100 CNY 和缺少汇率的 20 EUR 两条活跃记录且目标币种为 CNY
- **THEN** CNY 记录 `conversionStatus` SHALL 为 `CONVERTED` 且 `convertedAmount` 为 100
- **AND** EUR 记录 `conversionStatus` SHALL 为 `MISSING_RATE` 且 `convertedAmount` SHALL 为空
- **AND** 叶子及其祖先目录 `convertedAmount` SHALL 为已确认的 100
- **AND** 叶子及其祖先目录 `partial` SHALL 为真且 `missingCurrencies` SHALL 包含 `EUR`

### Requirement: 资产记录可按不透明引用过滤

系统 SHALL 允许资产记录列表按可选 `assetRef` 精确过滤。`assetRef` SHALL 对客户端保持不透明，查询结果 SHALL 同时受 `userId` 和 `ACTIVE` 状态限制；未传 `assetRef` 时 SHALL 保持原有活跃记录列表行为。

#### Scenario: 查询已登记的公共标的记录

- **WHEN** 用户提交服务端签发的 `assetRef`
- **THEN** 系统 SHALL 只返回该用户关联此引用的活跃资产记录
- **AND** 系统 MUST NOT 返回其他用户、其他引用或非活跃记录

#### Scenario: 其他用户持有相同标的

- **WHEN** 两个用户都登记同一 `assetRef` 且其中一个用户查询
- **THEN** 响应 SHALL 只包含查询用户自己的记录

### Requirement: 单条记录详情只读取所属用户活跃记录

系统 SHALL 提供按 `recordId + userId` 查询单条资产记录详情的接口，并 SHALL 只返回所属用户的 `ACTIVE` 记录。

#### Scenario: 查询自己的活跃记录

- **WHEN** 用户提交自己的活跃 `recordId`
- **THEN** 系统 SHALL 返回完整记录响应，包括原币金额、类型专属关联信息、创建时间和更新时间

#### Scenario: 查询跨用户或非活跃记录

- **WHEN** 用户提交其他用户的记录 ID，或提交已归档、已删除记录 ID
- **THEN** 系统 SHALL 按资产记录不存在或不可见处理
- **AND** 错误 MUST NOT 泄露记录金额、名称、备注或所属关系
