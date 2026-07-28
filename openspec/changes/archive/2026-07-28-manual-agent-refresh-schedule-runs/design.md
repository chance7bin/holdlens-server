## Context

`AgentRefreshScheduleJob` 已包含两个 public 调度方法：

- `runFundRefreshSchedule()`：按配置扫描 `fund_detail_item` 并分批创建基金详情刷新任务。
- `runStockRefreshSchedule()`：按配置扫描 `stock_market_current` 并分批创建股票行情刷新任务。

这两个方法内部已经处理 enabled 开关、batch-size 合法性、同类型非终态任务跳过、keyset 分页扫描和批次异常停止。现有 HTTP Controller 位于 `holdlens-server-trigger`，统一返回 `Response<T>`，并通过 `holdlens-server-api` 中的接口描述 HTTP 契约。

## Goals / Non-Goals

**Goals:**

- 为基金详情刷新调度提供手动 HTTP 触发入口。
- 为股票行情刷新调度提供手动 HTTP 触发入口。
- 手动入口复用现有 Job 方法，避免复制批量扫描与任务创建逻辑。
- 保持手动触发与 cron 触发的执行规则一致。

**Non-Goals:**

- 不新增调度运行记录、运行 ID、进度查询或取消能力。
- 不绕过现有 enabled 开关。
- 不改变基金详情刷新、股票行情刷新任务创建和回调契约。
- 不引入新的认证鉴权机制；接口安全边界沿用现有 `/api/agent/**` 风格。

## Decisions

- 新增 API 层服务接口和 Trigger 层 Controller 暴露两个 POST 接口，Controller 只记录入口日志、调用 `AgentRefreshScheduleJob` 并封装 `Response<Void>`。
  - 备选方案：在现有 `AgentFundRefreshController` 中直接增加方法。放入独立 Controller 能让“显式列表任务创建”和“全量调度运行触发”职责更清楚。
- 手动触发直接调用现有 `AgentRefreshScheduleJob` 的 public 方法。
  - 备选方案：抽取新的 Case 层编排服务。当前调度逻辑已经在 Job Trigger 中实现，手动入口只是另一种 Trigger 方式；为此引入 Case 层会扩大改动面。
- 手动触发继续受 `holdlens.agent.*-refresh-schedule.enabled` 控制。
  - 备选方案：手动接口绕过开关。绕过会让配置语义变复杂，也更容易在生产误触发批量刷新。
- 接口返回 `Response<Void>`，只表示 HTTP 触发调用已执行完成。
  - 备选方案：返回创建的批次数或任务 ID 列表。现有 Job 方法没有返回统计，强行返回会要求改造调度方法和测试面；本次保持最小可用。

## Risks / Trade-offs

- [误触发批量刷新] -> 手动接口仍受 enabled 开关和同类型非终态任务跳过规则保护；后续如需要更强边界，应单独设计内部接口鉴权。
- [HTTP 请求耗时较长] -> 当前复用同步 Job 方法，接口会等待本轮扫描与下发结束；这是保持语义一致的代价。若后续需要异步运行记录，应单独扩展。
- [返回信息较少] -> `Response<Void>` 不直接告诉调用方创建了多少批次；调用方可通过现有任务查询和日志观察结果，避免本次扩大调度方法返回模型。
