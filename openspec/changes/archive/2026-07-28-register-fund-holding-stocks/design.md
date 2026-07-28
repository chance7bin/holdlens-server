## Context

`split-fund-stock-refresh-persistence` 已将基金刷新和股票刷新拆分：

- 基金刷新回调保存 `fund_detail_item` 和 `fund_top_holding`。
- 股票行情刷新保存 `stock_market_current`。
- 股票刷新任务从 `stock_market_current` 读取全部 `stock_code + market` 作为刷新范围。

现有设计刻意不从 `fund_top_holding` 自动推导股票刷新范围。本变更按用户新需求补齐该规则：基金回调落库后，将本次回调中的重仓股票注册进股票当前市场表，作为后续股票行情刷新的 universe。

## Decisions

### 基金回调注册股票标的

基金刷新回调在通过 schema、任务类型、幂等键校验后，且目标状态为 `succeeded` 或 `partial_failed` 时：

1. 保存基金当前详情和重仓关系。
2. 从本次回调转换出的 `FundCurrentDataAggregate` 中提取所有有效重仓股票。
3. 按 `stock_code + market` 去重后调用 `IStockMarketRepository.registerQuoteTargets(...)`。
4. 更新任务和回调处理状态。

该流程仍处于同一个事务内。如果注册股票标的失败，回调处理失败并回滚本次事务，避免基金重仓已更新但股票刷新范围缺失。

### 注册不覆盖行情字段

`registerQuoteTargets` 表达的是“让股票进入股票表和刷新范围”，不是行情刷新。Infrastructure 需要新增独立 DAO/XML 语句：

- 插入新记录时写 `stock_code`、`market`、`stock_name`。
- 遇到已有记录时，只在入参股票简称非空时更新 `stock_name`。
- 不更新 `trade_date`、`daily_return`、`quote_time`，避免基金回调把已有行情置空。

股票行情字段仍只由 `stock_quote_refresh` 回调的 `upsertQuotes` 更新。

### 数据过滤和幂等

- 缺少 `stock_code` 或 `market` 的重仓不注册。
- `stock_code`、`market` 和 `stock_name` 在 Case 层 trim。
- 同一回调内重复股票按首次出现保留，避免重复 DAO 写入。
- 重复 `idempotency_key` 或已终态任务沿用现有提前返回逻辑，不保存基金数据也不注册股票。

## Layering

- Case 层负责跨基金数据与股票数据两个领域的流程编排。
- Domain 层只新增 Repository 接口方法，不依赖 MyBatis 或 SQL。
- Infrastructure 的 `adapter/repository` 调用 `dao`，MyBatis SQL 继续放在 `holdlens-server-app/src/main/resources/mybatis/mapper`。

## Risks

- 如果 agent 返回错误 market，股票会进入错误刷新范围；本变更只按回调契约保存，不引入市场推断。
- 如果股票简称为空，新记录可能只有代码和市场；后续股票行情刷新可补全简称。
- 注册股票标的失败会回滚基金回调，保证一致性，但可能让一次基金回调整体失败，需要依赖 agent 重试或人工重放回调。
