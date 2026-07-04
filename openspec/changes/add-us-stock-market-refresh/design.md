## Context

现有 A 股全量刷新链路为：

1. 调用方请求 `POST /api/agent/a-share-market-refresh/tasks`。
2. server 创建 `a_share_market_refresh` processing task。
3. server 下发 agent `/tasks/a-share-market-refresh`。
4. agent 回调 `/internal/agent/a-share-market-refresh/callback`。
5. server 校验 schema、任务类型、幂等键和 `market = A_SHARE` 后，分批 upsert `stock_market`。

美股已有 agent 数据源接口 `stock_us_spot_em()`，但还没有跨项目任务、回调契约和 server 持久化流程。

## Goals / Non-Goals

**Goals:**

- 按 A 股全流程新增美股任务创建、下发、回调和持久化。
- 固定业务市场 `US_STOCK`，并将东方财富 `105`、`106`、`107` 保存为 `provider_market_code`。
- 复用 `processing_task`、`processing_callback`、`processing_log` 的任务状态、回调幂等和诊断模型。
- 复用 `stock_market` 当前行情表，但补齐多市场语义和美股特有字段。
- 数值字段继续按 string/null 入契约，server 入库前转 BigDecimal/Long。

**Non-Goals:**

- 不把 A 股任务重构为通用多市场任务。
- 不恢复旧 `stock_quote_refresh` 股票列表刷新。
- 不在 server 访问东方财富、AKShare、HLShare 或其他外部行情源。
- 不保存历史行情、分钟行情、逐笔行情或回调原始 JSON。
- 不实现分片回调或 agent 结果文件下载。

## Decisions

### 1. 独立任务而不是泛化 A 股任务

新增 `us_stock_market_refresh`，与 `a_share_market_refresh` 并列。实现可以复用内部私有方法或小型 helper，但外部 URL、schema version、task type、回调入口保持独立。

选择原因：A 股契约已经稳定，泛化为 `stock_market_refresh` 会同时触碰 A 股调用方、测试、文档和配置。独立任务能保持回滚边界清晰。

### 2. `US_STOCK` 是业务市场

server 接收回调时要求顶层 `market = US_STOCK`，并跳过或拒绝明细中非 `US_STOCK` 的股票记录。东方财富 `f13` 或 secid 中的 `105`、`106`、`107` 只进入 `provider_market_code`。

`exchange_code` 第一版可为空；若 agent 后续能可靠映射，可使用 `NASDAQ`、`NYSE`、`AMEX` 等稳定交易所代码。

### 3. `stock_market` 扩展为多市场当前行情表

现有 `stock_market.market` 注释偏向 `A_SHARE`。实现时应改为业务市场枚举，例如 `A_SHARE`、`US_STOCK`。

美股结果中的 `pe_ratio` 不应写入 `pe_dynamic`。推荐新增：

- `pe_ratio DECIMAL(12,4) DEFAULT NULL COMMENT '市盈率'`
- `listing_date DATE DEFAULT NULL COMMENT '上市日期'`

A 股继续使用 `pe_dynamic`；美股使用 `pe_ratio`。缺失的 `circulating_market_value` 可保持 NULL。

### 4. 回调处理与 A 股保持同构

server 接收 `us-stock-market-refresh-result/v1` 后：

1. 校验任务存在且类型为 `us_stock_market_refresh`。
2. 校验 schema version。
3. 校验幂等键并保存 `processing_callback`。
4. 对 `succeeded` 和 `partial_failed` 分批 upsert 股票。
5. 保存 agent warning 和字段解析 warning 到 `processing_log.module = us_stock_market_refresh`。
6. 更新任务终态。

重复回调或任务已终态时返回当前任务状态，不重复写入。

### 5. 配置项独立

新增配置建议：

- `holdlens.agent.us-stock-market-refresh-url`
- `holdlens.agent.us-stock-market-callback-url`

默认值可与 A 股保持本地开发风格：

- `http://127.0.0.1:8765/tasks/us-stock-market-refresh`
- `http://127.0.0.1:8091/internal/agent/us-stock-market-refresh/callback`

### 6. 安全与隐私

创建请求仅允许 `trigger` 这类安全触发摘要，不接收账户、持仓、资产金额或凭据。`task_params_json` 建议只保存：

```json
{"market":"US_STOCK","trigger":"manual"}
```

`processing_log.message` 和 `error_summary` 必须做长度限制和换行清洗。

## Risks / Trade-offs

- [重复代码增加] -> 第一版接受与 A 股并列实现；如果 A 股、美股、港股三条链路稳定后，再单独设计通用市场刷新抽象。
- [payload 过大] -> 美股全量记录更多，第一版沿用一次性回调；若 HTTP 限制成为真实问题，再新增分片契约。
- [字段语义冲突] -> 明确新增 `pe_ratio`，不把美股市盈率写入 A 股 `pe_dynamic`。
- [交易所映射不稳定] -> 第一版允许 `exchange_code = null`，保留 `provider_market_code` 供排障。

## Open Questions

当前无待确认事项。实现前需要用户明确授权生产代码、测试和 SQL 变更。
