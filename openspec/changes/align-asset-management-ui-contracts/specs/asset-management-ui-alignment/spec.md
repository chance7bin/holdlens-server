## ADDED Requirements

### Requirement: 系统默认目录允许用户按需细化

系统 SHALL 全局提供 `user_id = null` 的默认系统目录，并 SHALL 允许用户在任意可见、启用的一级系统目录下创建带所属 `userId` 的私有二级目录。系统目录本身仍 SHALL 不可由用户修改或删除。

#### Scenario: 在银行卡下创建用户银行目录

- **WHEN** 用户在系统目录 `BANK_CARD` 下创建“招商银行”
- **THEN** 系统 SHALL 创建带该用户 `userId` 的二级用户目录
- **AND** 其他用户 MUST NOT 查询或使用该目录

#### Scenario: 父目录已有直接记录

- **WHEN** 当前用户在父目录下已有活跃直接资产记录并尝试创建子目录
- **THEN** 系统 SHALL 拒绝创建
- **AND** 原有目录和记录 SHALL 保持不变

### Requirement: 银行卡系统目录名称统一

系统 SHALL 使用稳定编码 `BANK_CARD` 表达银行卡系统目录，并 SHALL 向客户端返回名称“银行卡”。

#### Scenario: 查询系统目录

- **WHEN** 客户端查询可见资产目录
- **THEN** `catalogCode=BANK_CARD` 的目录名称 SHALL 为“银行卡”

### Requirement: 资产记录返回类型专属关联标识

系统 SHALL 在已关联公共标的的资产记录中按 `assetKind` 返回对应关联信息，客户端 MUST NOT 解析 `assetRef`。

#### Scenario: 返回关联基金记录

- **WHEN** 记录关联公共基金
- **THEN** 响应 SHALL 填充 `fund.assetCode`
- **AND** `stock` SHALL 为空

#### Scenario: 返回关联股票记录

- **WHEN** 记录关联公共股票
- **THEN** 响应 SHALL 填充 `stock.assetCode`、`stock.assetMarket` 和 `stock.assetMarketLabel`
- **AND** `fund` SHALL 为空

#### Scenario: 返回未关联基金或股票记录

- **WHEN** 记录具有 `assetKind` 但 `assetRef` 为空
- **THEN** `fund` 和 `stock` SHALL 均为空

### Requirement: 使用统一引用查询公共市场详情

系统 SHALL 提供 `GET /api/market-assets/detail`，根据 `assetKind + assetRef` 查询基金或股票公共详情，并 SHALL 与用户资产记录解耦。

#### Scenario: 查询基金详情

- **WHEN** 客户端提交有效基金 `assetKind + assetRef`
- **THEN** 系统 SHALL 返回基金详情并只填充 `fund`
- **AND** 查询 MUST NOT 依赖或读取该用户的资产记录

#### Scenario: 查询股票详情

- **WHEN** 客户端提交有效股票 `assetKind + assetRef`
- **THEN** 系统 SHALL 返回股票详情并只填充 `stock`

#### Scenario: 引用类型冲突

- **WHEN** `assetKind` 与 `assetRef` 表达的类型不一致
- **THEN** 系统 SHALL 在公共数据查询前拒绝请求

### Requirement: 首期不提供基金风险等级

系统和客户端 SHALL 不展示或推导没有可信数据来源的基金风险等级。

#### Scenario: 展示基金详情

- **WHEN** 客户端展示首期基金详情
- **THEN** 页面 SHALL 不展示风险等级行
- **AND** 服务端响应 SHALL 不通过基金类型推导风险等级
