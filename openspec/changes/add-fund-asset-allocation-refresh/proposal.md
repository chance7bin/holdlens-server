## Why

当前 server 只保存基金收益与前十大股票重仓，无法表达基金在股票、债券、现金等资产类别上的独立配置。需要新增基金资产配置刷新切片，由 server 继续负责目标选择、任务幂等、有效快照取舍和长期持久化，避免将资产配置错误混入重仓语义或交给 agent 决定写入规则。

## What Changes

- 新增独立 `fund_asset_allocation_refresh` 任务，复用基金重仓的“当前持有、关注/有效资产主数据、近 90 天查看”目标发现规则，但使用独立 task type、endpoint、callback、开关、每周 cron 和手动 HTTP 调度入口。
- 新增基金资产配置当前快照表，并在 `fund` 表维护报告期、可用状态和最近认可抓取时间；资产配置与 `fund_top_holding` 保持完全独立。
- 固定 `fund-asset-allocation-refresh-task/v1` dispatch 与 `fund-asset-allocation-refresh-result/v1` callback 契约；server dispatch 只发送基金代码，报告期探测由 agent 执行。
- 对有效非空快照执行事务内原子覆盖，支持新报告期替换、同期 no-op/源修正、旧报告期忽略、未知基金跳过和 unavailable/失败保留历史数据。
- 基金详情 API 增加独立资产配置报告期、状态、抓取时间和配置明细，不改变已有前十大重仓字段及语义。
- 新增数据库重建基线和前向迁移，并为幂等、原子覆盖、报告期排序、空结果保护、调度派发、详情响应和敏感信息保护补充测试。

## Capabilities

### New Capabilities

- `fund-asset-allocation-refresh`: 定义基金资产配置的任务调度、agent 契约、回调幂等、当前快照持久化保护和详情 API 展示行为。

### Modified Capabilities

- 无。当前 `openspec/specs` 下没有需要调整的已归档能力；本 change 提供独立完整能力规格。

## Impact

- 影响模块：`holdlens-server-api`、`holdlens-server-case`、`holdlens-server-domain`、`holdlens-server-infrastructure`、`holdlens-server-trigger`、`holdlens-server-app`。
- 影响数据模型：扩展 `fund`，新增 `fund_asset_allocation` 当前快照表；不改变 `fund_top_holding`。
- 影响外部契约：新增 agent dispatch `/tasks/fund-asset-allocation-refresh` 与 server callback `/internal/agent/fund-asset-allocation-refresh/callback`。
- 影响对外 API：基金详情新增 `assetAllocationAsOf`、`assetAllocationStatus`、`assetAllocationFetchedAt` 和 `assetAllocations`。
- 需求来源：前序基金资产配置方案讨论及本次明确实现授权；不直接关联 `docs/requirements/**/prd-*.md`，无需更新 PRD 状态。
- 范围边界：只保存单基金当前有效资产配置，不保存历史报告期，不改变前十大股票重仓语义，不让 agent 直接写业务数据库，不在 server dispatch 中指定报告期。
- 成功标准：新切片可独立调度、派发、回调和失败；无效/较旧/空结果不删除旧快照；有效结果原子覆盖；详情 API 独立返回资产配置；相关测试、JDK 17 Maven 构建和严格 OpenSpec 校验通过。
