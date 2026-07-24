## 1. 领域与用例

- [x] 1.1 扩展资产汇率领域服务，生成单条记录换算状态、目录递归小计和全局汇总；验证：换算、递归、负债规模和缺失汇率 Domain 测试。
- [x] 1.2 扩展资产管理 Case，编排目录、活跃记录、汇率和总览结果；验证：默认 CNY、只含 ACTIVE、用户隔离和缺失汇率 Case 测试。
- [x] 1.3 增加 `assetRef` 可选过滤和单条活跃记录查询；验证：未传兼容、精确过滤、跨用户与非活跃详情测试。

## 2. 基础设施与 API

- [x] 2.1 扩展 Repository、DAO 和 Mapper XML 查询，保持参数化 SQL 与 `user_id + ACTIVE` 限制；验证：Infrastructure SQL 结构测试。
- [x] 2.2 新增 overview API DTO、服务接口和轻量 Controller 映射；验证：响应字段、基金/股票互斥关联、不泄露技术 ID 和 HTTP 映射测试。
- [x] 2.3 更新根目录客户端契约，补充 overview、`assetRef` 过滤、详情和错误兼容语义。

## 3. 质量门与安全

- [x] 3.1 运行 Domain、Case、Infrastructure、Trigger 相关测试，确认目标测试实际执行。
- [x] 3.2 使用 JDK 17 串行运行后端 Maven 聚合测试/编译，并确认没有进程并发污染 `target`。
- [x] 3.3 检查日志、错误和 DTO 不泄露用户金额、名称、备注、关系或公共数据库技术 ID。
- [x] 3.4 同步任务状态并运行 `openspec validate --strict expose-asset-management-overview`。
