## Why

基金详情回调中有些重仓股票只有 `stock_code` 和 `stock_name`，没有 `market`。当前实现会把这类重仓保存到 `fund_top_holding`，但不会注册到 `stock_market_current`，导致股票库缺少待补全的股票记录，也不利于后续诊断和市场信息补全。

用户要求：基金详情刷新回调中 `market` 为 `null` 的重仓股票也允许进入股票库。

## What Changes

- 基金详情刷新回调成功或部分成功后，允许从本次重仓中提取非空 `stock_code` 的股票并注册到 `stock_market_current`，即使 `market` 为空。
- `stock_market_current.market` 从必填调整为可空；唯一性规则从“全部记录按 `stock_code + market` 唯一”调整为“有 market 的记录按 `stock_code + market` 唯一，market 为空的待补全记录按 `stock_code` 唯一”。
- 股票行情刷新任务将选取 `stock_code` 非空的记录作为报价目标；当 `market` 为空时，仍以 `market: null` 下发给 agent。
- 注册股票目标时继续不覆盖已有行情字段，如 `trade_date`、`daily_return`、`quote_time`。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `fund-stock-refresh-persistence`: 调整基金重仓股票注册规则，允许缺少 market 的股票进入 `stock_market_current`，并允许这类记录进入股票行情刷新范围。

## Impact

- 影响后端基金详情回调用例编排：`toQuoteTargets` 过滤条件需要从“stock code 和 market 都必填”调整为“stock code 必填，market 可空”。
- 影响股票市场数据仓储、DAO、Mapper XML 和初始化 SQL：`stock_market_current.market` 需要允许 `NULL`，并处理 nullable market 下的唯一性和 upsert。
- 影响股票行情刷新任务创建：刷新范围从“stock code + market 都非空”调整为“stock code 非空即可”，agent 股票行情刷新请求可能包含 `market: null`。
- 需要补充单元测试或仓储测试，覆盖 `market = null` 的注册、去重、已有行情字段保护，以及股票刷新目标包含 market 为空记录。
