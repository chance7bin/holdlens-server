# Design: 统一公共市场详情确保与活动刷新

## Context

现有基金补全使用 slice 行锁但不返回 task id，客户端轮询数据 GET；股票使用 processing task 活动键并轮询 task。基金详情 GET 还会记录访问和派发重仓任务，导致纯查询包含副作用。股票基础表由全市场同步创建，不能用记录存在代表用户访问。

## Goals / Non-Goals

**Goals:**

- 详情页只需一个有副作用的进入动作，由 Server 判断是否需要 Agent。
- 客户端只在没有可展示数据且操作处理中时轮询。
- 可信空、失败、缺失、可用及陈旧状态可区分且由 Server 判定。
- 基金与股票的并发首次访问只派发一个活动任务。
- 访问激活、活动目标和调度策略符合 ADR-005。

**Non-Goals:**

- 不合并各数据切片的数据表或查询模型。
- 不提供分钟级实时行情或推送。
- 不物理删除基金、股票目录或详情数据。
- 不把用户持仓、自选明细或账户信息发送给 Agent。

## Decisions

1. 新增 `POST /api/market-assets/detail/ensure`，请求包含 `userId/assetKind/assetRef`。MarketAsset Case 编排当前公共详情、自选关系和 MarketDetail ensure，Trigger 不承载业务判断。
2. 统一返回 `operationId`、整体状态、切片状态、`hasData` 和独立 `freshness`。状态代码固定为 `MISSING/PROCESSING/AVAILABLE/EMPTY/FAILED`，新鲜度固定为 `FRESH/STALE`。
3. 新增 `GET /api/market-detail-data/operations/{operationId}`。GET 只读取任务和切片事实，不领取、不回收、不重试任务。旧基金/股票 API 映射到兼容响应。
4. 基金继续以 slice 状态行锁合并并发任务，并返回活动 task id；股票继续使用 `processing_task.active_key`。两者都以 callback 的 active task 条件保护迟到结果。
5. slice 初始状态为 `MISSING`。Agent 的可选 `slice_results` 显式声明 available/empty/failed；兼容旧 callback 时才依据 payload 推断。
6. 有事实且未过阈值时直接 AVAILABLE；有事实但过期时返回 AVAILABLE+STALE，同时后台创建或复用任务。失败冷却期内继续返回旧数据，不前台阻塞。
7. 基金和股票详情访问时间只在统一 POST 中更新，并用 SQL 条件把写入频率限制为可配置窗口。既有 GET 改为纯查询。
8. 活动目标为当前持有、任一用户自选或最近 90 天访问。基金净值/阶段业绩和股票日线/公司资料分别由定时入口调用相同 ensure 领取逻辑。
9. 股票全市场行情按市场在收盘后批量刷新一次。Trigger 使用市场时区 cron，Case 根据配置化休市日、提前收盘和日终刷新点判断是否派发；活动标的不影响该批量任务。盘中继续展示上一份快照及其数据时间。
10. 活动基金在交易日晚间检查净值和阶段业绩，净值与股票日线使用小于 24 小时的新鲜度阈值保证逐交易日领取；阶段业绩保持 7 天阈值。基金重仓每周检查且仅领取超过 15 天未成功获取的活动基金，资产配置每周检查但只为报告期落后于最近结束季度的活动基金派发。
11. `stock_market.refreshed_at` 对外命名为 `quoteFetchedAt`；`quoteAsOf` 只在未来取得交易所/来源行情时间时赋值。

## Data and Transactions

- 新增 `stock_market.last_detail_view_time` 及索引；迁移不改写现有行情和标的身份。
- 访问时间更新使用条件 UPDATE，不记录用户身份。
- 领取任务、slice 状态与 processing task 在同一事务；Agent 派发在事务提交后。
- 任务回调按 slice 独立事务持久化，旧 task id 不能覆盖新状态。

## Security and Privacy

- 统一入口校验正数 `userId`，仅用它读取当前用户自选关系。
- 公共详情任务只包含规范化 assetRef、slice 和必要市场代码，不包含用户 ID、持仓、金额或凭据。
- 操作查询只接受市场详情 ensure 产生的任务，未知或其他类型任务不返回参数和诊断。

## Rollback

- 客户端可回退到旧 GET + 显式刷新流程；兼容入口继续保留。
- 新列、索引、状态枚举和调度配置可保留；关闭新增 schedule enabled 即停止后台派发。
- 新 callback 字段为可选，旧 Agent 仍可按既有 payload 被 Server 推断处理。

## Open Questions

无。
