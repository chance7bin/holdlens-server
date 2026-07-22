## 1. 契约与决策

- [x] 1.1 对照根目录客户端契约实现统一 `assetRef` 生成/解析，并验证示例字段一致。
- [x] 1.2 确认 ADR-004 已表达 API 引用、旧请求兼容和不迁移 `asset_info` 的决策。

## 2. Domain 与持久化查询

- [x] 2.1 新增无框架依赖的市场资产引用值对象及单元测试，覆盖基金、A 股、美股、非法格式和 kind 冲突。
- [x] 2.2 扩展 Portfolio Repository/DAO/XML，按用户查询启用自选并批量判断自选状态。
- [x] 2.3 扩展 Fund Repository/DAO/XML，支持代码、名称、拼音的受限搜索和当前数据批量查询。
- [x] 2.4 扩展 Stock Repository/DAO/XML，支持代码/名称搜索及精确当前详情查询。

## 3. Case 用例

- [x] 3.1 实现自选列表聚合用例，覆盖 kind 筛选、计数、默认顺序、公开字段缺失和用户隔离。
- [x] 3.2 实现统一搜索用例，覆盖筛选、limit、空结果、watchlisted 和无写入副作用。
- [x] 3.3 扩展批量加入用例支持 assetRef，保留旧字段兼容并覆盖幂等、冲突和无刷新副作用。
- [x] 3.4 实现股票当前详情用例，覆盖 null/0、行情时间、自选状态和不返回交易状态。

## 4. API 与 Trigger

- [x] 4.1 新增自选列表、统一搜索和股票详情 API DTO、服务接口及 Controller 映射。
- [x] 4.2 扩展批量加入请求/无效项响应 DTO，保持旧字段兼容。
- [x] 4.3 增加 Trigger/API 测试，校验路径、参数、响应字段和错误语义。

## 5. 验证与审查

- [x] 5.1 运行相关 Domain/Case/Infrastructure/Trigger 测试和 Maven 编译，记录真实结果。
- [x] 5.2 检查用户隔离、参数化 SQL、输入上限、日志脱敏及不触发刷新任务。
- [x] 5.3 运行 `openspec validate --strict add-client-market-asset-apis`。
- [x] 5.4 根据实际完成和验证结果同步任务状态，不提前勾选。

## 6. 联调缺陷修复

- [x] 6.1 为自选、搜索和股票详情 Controller 的查询参数显式声明协议字段名，并增加反射回归测试。
- [x] 6.2 使用标准 Maven 产物复测三个 GET 接口，确认不依赖 `-parameters`。
