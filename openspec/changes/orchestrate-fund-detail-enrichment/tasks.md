## 1. 数据模型与领域接口

- [x] 1.1 新增基金阶段业绩和基金详情 slice 状态数据库迁移、索引与可重复执行校验
- [x] 1.2 新增阶段业绩、slice 状态的 Domain 模型和仓储接口
- [x] 1.3 在 Infrastructure 实现批量 upsert、固定期间查询、状态行锁与条件更新

## 2. 补全任务编排

- [x] 2.1 扩展 `market_detail_data_refresh` 基金 slice 校验，支持 `nav_history`、`period_performance` 任意非空组合
- [x] 2.2 在 Case 层实现缺失检测、事务领取、并发合并、冷却和超时恢复
- [x] 2.3 新增显式基金详情补全 POST Trigger，并保持既有 GET 无副作用
- [x] 2.4 接入提交后派发及派发失败状态收敛

## 3. callback 与查询

- [x] 3.1 扩展 Agent dispatch/callback DTO 和适配器，支持基金阶段业绩
- [x] 3.2 按 slice 独立事务保存阶段业绩并更新 `available/empty/failed` 状态
- [x] 3.3 新增阶段业绩只读 GET，并实现旧快照不覆盖新快照

## 4. 验证

- [x] 4.1 增加并发合并、冷却、超时和派发失败的 Case 测试
- [x] 4.2 增加 callback 幂等、部分成功、阶段业绩和状态收敛测试
- [x] 4.3 增加 Trigger、仓储和数据库迁移测试
- [x] 4.4 运行相关 Maven 测试并严格验证 OpenSpec change
