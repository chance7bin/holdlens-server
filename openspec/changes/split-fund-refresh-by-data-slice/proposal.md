## Why

当前 `AgentRefreshScheduleJob` 将基金目录、申购状态、阶段收益和重仓信息绑定在一次 `fund_detail_refresh` 中，并扫描 `fund` 全表创建同质批次，导致不同更新频率的数据无法独立调度，基金全目录入库后还会把外部逐基金持仓调用放大到全市场。需要按外部接口粒度拆分刷新链路，让 server 继续作为任务编排、目标选择、幂等和最终写入的事实源。

## What Changes

- 新增 `fund_catalog_refresh`、`fund_purchase_status_refresh`、`fund_period_return_refresh`、`fund_top_holding_refresh` 四类独立任务，并分别对接冻结的 agent dispatch/callback 契约。
- 目录、申购状态和阶段收益按自然日每天调度；重仓按自然月 1 日、15 日调度；所有新调度默认关闭并可独立配置。
- 基金目录允许插入或更新返回记录，但不因数据源缺失行删除、禁用或清空存量基金；其他三个 slice 只更新已存在的基金代码。
- 收益只保存当前快照；基金重仓只保存最新已公开报告期的前十大股票及增减字段，并按报告期与内容决定替换、源修正或 no-op。
- 重仓定时目标限定为当前持有、关注、近 90 天查看的基金；详情页始终先返回数据库数据，缺失或陈旧时异步触发刷新，并把本次查看纳入 90 天目标窗口。
- 每个 slice 使用独立 Repository/Mapper XML/事务和抓取时间；回调幂等、slice 写入与任务终态在同一事务内提交。
- 首期不新增 `processing_task_item`，重仓默认每批 20 只基金、最多 2 个批次并发；批内部分失败保留旧数据并返回 `partial_failed`。
- **BREAKING**：停用并最终退役旧 `fund_detail_refresh` 复合调度和 endpoint；完整目录导入后禁止恢复扫描 `fund` 全表的旧复合刷新。
- 数据库交付同时支持重建 `holdlens.sql` 和前向 ALTER migration，但数据库文件在后续实现阶段创建。

## Capabilities

### New Capabilities

- `fund-data-slice-refresh`: 定义四类基金数据 slice 的调度、目标选择、回调幂等、独立写入、异常保护、详情页异步回源和旧链路退役行为。

### Modified Capabilities

- 无。当前 `openspec/specs` 下暂无已归档能力；本变更在 change 内新增完整能力规格。

## Impact

- 影响模块：`holdlens-server-api`、`holdlens-server-trigger`、`holdlens-server-case`、`holdlens-server-domain`、`holdlens-server-infrastructure`、`holdlens-server-app`。
- 影响外部契约：新增四组 agent dispatch/callback；旧 `fund_detail_refresh` 契约进入退役流程。
- 影响数据模型：扩展 `fund` 的目录、申购、当前收益与各 slice 抓取时间字段，保留 `fund_top_holding` 作为当前前十大重仓子表，并引入近期查看事实以支持 90 天窗口。
- 影响调度与发布：agent endpoints 必须先发布；server 新调度默认关闭；停旧复合调度后再依次启用目录、申购、收益和重仓。
- 需求来源：本次基金刷新架构讨论及已确认架构图；不直接关联 `docs/requirements/**/prd-*.md`，无需更新 PRD 状态。

## Scope Boundaries

- 本期不保存或展示每日净值/收益历史序列。
- 本期不保存全量股票持仓、债券持仓或历史持仓报告期。
- 本期不接入基金公告接口，不做跨数据源自动 fallback，也不让详情页同步阻塞外部接口。
- 本期不新增逐基金任务明细表或可恢复游标；只保留批次级 `processing_task` 状态。

## Success Criteria

- 四类任务可以独立调度、独立失败、独立回调并且只更新自己的数据 slice。
- 全量基金目录入库不会触发全市场逐基金重仓刷新。
- 空结果、较旧重仓、未知基金代码和回调重试不会清空或重复写入有效存量数据。
- 旧复合任务停用后，新链路可按顺序灰度启用并可通过关闭独立开关回滚调度。
