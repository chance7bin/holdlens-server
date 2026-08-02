## 1. 契约与领域模型

- [x] 1.1 扩展 callback DTO/Command/Trigger 映射和基金聚合，携带 `provider_market_code`。
- [x] 1.2 在 Case 中实现业务市场软校验、未知市场 warning 和不拒绝持仓行语义。

## 2. 持久化与迁移

- [x] 2.1 扩展 PO、Repository 和 MyBatis XML，持久化并读取 `provider_market_code`。
- [x] 2.2 更新基线 SQL，新增一次性历史市场迁移脚本。

## 3. 验证

- [x] 3.1 补充 Case、Repository、Mapper/SQL 结构测试。
- [x] 3.2 运行 focused Maven tests、必要回归、OpenSpec strict 校验和差异检查。
- [x] 3.3 确认迁移脚本未自动应用到运行数据库，并记录部署顺序。
