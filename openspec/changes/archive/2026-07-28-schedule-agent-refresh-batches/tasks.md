## 1. API 与用例契约

- [x] 1.1 新增股票手动刷新请求 DTO，表达非空 `stocks` 列表和每项 `stockCode + market`。
- [x] 1.2 调整 `IAgentFundRefreshService` 和 `AgentFundRefreshController.createStockQuoteTask`，使股票手动刷新入口接收请求体并映射到 Case 命令。
- [x] 1.3 新增或调整 Case 层股票刷新命令模型，支持手动传入股票列表并按 `stockCode + market` 去重。
- [x] 1.4 保留基金手动刷新接口现有请求契约和行为。

## 2. Case 层任务创建与并发检查

- [x] 2.1 调整 `IAgentFundRefreshCase` 和实现，使股票刷新既支持显式股票列表，也支持调度器传入分批目标。
- [x] 2.2 为基金和股票调度前置检查增加“同类型非终态任务是否存在”的用例能力。
- [x] 2.3 确保批次任务返回状态只将 `created`、`running`、`dispatched` 视为调度可继续状态，其余状态供调度器停止本轮。
- [x] 2.4 确保任务参数摘要只保存安全计数字段和触发上下文，不写入完整请求、账号资产、凭据或敏感信息。

## 3. Domain 与 Infrastructure 数据访问

- [x] 3.1 在基金数据仓储接口及实现中增加按 `id` keyset 分页查询有效 `fund_code` 目标的能力。
- [x] 3.2 在股票行情仓储接口及实现中增加按 `id` keyset 分页查询有效 `stock_code + market` 目标的能力，并排除空 market。
- [x] 3.3 在 DAO 接口和 Mapper XML 中实现分页 SQL，保持 SQL 位于 MyBatis XML，不使用注解 SQL。
- [x] 3.4 在处理任务仓储接口及实现中增加按任务类型查询非终态任务是否存在的能力。

## 4. 定时任务 Trigger

- [x] 4.1 新增基金详情刷新调度配置，包含 enabled、cron、batch-size，默认关闭。
- [x] 4.2 新增股票行情刷新调度配置，包含 enabled、cron、batch-size，默认关闭。
- [x] 4.3 在 trigger job 包中新增基金详情定时任务，启用时从 `fund_detail_item` 全表 keyset 分页创建批次任务。
- [x] 4.4 在 trigger job 包中新增股票行情定时任务，启用时从 `stock_market_current` 全表 keyset 分页创建批次任务。
- [x] 4.5 调度器在同类型非终态任务存在时跳过本轮，并记录安全日志。
- [x] 4.6 调度器在 Case 抛异常或返回异常状态时停止本轮，不回滚已创建批次。

## 5. 测试

- [x] 5.1 补充 Case 层测试：股票手动列表校验、去重、下发目标和异常状态返回。
- [x] 5.2 补充仓储/DAO 相关测试或等价验证：基金目标 keyset 分页、股票目标 keyset 分页、空 market 股票不进入调度目标。
- [x] 5.3 补充调度器测试：默认关闭不触发、同类型非终态任务跳过、全表分页创建多个批次、批次失败停止本轮。
- [x] 5.4 回归确认基金手动刷新接口仍可按基金代码列表创建任务。

## 6. 验证

- [x] 6.1 运行相关模块单元测试，至少覆盖 `holdlens-server-case`、`holdlens-server-infrastructure`、`holdlens-server-trigger` 中受影响测试。
- [x] 6.2 运行必要 Maven 构建或聚合测试，确认多模块编译通过。
- [x] 6.3 运行 `openspec validate --strict schedule-agent-refresh-batches`。
- [x] 6.4 确认 `tasks.md` 完成状态与实际实现和验证结果一致。
