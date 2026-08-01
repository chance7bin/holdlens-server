## Context

`AgentRefreshScheduleJob` 包含基金目录、申购状态、重仓、资产配置和回调超时处理入口，`MarketDataRefreshScheduleJob` 包含 A 股全市场、美股全市场、活跃基金详情、活跃 A 股详情和活跃美股详情入口。现有 `AgentRefreshScheduleController` 已直接委托 `IFundSliceRefreshCase` 暴露其中三个手动入口。

cron Job 的 `enabled` 只应控制自动触发。手动 HTTP 调用用于运维补刷和链路验证，应直接进入与 cron 相同的 Case 编排，不借用 Job 方法，以免被定时开关拦截。

## Goals / Non-Goals

**Goals:**

- 为当前尚未开放的七个定时任务提供统一风格的 POST 手动入口。
- 保持 Trigger 轻量，只负责日志、路由和统一响应。
- 保持 cron 与 HTTP 共用 Case 中的业务规则。
- 用单元测试覆盖路径映射、调用路由、市场参数和超时配置传递。

**Non-Goals:**

- 不合并或改名既有接口。
- 不允许通过 HTTP 覆盖 batch size、timeout 或 market 等服务端配置。
- 不改变定时开关、cron 表达式、交易日判断、任务去重或回调状态机。
- 不新增鉴权、审计、异步响应或运行结果 DTO。

## Decisions

### 1. 继续扩展统一调度 Controller

在 `IAgentRefreshScheduleService` 和 `AgentRefreshScheduleController` 中补齐七个方法。Controller 新增依赖 `IMarketDataRefreshScheduleCase`，基金任务继续使用 `IFundSliceRefreshCase`。这样所有手动调度入口保持相同的 HTTP 响应和命名结构，且不让 HTTP 入口依赖带 cron 语义的 Job。

### 2. 每个 cron 对应一个显式动作路径

新增路径为：

- `POST /api/agent/fund-purchase-status-refresh/schedule-runs`
- `POST /api/agent/fund-slice-callback-timeout/schedule-runs`
- `POST /api/agent/a-share-market-refresh/schedule-runs`
- `POST /api/agent/us-stock-market-refresh/schedule-runs`
- `POST /api/agent/active-fund-detail-refresh/schedule-runs`
- `POST /api/agent/active-a-share-detail-refresh/schedule-runs`
- `POST /api/agent/active-us-stock-detail-refresh/schedule-runs`

路径沿用现有 `.../schedule-runs` 风格，并与配置及 Job 名称一一对应，避免一个通用接口通过字符串参数选择任意任务。

### 3. 手动调用绕过 cron 开关但复用业务规则

Controller 直接调用 Case，因此不读取 `enabled`。申购状态传递 `manual` 来源；股票详情使用领域已有的 `A_SHARE` 或 `US_STOCK` 市场常量。全市场与活跃详情入口继续由 `IMarketDataRefreshScheduleCase` 内部执行既有交易日、目标选择和刷新规则。

### 4. 回调超时入口原子复用 cron 行为

手动回调超时处理按 cron 的既有顺序先调用 `closeTimedOutCallbacks(timeoutMinutes)`，再调用 `warnSlowCatalogCallbacks(processingWarningMinutes)`；两个参数读取现有配置。接口不暴露可覆盖参数，避免调用方扩大处理范围。

## Risks / Trade-offs

- [手动入口可在自动调度关闭时产生刷新或状态变更] -> 这是目标行为；继续受既有 Case 规则和 Controller 访问边界约束。
- [全市场刷新可能耗时] -> 保持现有同步调用语义和 `Response<Void>`，不在本次引入另一套任务查询协议。
- [回调超时处理会改变任务状态] -> 仅使用服务端固定阈值，复用 cron 的处理顺序，不允许调用方传入阈值。
- [新增接口没有单独审计记录] -> 继续使用现有入口日志；独立审计能力不属于本次范围。

## Migration Plan

无需数据库或配置迁移。部署后新增入口立即可用；回滚时删除新增映射和接口方法即可，cron 行为不受影响。

## Open Questions

当前无待确认事项，按用户确认的七个任务范围实施。
