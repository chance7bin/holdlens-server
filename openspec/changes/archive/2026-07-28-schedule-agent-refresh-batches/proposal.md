## Why

当前基金详情和股票行情刷新主要依赖手动 HTTP 触发，其中股票刷新只能从 `stock_market_current` 全量读取并一次性下发。为了让公开基金详情和股票行情可以低频自动刷新，同时避免单次任务过大，需要新增定时分批触发能力，并补齐股票手动指定列表刷新能力。

## What Changes

- 新增基金详情定时刷新任务：按 `fund_detail_item` 表内有效 `fund_code` 使用 keyset 分页扫描全表，每批创建独立 agent 基金详情刷新任务。
- 新增股票行情定时刷新任务：按 `stock_market_current` 表内有效 `stock_code + market` 使用 keyset 分页扫描全表，每批创建独立 agent 股票行情刷新任务。
- 定时任务配置化且默认关闭，分别支持 cron 和 batch size 配置。
- 同类型存在非终态任务时，本轮定时任务跳过，避免重复下发和回调乱序。
- 每批任务复用现有 processing task 状态模型；批次下发失败或返回异常状态时停止本轮，已经创建的前置批次不回滚。
- 股票手动刷新接口改为接收股票列表，支持按 `stockCode + market` 创建单批股票行情刷新任务。
- 保留现有基金手动传基金代码列表刷新能力。
- 本次不做调度游标持久化、不做最近更新时间/过期过滤、不新增 `allowNetwork` 配置开关。

## Capabilities

### New Capabilities

- `agent-refresh-scheduling`: 定时分批触发基金详情和股票行情刷新，并支持股票手动列表刷新。

### Modified Capabilities

- 无。当前 `openspec/specs` 下暂无已归档能力；本变更在 change 内新增能力规格。

## Impact

- 影响模块：`holdlens-server-api`、`holdlens-server-trigger`、`holdlens-server-case`、`holdlens-server-domain`、`holdlens-server-infrastructure`、`holdlens-server-app`。
- 影响接口：`POST /api/agent/stock-quote-refresh/tasks` 将支持请求体传入股票列表；基金手动刷新接口保持现有契约。
- 影响数据访问：需要为 `fund_detail_item` 和 `stock_market_current` 增加按 `id` keyset 分页查询有效刷新目标的 DAO/Repository 能力，SQL 继续放在 Mapper XML。
- 影响配置：新增定时任务 enabled、cron、batch-size 配置，默认关闭。
- 需求来源：本次用户讨论收敛的运维自动刷新需求；不直接关联 `docs/requirements/**/prd-*.md`，无需更新 PRD 状态。
