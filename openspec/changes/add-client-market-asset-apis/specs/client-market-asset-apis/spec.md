## ADDED Requirements

### Requirement: 服务端签发和校验市场资产引用

系统 SHALL 为基金和股票生成稳定 `assetRef`，并 SHALL 在接收引用时校验格式、资产类型和市场语义。客户端 MUST NOT 需要提交公开资产名称或自行推导股票市场。

#### Scenario: 生成基金和股票引用

- **WHEN** 系统向客户端返回基金或股票
- **THEN** 响应 SHALL 包含对应 `assetKind` 和 `assetRef`
- **AND** 同一公开资产在自选、搜索和详情接口 SHALL 使用相同引用

#### Scenario: 引用与类型不一致

- **WHEN** 调用方提交的 `assetKind` 与 `assetRef` 表达的资产类型不一致
- **THEN** 系统 SHALL 在写入或公开数据查询前拒绝该输入

### Requirement: 查询用户自选资产

系统 SHALL 提供 `GET /api/watchlist/assets`，按用户隔离查询启用的自选关系，并聚合基金或股票当前公开数据。

#### Scenario: 查询基金和股票自选

- **WHEN** 调用方使用有效 `userId` 查询全部自选
- **THEN** 系统 SHALL 返回基金和股票计数及资产列表
- **AND** 每项 SHALL 包含 `assetKind`、`assetRef`、代码、名称和允许为空的当前值
- **AND** 结果 SHALL 按自选关系默认顺序返回

#### Scenario: 用户没有自选

- **WHEN** 指定用户没有启用的自选资产
- **THEN** 系统 SHALL 返回 HTTP 200、计数 0 和空数组

#### Scenario: 用户隔离

- **WHEN** 查询某一 `userId` 的自选
- **THEN** 系统 MUST NOT 返回其他用户的 `asset_info`

### Requirement: 统一搜索既有市场资产

系统 SHALL 提供 `GET /api/assets/search`，按代码、名称和基金拼音搜索已存在的基金、A 股及美股，并返回当前用户的自选状态。

#### Scenario: 搜索基金和股票

- **WHEN** 调用方提交合法关键字和筛选条件
- **THEN** 系统 SHALL 返回至多 `limit` 条匹配资产
- **AND** 每项 SHALL 包含 `assetKind`、`assetRef`、行情摘要和 `watchlisted`

#### Scenario: 搜索无结果

- **WHEN** 没有公开资产匹配关键字
- **THEN** 系统 SHALL 返回 HTTP 200 和空数组

#### Scenario: 搜索不得产生副作用

- **WHEN** 任意搜索请求完成
- **THEN** 系统 MUST NOT 写入公开资产、自选、持仓或 processing task

### Requirement: assetRef 批量加入保持旧调用兼容

系统 SHALL 允许新客户端使用 `assetKind + assetRef` 批量加入自选，并 SHALL 在没有 `assetRef` 时继续处理旧 `assetCode/market` 请求。批量加入 SHALL 保持幂等且只返回无效项。

#### Scenario: 使用 assetRef 加入自选

- **WHEN** 请求项包含有效且已存在公开资产的 `assetKind + assetRef`
- **THEN** 系统 SHALL 创建或保持对应 `asset_info`
- **AND** 响应的 `invalidItems` MUST NOT 包含该项

#### Scenario: 旧请求继续有效

- **WHEN** 请求项没有 `assetRef` 但包含现有接口支持的合法旧字段
- **THEN** 系统 SHALL 按旧业务键校验并幂等加入自选

#### Scenario: assetRef 优先且名称不可信

- **WHEN** 同一项同时包含 `assetRef` 和不一致的兼容字段
- **THEN** 系统 SHALL 以 `assetRef` 解析结果为准并校验冲突
- **AND** 系统 MUST NOT 把客户端 `assetName` 当作公开名称事实

### Requirement: 查询股票当前详情

系统 SHALL 提供 `GET /api/stocks/detail`，根据股票 `assetRef` 返回 `stock_market` 当前事实和用户自选状态。

#### Scenario: 股票部分字段缺失

- **WHEN** 股票存在但部分行情字段没有数据
- **THEN** 系统 SHALL 返回 HTTP 200
- **AND** 缺失字段 SHALL 为 `null`
- **AND** 合法数值 0 MUST 保持为 0

#### Scenario: 不返回交易状态或固定延迟

- **WHEN** 股票详情查询成功
- **THEN** 响应 MUST NOT 包含由 `stock_market.status` 推导的交易状态
- **AND** 延迟提示 SHALL 不承诺固定分钟数

### Requirement: 查询与加入不触发公开数据刷新

自选列表、统一搜索、股票详情和批量加入 SHALL 只读取或维护既有事实，MUST NOT 创建 agent 刷新任务或修改基金、股票公开数据。

#### Scenario: 完成首期客户端市场资产操作

- **WHEN** 调用方执行任一上述接口
- **THEN** 系统 MUST NOT 创建 `processing_task`
- **AND** 系统 MUST NOT upsert `fund` 或 `stock_market`
- **AND** 批量加入之外的接口 MUST NOT 写入 `asset_info`

### Requirement: HTTP 参数绑定不依赖编译器参数名

系统 SHALL 为公开查询接口的 `@RequestParam` 和 `@PathVariable` 显式声明协议字段名，不得依赖 Java 编译产物是否保留方法参数名。

#### Scenario: 以标准 Maven 产物调用查询接口

- **WHEN** 应用使用未启用 `-parameters` 的标准 Maven 产物运行
- **THEN** 自选、搜索和股票详情接口 SHALL 正确绑定契约中的查询参数
- **AND** 系统 MUST NOT 因缺少 `MethodParameters` 元数据返回非法参数
