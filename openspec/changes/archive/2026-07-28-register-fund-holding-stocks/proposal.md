## Why

基金刷新回调已经会保存基金当前详情和基金当前重仓关系，但股票行情刷新范围只来自 `stock_market_current`。当基金首次刷新出新的重仓股票时，这些股票不会自动进入股票表，后续股票行情刷新也无法覆盖它们。

用户要求：基金刷新回调后，如果基金有对应的股票持仓，就把股票信息加到股票表里面去。

## What Changes

- 基金刷新回调成功或部分成功后，从本次回调的基金重仓中提取有效 `stock_code + market`。
- 将这些股票注册到 `stock_market_current`，使它们进入后续股票行情刷新范围。
- 注册股票标的时只补充股票身份和简称，不覆盖已有 `trade_date`、`daily_return`、`quote_time` 等行情字段。
- 重复回调或终态任务保持幂等，不重复注册股票标的。

## Scope

- 后端 Case 编排：基金刷新回调处理事务。
- Domain Repository 接口：股票当前市场数据仓储新增“注册股票标的”语义。
- Infrastructure Repository/DAO/MyBatis XML：新增不覆盖行情字段的 upsert。
- 测试：覆盖基金回调注册股票、跳过无效股票、重复回调幂等和注册不覆盖行情字段。

## Non-Goals

- 不在基金回调里保存股票涨跌幅或行情时间。
- 不改变股票行情刷新 agent 契约。
- 不调整前端展示或组合查询返回结构。
- 不新增在线迁移脚本；现阶段仍只维护初始化 SQL 与当前表行为。

## Success Criteria

- 基金刷新回调含有效重仓股票时，`stock_market_current` 至少存在对应 `stock_code + market` 记录。
- 已有股票行情记录被基金回调再次注册时，行情字段不被置空或覆盖。
- 重复基金回调不会再次写基金数据或股票标的。
- 相关 Maven 测试与 `openspec validate --strict register-fund-holding-stocks` 通过。
