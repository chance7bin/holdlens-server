## 1. 契约与设计确认

- [x] 1.1 确认根目录 ADR `docs/decisions/adr-001-fund-stock-refresh-boundary.md` 与本 change 范围一致。
- [x] 1.2 与 agent change `split-fund-stock-refresh-tasks` 对齐基金刷新 v2、股票刷新 v1 的请求/回调 schema、状态枚举和错误语义。
- [x] 1.3 已确认用户决策：股票当前市场数据表命名采用 `stock_market_current`；股票刷新范围取 `stock_market_current` 表内所有股票；不兼容旧回调字段；SQL 直接改初始化脚本，不做在线迁移。

## 2. 数据库 schema 调整

- [x] 2.1 调整数据库初始化 SQL，删除 `fund_detail_snapshot`。
- [x] 2.2 调整 `fund_detail_item`：删除 `snapshot_id`、`field_sources_json`、`missing_reasons_json`，增加或确认 `fund_code` 唯一约束。
- [x] 2.3 调整 `fund_top_holding`：删除 `fund_detail_item_id`、`snapshot_id`、`daily_return`、`missing_reasons_json`，新增 `fund_code`，唯一约束改为 `fund_code + rank_no`。
- [x] 2.4 新增 `stock_market_current` 表，唯一约束为 `stock_code + market`。

## 3. 基金当前数据落库

- [x] 3.1 调整 FundData 领域模型和 Repository 接口，使基金详情和重仓按当前表 upsert。
- [x] 3.2 调整 PO、DAO 和 Mapper XML，移除 snapshot 相关字段和查询。
- [x] 3.3 调整基金刷新回调 DTO/Command，使用 `fund-detail-refresh-result/v2`，不再接收 `data_sources`、`field_sources`、`missing_reasons`、`top_holdings.daily_return`。
- [x] 3.4 调整回调处理事务：重复回调不重复写入；当前重仓旧 rank 不残留。
- [x] 3.5 验证：基金详情 upsert、重仓覆盖、重复回调、部分失败和 unsupported schema 测试通过。

## 4. 股票行情刷新能力

- [x] 4.1 新增股票行情领域模型、Repository 接口、PO、DAO 和 Mapper XML。
- [x] 4.2 新增 `stock_quote_refresh` 任务创建与 agent 下发能力，股票范围从 `stock_market_current` 全表读取。
- [x] 4.3 新增股票刷新回调 DTO/Command 和处理 Case，使用 `stock-quote-refresh-result/v1`。
- [x] 4.4 实现 `stock_market_current` upsert 和股票刷新 warning 写入 `processing_log`。
- [x] 4.5 验证：股票任务创建、空股票列表处理、成功回调、部分失败回调、重复回调测试通过。

## 5. 组合查询调整

- [x] 5.1 调整账户基金详情查询，不再通过 snapshot 查询最新基金详情。
- [x] 5.2 查询当前 `fund_detail_item`、`fund_top_holding` 和 `stock_market_current` 后拼接返回。
- [x] 5.3 确认缺少基金详情、缺少重仓、缺少股票行情时仍返回 portfolio 持仓主体。
- [x] 5.4 验证：有完整公开数据、缺少股票行情、未持有基金不返回、跨用户隔离和 stale/缺失状态测试通过。

## 6. 质量门与安全评审

- [x] 6.1 运行相关 Maven 模块测试。
- [x] 6.2 运行 `openspec validate --strict split-fund-stock-refresh-persistence`。
- [x] 6.3 从产品、工程、QA、发布、安全五个视角做轻量评审。
- [x] 6.4 确认 `task_params_json`、`processing_log.message`、`error_summary` 和应用日志不保存真实资产明细、账户标识、完整持仓或凭据。
