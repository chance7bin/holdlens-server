## 1. 扩展手动调度 API

- [x] 1.1 扩展 `IAgentRefreshScheduleService`，声明七个新增手动调度操作并明确绕过 cron 开关。
- [x] 1.2 扩展 `AgentRefreshScheduleController`，直接委托既有基金和市场数据 Case，并复用现有超时、告警与市场参数。

## 2. 测试与回归

- [x] 2.1 扩展 Controller 单元测试，覆盖十个 POST 路径、七个新增调用路由、`manual` 来源、市场常量和超时配置。
- [x] 2.2 使用 JDK 17 运行 trigger 指定测试与相关模块回归测试。

## 3. OpenSpec 一致性与验证

- [x] 3.1 检查权限、数据暴露、幂等、事务和审计影响，确认新增入口未绕过 Case 业务边界。
- [x] 3.2 运行 `openspec validate --strict expose-remaining-schedules-http` 并确认通过。
