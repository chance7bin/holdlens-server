## Context

根目录 ADR `docs/decisions/adr-001-fund-stock-refresh-boundary.md` 已决定将基金刷新和股票刷新拆分。server 当前实现仍以 `FundDetailSnapshotAggregate` 为核心落库：回调成功时先写 `fund_detail_snapshot`，再写 `fund_detail_item` 和 `fund_top_holding`，查询最新基金详情时通过 snapshot 状态与生成时间排序。

本 change 调整 server 侧持久化与编排模型，使业务表只保存当前可用事实：

- `fund_detail_item`：每只基金当前详情。
- `fund_top_holding`：每只基金当前前十大重仓关系。
- `stock_market_current`：每只股票当前市场数据。
- `processing_*`：刷新任务、回调幂等与诊断日志。

## Goals / Non-Goals

**Goals:**

- 调整基金相关 DDL 和数据访问模型，删除 snapshot 关联字段。
- 新增股票当前行情表和 Repository/DAO/Mapper。
- 升级基金刷新回调处理为当前表 upsert。
- 新增股票刷新任务下发、回调接收和行情 upsert。
- 调整组合查询，让基金重仓股票涨跌幅来自股票行情表。
- 将缺失、异常、部分失败和 provider 诊断统一写入 `processing_log`。

**Non-Goals:**

- 不保存历史基金详情快照。
- 不保存股票历史日行情。
- 不直接在 server 抓取外部行情接口。
- 不实现字段级缺失原因结构化展示。
- 不变更 `portfolio` 作为用户持仓事实源的边界。

## Decisions

### 基金刷新结果写入当前表

基金刷新回调使用新的结果契约，例如 `fund-detail-refresh-result/v2`。server 校验任务、schema version 和幂等键后，将成功基金详情 upsert 到 `fund_detail_item`，将每只基金的当前重仓按 `fund_code + rank_no` upsert 到 `fund_top_holding`。

由于 `fund_top_holding` 只维护当前版，保存时应以本次回调的基金代码为边界清理或覆盖旧 rank，避免某次重仓数量减少时残留旧排名。

### 删除 `fund_detail_snapshot`

刷新批次状态、错误摘要和幂等信息由 `processing_task` 与 `processing_callback` 承担；缺失和诊断信息由 `processing_log` 承担。删除 `fund_detail_snapshot` 后，不再以 snapshot 排序来查询最新基金详情。

### `fund_top_holding` 直接保存 `fund_code`

当前版模型中，`fund_detail_item` 与 `fund_top_holding` 都以 `fund_code` 表达基金身份。`fund_detail_item_id` 不再提供区分快照或版本的价值，因此删除该字段，避免同时维护技术主键关联和业务键关联。

### 股票行情独立建模

新增 `stock_market_current`，以 `stock_code + market` 唯一。股票行情由 `stock_quote_refresh` 任务更新，基金重仓查询只通过股票代码和市场关联行情，不把 `daily_return` 存在 `fund_top_holding` 中。

### 股票刷新任务来源

server 创建股票刷新任务时，直接从 `stock_market_current` 表读取全部 `stock_code + market` 作为刷新范围。`fund_top_holding` 只表达基金当前重仓关系，不作为本 change 的股票刷新范围来源。

如果后续需要自动把基金重仓股票加入股票刷新范围，或合并用户直接持有股票资产，应通过新的 OpenSpec change 明确维护股票刷新 universe 的规则。

### processing 继续承载任务和诊断

`processing_task.task_type` 支持：

- `fund_detail_refresh`
- `stock_quote_refresh`

`processing_log.module` 建议使用：

- `fund_refresh`
- `stock_quote_refresh`

缺失原因、provider 失败、部分失败和无法解析的行信息写入 `processing_log.message` 的安全摘要，不进入基金或股票业务表 JSON 字段。

### 查询链路

账户基金详情查询顺序保持：

1. 读取当前用户 `portfolio` 持仓。
2. 提取基金代码查询 `fund_detail_item`。
3. 查询每只基金当前 `fund_top_holding`。
4. 按 `stock_code + market` 查询 `stock_market_current`。
5. 拼接返回，不用基金详情覆盖账户、资产金额或持仓事实。

