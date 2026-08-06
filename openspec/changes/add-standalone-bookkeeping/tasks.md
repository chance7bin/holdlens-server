# 实施任务

## 1. 领域与用例

- [x] 1.1 新增收支条目实体、类型/状态值对象和固定分类目录；验证：金额、备注、分类匹配、状态迁移和分类排序 Domain 测试。
- [x] 1.2 新增记账 Repository 端口、Case 接口与实现，覆盖幂等创建、详情、日期范围明细、修订和软删除；验证：幂等、用户隔离、活动状态和非法输入 Case 测试。
- [x] 1.3 实现自然周/月/年周期、零值补点、当前周期平均值、分类占比、月账单和年账单编排；验证：跨月、跨年、闰年、当前周期、空数据和删除后统计测试。

## 2. 基础设施与数据库

- [x] 2.1 新增 `bookkeeping_entry` PO、DAO、Repository 适配器和 Mapper XML，所有 SQL 参数化并强制用户/状态过滤；验证：Repository 测试与 SQL 结构测试。
- [x] 2.2 更新数据库初始化脚本并新增独立迁移，包含幂等唯一键和明细/统计索引；验证：迁移结构与初始化脚本一致。

## 3. API 与契约

- [x] 3.1 新增记账 API 请求/响应 DTO、服务接口和 Controller，查询只用 GET，创建/修订/删除使用契约动作 POST；验证：HTTP 映射和 DTO 映射测试。
- [x] 3.2 对齐 `contracts/holdlens-server/client/bookkeeping.md` 的字段、枚举、日期、金额、错误、幂等和兼容语义；验证：契约映射测试覆盖全部端点。

## 4. 质量门与安全

- [x] 4.1 运行 Domain、Case、Infrastructure、Trigger 相关测试并确认目标测试实际执行。
- [x] 4.2 使用 JDK 17 串行运行后端 Maven 聚合测试或编译，避免并发污染 `target`。
- [x] 4.3 检查用户隔离、参数化 SQL、创建幂等、事务边界，以及日志/错误不泄露金额、备注和用户关系。
- [x] 4.4 同步任务状态并运行 `openspec validate --strict add-standalone-bookkeeping`。
