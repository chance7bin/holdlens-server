## 1. OpenSpec 与命名收敛

- [x] 1.1 将 ADR-004 从“关注资产与刷新目标双写”收敛为“用户自选基金/股票”。
- [x] 1.2 将 OpenSpec proposal/design/spec 统一为“批量加入自选”语义。
- [x] 1.3 固定 HTTP 路径为 `POST /api/watchlist/assets/batch-add`。
- [x] 1.4 明确本变更暂不调整 `asset_info` 表结构。

## 2. 数据库与持久化映射

- [x] 2.1 保留 `asset_info` 现有 upsert 和按 `user_id + asset_code + asset_kind` 查询能力。
- [x] 2.2 补充或复用 `fund_detail_item` 按 `fund_code` 查询已存在基金的能力。
- [x] 2.3 补充或复用 `stock_market_current` 按 `stock_code + market` 查询已存在股票的能力，空 market 只匹配空 market 记录。
- [x] 2.4 确保批量加入自选流程不 upsert `fund_detail_item` 或 `stock_market_current`。

## 3. Domain 与 Repository

- [x] 3.1 将关注/导入相关模型命名收敛为 `WatchlistAsset` / `BatchAdd` 语义。
- [x] 3.2 保留或调整 `IPortfolioRepository` 的批量 upsert 自选资产能力，并保持 Domain 接口不依赖 MyBatis 或 PO。
- [x] 3.3 为公开基金/股票存在性校验补充清晰的 Repository 接口语义。
- [x] 3.4 移除本用例中“注册基金刷新目标/股票刷新目标”的 Domain 接口依赖。

## 4. Case 用例编排

- [x] 4.1 将相关 Case 及实现统一为批量加入自选用例命名。
- [x] 4.2 完成请求归一化、请求内重复幂等处理和 `invalidItems` 结果构建。
- [x] 4.3 在写入 `asset_info` 前校验基金已存在于 `fund_detail_item`。
- [x] 4.4 在写入 `asset_info` 前校验股票已存在于 `stock_market_current`，并允许空 market 按空 market 匹配。
- [x] 4.5 确保批量加入自选流程不写 `asset_holding`，不写 `asset_holding_change`。
- [x] 4.6 移除批量加入自选后的基金详情刷新任务触发和股票行情刷新任务触发。
- [x] 4.7 增加 Case 单元测试，覆盖已存在幂等、请求内重复、基金不存在、股票不存在、空 market 股票、无刷新副作用。

## 5. API 与 Trigger

- [x] 5.1 将请求/响应 DTO 命名统一为 `WatchlistAssetBatchAddRequestDTO` / `WatchlistAssetBatchAddResponseDTO` 或等价语义。
- [x] 5.2 将 HTTP Controller 命名统一为 `WatchlistAssetController` 或等价语义，避免继续使用持仓或导入语义。
- [x] 5.3 将接口路径调整为 `POST /api/watchlist/assets/batch-add`。
- [x] 5.4 固定 `invalidItems.index` 为 0 基请求数组下标，并在 DTO 注释或接口说明中表达该语义。
- [x] 5.5 确认接口响应不包含新建数量、已存在数量、刷新任务 ID、刷新状态或刷新失败摘要。
- [x] 5.6 增加 Trigger/API 相关测试，覆盖请求映射、无效项响应和不暴露刷新任务字段。

## 6. 验证与质量门

- [x] 6.1 串行运行相关模块测试，至少覆盖 `holdlens-server-infrastructure`、`holdlens-server-case` 和 `holdlens-server-trigger` 中本变更相关测试。
- [x] 6.2 运行必要 Maven 编译或聚合测试，确认多模块依赖和 DTO/Case/Domain/Infrastructure 边界无编译问题。
- [x] 6.3 运行 `openspec validate --strict import-watch-assets` 并通过。
- [x] 6.4 做产品、工程、QA、发布、安全五个视角轻量评审，确认接口不注册刷新目标、不触发刷新任务、不暴露刷新任务细节。
- [x] 6.5 根据实际完成情况同步勾选 `tasks.md`，不提前标记未验证任务。