## Contract Sketch

基金刷新回调 v2 示例：

```json
{
  "schema_version": "fund-detail-refresh-result/v2",
  "server_task_id": "task_fund_123",
  "idempotency_key": "task_fund_123:result:1",
  "status": "succeeded",
  "generated_at": "2026-06-18T10:00:00Z",
  "funds": [
    {
      "fund_code": "000001",
      "fund_name": "示例基金",
      "buy_status": "open",
      "daily_purchase_limit": null,
      "returns_as_of": "2026-06-18",
      "top_holdings_as_of": "2026-03-31",
      "public_holdings_status": "public",
      "one_month_return": "1.23",
      "three_months_return": "2.34",
      "six_months_return": "3.45",
      "one_year_return": "4.56",
      "three_years_return": null,
      "top_holdings": [
        {
          "rank_no": 1,
          "stock_name": "示例股份",
          "stock_code": "600000",
          "market": "1",
          "holding_ratio": "8.00",
          "quarter_change_type": "increased",
          "quarter_change_value": "1.00"
        }
      ]
    }
  ],
  "refresh_warnings": [],
  "error_summary": null
}
```

股票刷新回调 v1 示例：

```json
{
  "schema_version": "stock-quote-refresh-result/v1",
  "server_task_id": "task_stock_123",
  "idempotency_key": "task_stock_123:result:1",
  "status": "succeeded",
  "generated_at": "2026-06-18T10:05:00Z",
  "quotes": [
    {
      "stock_code": "600000",
      "market": "1",
      "stock_name": "示例股份",
      "trade_date": "2026-06-18",
      "daily_return": "0.50",
      "quote_time": "2026-06-18T10:04:30+08:00"
    }
  ],
  "refresh_warnings": [],
  "error_summary": null
}
```

## Persistence Impact

目标 DDL 方向：

- 删除 `fund_detail_snapshot`。
- `fund_detail_item` 删除 `snapshot_id`、`field_sources_json`、`missing_reasons_json`、`generated_at`，新增或确认 `fund_code` 唯一索引。
- `fund_top_holding` 删除 `fund_detail_item_id`、`snapshot_id`、`daily_return`、`missing_reasons_json`，新增 `fund_code` 和唯一索引 `fund_code + rank_no`。
- 新增 `stock_market_current`，唯一索引 `stock_code + market`。

当前阶段直接调整初始化 SQL，不做在线迁移脚本。若已有真实数据需要保留，需另开迁移任务。

## Security / Privacy

- server 下发给 agent 的基金任务只包含基金代码和任务上下文。
- server 下发给 agent 的股票任务只包含股票代码、市场和任务上下文。
- `task_params_json`、`processing_log.message`、`error_summary` 和应用日志不得保存真实账户名称、资产金额、完整持仓、token、cookie、API key 或回调鉴权。
- 组合查询必须继续按 `user_id` 隔离 `portfolio` 持仓；基金详情和股票行情作为公开数据，不反向决定用户持仓事实。

## Risks / Trade-offs

- [历史快照能力丢失] -> 当前阶段接受不支持刷新结果回放，后续如需要通过新 change 设计历史表。
- [字段级缺失展示能力丢失] -> 当前阶段缺失原因进入 `processing_log`，只用于排查，不作为字段级 UI 展示契约。
- [跨 agent/server 契约升级] -> 新 schema version 必须与 agent change `split-fund-stock-refresh-tasks` 对齐；本 change 不兼容旧回调字段。
- [重仓覆盖残留] -> 保存当前重仓时必须处理旧排名清理或全量替换。
- [股票行情过期] -> 查询结果应能表达缺失或过期行情，具体状态在实现阶段与 DTO 对齐。

## Resolved Decisions

- 股票当前市场数据表命名为 `stock_market_current`。
- 股票刷新范围直接取 `stock_market_current` 表内所有股票，不从 `fund_top_holding` 或 `portfolio` 自动推导。
- 不兼容旧 agent 回调字段，server 与 agent 同步升级新 schema。
- SQL 只直接修改初始化脚本，不新增在线迁移脚本。
