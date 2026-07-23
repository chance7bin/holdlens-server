## ADDED Requirements

### Requirement: 自选关系独立于资产记录

系统 SHALL 使用独立自选关系保存用户关注的公共基金和股票。自选关系 MUST NOT 关联资产目录、金额、币种或资产记录。

#### Scenario: 加入基金自选

- **WHEN** 用户加入一只已经存在的公共基金
- **THEN** 系统 SHALL 创建或保持 `user_id + FUND + fund.id` 自选关系
- **AND** 系统 MUST NOT 创建资产记录

#### Scenario: 创建持仓不自动自选

- **WHEN** 用户创建一条具体基金或股票持仓
- **THEN** 系统 MUST NOT 自动创建自选关系

#### Scenario: 移除自选不影响资产

- **WHEN** 用户移除已经存在的自选基金或股票
- **THEN** 系统 SHALL 删除对应自选关系
- **AND** 系统 MUST NOT 修改任何资产记录

### Requirement: 自选标的只引用既有公共资产

系统 SHALL 只允许 `FUND` 自选引用既有 `fund.id`，只允许 `STOCK` 自选引用既有 `stock_market.id`。同一用户的同一公共标的 SHALL 只有一条自选关系。

#### Scenario: 重复加入自选

- **WHEN** 用户重复加入同一公共基金或股票
- **THEN** 系统 SHALL 幂等保持单条关系

#### Scenario: 公共标的不存在

- **WHEN** 用户尝试加入不存在或类型不匹配的公共标的
- **THEN** 系统 SHALL 拒绝该输入

### Requirement: 外部资产引用保持不透明

系统 SHALL 继续向客户端签发和解析不透明 `assetRef`，并在服务端将其映射为 `asset_kind + asset_id`。客户端 MUST NOT 直接依赖数据库技术 ID。

#### Scenario: 客户端通过 assetRef 加入自选

- **WHEN** 客户端提交服务端签发的合法 `assetKind + assetRef`
- **THEN** 系统 SHALL 解析并校验对应公共标的
- **AND** 系统 SHALL 保存内部 `asset_kind + asset_id` 关系
