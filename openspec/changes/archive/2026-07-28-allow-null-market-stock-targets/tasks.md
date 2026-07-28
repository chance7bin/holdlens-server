## 1. 数据模型与持久化

- [x] 1.1 调整 `docs/dev-ops/mysql/sql/holdlens.sql`，允许 `stock_market_current.market` 为 `NULL`，并增加可保障 `stock_code + null market` 幂等的归一化唯一约束。
- [x] 1.2 调整 `stock_market_current_mapper.xml` 的注册目标 upsert，使 `market = NULL` 可插入、可幂等更新，并继续不覆盖 `trade_date`、`daily_return`、`quote_time`。
- [x] 1.3 调整 `selectAllTargets`，使 `stock_code` 非空的记录都进入股票刷新目标，包括 `market = NULL` 的记录。

## 2. 用例编排

- [x] 2.1 调整 `AgentFundRefreshCaseImpl.toQuoteTargets`，从基金重仓中注册非空 `stock_code`，并将空白 `market` 归一化为 `NULL`。
- [x] 2.2 调整同一回调内的股票目标去重规则，确保 `stock_code + null market` 只产生一个注册目标。
- [x] 2.3 保持重复回调或终态任务提前返回逻辑不变，不重复注册股票目标。

## 3. 测试验证

- [x] 3.1 补充 Case 测试：基金回调中 `market = null` 或空白时，股票目标会被注册到 `stock_market_current` 入参。
- [x] 3.2 补充 Case 测试：缺少 `stock_code` 的重仓仍不会注册。
- [x] 3.3 补充 Repository 或 Mapper 相关测试：空 market 注册幂等，且不覆盖已有行情字段。
- [x] 3.4 补充股票刷新任务测试：`market = null` 的股票记录会进入 agent 股票行情刷新请求，并以下发字段 `market: null` 表达。

## 4. 质量门

- [x] 4.1 串行运行相关 Maven 测试，至少覆盖 `holdlens-server-case` 与 `holdlens-server-infrastructure` 中被修改路径的测试。
- [x] 4.2 运行 `openspec validate --strict allow-null-market-stock-targets` 并通过。
- [x] 4.3 确认 `tasks.md` 勾选状态与实际完成和验证结果一致。
