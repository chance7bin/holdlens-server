## 1. OpenSpec

- [x] 1.1 创建 proposal、design、spec 和 tasks，明确手动触发接口契约与范围。

## 2. Trigger 实现

- [x] 2.1 新增手动触发 Controller，暴露基金详情刷新调度运行接口。
- [x] 2.2 新增手动触发 Controller，暴露股票行情刷新调度运行接口。
- [x] 2.3 复用 `AgentRefreshScheduleJob` 方法，保持与 cron 触发行为一致。

## 3. 验证

- [x] 3.1 补充 Controller 单元测试，验证两个接口调用对应 Job 方法并返回成功响应。
- [x] 3.2 运行相关 Maven 测试或编译验证。
- [x] 3.3 运行 `openspec validate --strict manual-agent-refresh-schedule-runs`。
