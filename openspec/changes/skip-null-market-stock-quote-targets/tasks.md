## 1. 用例编排

- [x] 1.1 在 `AgentFundRefreshCaseImpl.createAndDispatchStockQuotes` 中基于仓储候选目标构建实际下发目标，过滤掉 `stock_code` 为空、`market` 为空或空白的记录。
- [x] 1.2 使用过滤后的目标创建 `stock_quote_refresh` 任务、写入 `stockCount` 并下发 agent。
- [x] 1.3 当过滤后没有可下发目标时，沿用现有“股票刷新范围为空”的失败语义拒绝创建任务。

## 2. Agent 出站适配

- [x] 2.1 更新 `AgentFundRefreshPort` 的股票行情请求 payload 转换测试，验证不会生成 `market = null` 的下发项。
- [x] 2.2 如实现需要，在 Port 层保留防御性过滤，但不把任务计数和是否创建任务的业务判断放入 Port 层。

## 3. 测试验证

- [x] 3.1 更新 Case 测试，覆盖混合目标时只下发非空 market 股票，并验证 `stockCount` 等于实际下发数量。
- [x] 3.2 更新 Case 测试，覆盖候选目标全部缺少 market 时拒绝创建股票行情刷新任务。
- [x] 3.3 确认基金回调注册空 market 股票目标的既有测试仍保留并通过，避免回退持久化能力。
- [x] 3.4 串行运行相关 Maven 测试，至少覆盖 `AgentFundRefreshCaseImplTest` 和 `AgentFundRefreshPortTest`。
- [x] 3.5 运行 `openspec validate --strict skip-null-market-stock-quote-targets` 并通过。
