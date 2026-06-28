## Why

当前股票行情刷新会把 `market = null` 的股票目标下发给 agent，但现有 agent 无法支持 market 为空的股票查询，导致股票刷新请求包含不可处理目标并影响刷新稳定性。

## What Changes

- 股票行情刷新任务创建时，只把 `stock_code` 和 `market` 都非空的股票目标纳入本次 agent 下发范围。
- `stock_market_current` 仍允许保存 `market = null` 的股票记录，基金回调注册空 market 股票的能力不回退。
- 当当前股票表没有任何可下发的非空 market 目标时，股票行情刷新任务应拒绝或跳过创建。
- 股票行情刷新任务记录中的 `stockCount` 应反映实际下发给 agent 的目标数量，而不是数据库中的全部股票候选数量。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `fund-stock-refresh-persistence`: 调整股票行情刷新目标下发规则，空 market 股票保留在股票库中但不进入 agent 股票行情刷新请求。

## Impact

- 影响股票行情刷新用例编排：从仓储读取候选股票后，需要过滤掉 `market` 为空或空白的目标，再创建任务和下发 agent。
- 影响 agent 股票行情刷新出站契约：server 不再主动发送 `market: null` 的股票目标。
- 影响测试：需要更新空 market 下发相关 Case 和 Infrastructure 测试，覆盖过滤、计数和全为空时拒绝创建。
- 不涉及数据库结构变更，不改变基金详情回调保存和注册空 market 股票目标的行为。
