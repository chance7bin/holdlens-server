## 1. 解耦 HTTP 与 cron 入口

- [x] 1.1 调整 `AgentRefreshScheduleController` 直接调用 `IFundSliceRefreshCase`，三个接口传递 `manual` 并沿用现有 batch-size 配置。
- [x] 1.2 更新 `IAgentRefreshScheduleService` 注释，明确手动入口不受 cron `enabled` 开关影响。

## 2. 关闭 application 定时开关

- [x] 2.1 将 `application-dev.yml` 中基金刷新与回调超时的六个 `enabled` 设为 `false`，并确认 `application.yml` 同样全部关闭。

## 3. 测试与回归

- [x] 3.1 更新 Controller 单元测试，验证三个手动入口调用 Case、使用 `manual` 来源和配置的 batch size。
- [x] 3.2 更新 Job 配置测试，验证 common 与 dev 两份 application 配置中的六个调度开关均为关闭状态。
- [x] 3.3 使用 JDK 17 运行 trigger 相关单元测试和必要的 Maven 回归测试。

## 4. OpenSpec 一致性与验证

- [x] 4.1 同步修正直接涉及现有三个 HTTP 入口的 active OpenSpec 旧约定，避免继续描述手动调用受定时开关控制。
- [x] 4.2 运行 `openspec validate --strict decouple-manual-refresh-from-schedule-switches` 并确认通过。
