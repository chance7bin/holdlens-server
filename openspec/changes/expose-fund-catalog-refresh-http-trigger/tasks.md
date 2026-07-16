## 1. OpenSpec

- [x] 1.1 创建 proposal、design、spec 和 tasks，明确基金目录刷新 HTTP 接口契约与范围。

## 2. API 与 Trigger 实现

- [x] 2.1 扩展 `IAgentRefreshScheduleService`，声明基金目录刷新调度方法。
- [x] 2.2 扩展 `AgentRefreshScheduleController`，暴露基金目录刷新调度 HTTP 接口。
- [x] 2.3 复用 `AgentRefreshScheduleJob.runFundCatalogRefreshSchedule()`，保持与 cron 触发规则一致。

## 3. 验证

- [x] 3.1 补充 Controller 单元测试，验证接口调用基金目录刷新 Job 方法并返回成功响应。
- [x] 3.2 使用 JDK 17 运行相关 Maven 测试。
- [x] 3.3 运行 `openspec validate --strict expose-fund-catalog-refresh-http-trigger`。
