## ADDED Requirements

### Requirement: HTTP 入口只使用 GET 和 POST

server SHALL 只使用 `GET` 承载无副作用查询，只使用 `POST` 承载创建、更新、删除及其他状态变更，并 SHALL NOT 暴露 `PUT`、`PATCH` 或 `DELETE` 映射。

#### Scenario: 查询业务数据

- **WHEN** 调用方读取资产、自选、行情、任务或内部查询数据
- **THEN** server SHALL 使用无副作用的 GET 接口

#### Scenario: 改变业务状态

- **WHEN** 调用方创建、更新、删除、移除、归档、恢复或写入数据
- **THEN** server SHALL 使用 POST 接口
- **AND** server SHALL 通过明确动作路径表达非创建类操作

#### Scenario: 检查遗留 HTTP 方法

- **WHEN** 检查所有服务端 Controller 映射和客户端请求适配
- **THEN** SHALL NOT 存在 PUT、PATCH 或 DELETE 请求方法

### Requirement: 资产管理写接口使用明确 POST 动作

server SHALL 通过 POST 动作接口更新或删除资产目录和资产记录，并 SHALL 保持原有字段校验、用户隔离、审计和业务错误语义。

#### Scenario: 更新资产目录

- **WHEN** 客户端向 `POST /api/asset-catalogs/{catalogId}/update-details` 提交目录更新 DTO
- **THEN** server SHALL 只更新 DTO 允许的目录字段

#### Scenario: 删除资产目录

- **WHEN** 客户端向 `POST /api/asset-catalogs/{catalogId}/delete` 提交包含 `userId` 的 JSON 请求体
- **THEN** server SHALL 按原有规则软删除允许删除的自有目录

#### Scenario: 更新资产记录详情或金额

- **WHEN** 客户端分别调用 `POST /api/asset-records/{recordId}/update-details` 或 `POST /api/asset-records/{recordId}/update-amount`
- **THEN** server SHALL 只执行对应局部更新
- **AND** 金额更新 SHALL 继续追加现有变更历史

### Requirement: 其他写入口统一使用 POST

server SHALL 使用 POST 动作接口移除自选关系和写入内部汇率。

#### Scenario: 移除自选关系

- **WHEN** 客户端向 `POST /api/watchlist/assets/remove` 提交 `userId + assetKind + assetRef`
- **THEN** server SHALL 只移除对应自选关系
- **AND** 重复移除 SHALL 保持幂等成功语义

#### Scenario: 写入汇率

- **WHEN** 内部调用方向 `POST /internal/exchange-rates/upsert` 提交汇率 DTO
- **THEN** server SHALL 按原有 upsert 规则写入汇率
