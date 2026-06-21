## Why

当前 server 的基金刷新落库模型仍围绕 `fund_detail_snapshot`、`fund_detail_item`、`fund_top_holding` 的快照批次展开，并把重仓股票的 `daily_return` 保存在基金重仓表中。经过根目录 ADR `docs/decisions/adr-001-fund-stock-refresh-boundary.md` 讨论收敛后，基金公开详情、基金当前重仓关系和股票当前行情应拆成不同事实归属：

- 基金详情和基金重仓关系属于基金公开数据。
- 股票涨跌幅属于股票行情数据。
- 刷新任务状态、失败、缺失和诊断属于 processing 域。

server 需要调整数据库 schema、回调处理、查询编排和 agent 集成契约，使基金刷新只更新基金相关表，股票刷新独立更新股票行情表。

## What Changes

- 删除 `fund_detail_snapshot`，不再按每次基金刷新保存完整快照批次。
- 将 `fund_detail_item` 收敛为基金当前详情表，以 `fund_code` 唯一。
- 将 `fund_top_holding` 收敛为基金当前重仓关系表，以 `fund_code + rank_no` 唯一。
- 从基金表删除来源追踪和缺失原因 JSON 字段：`field_sources_json`、`missing_reasons_json`；`data_sources_json` 随 `fund_detail_snapshot` 删除。
- 从 `fund_top_holding` 删除 `fund_detail_item_id`、`snapshot_id`、`daily_return`、`missing_reasons_json`，新增或保留 `fund_code`。
- 新增股票当前市场数据表，表名确定为 `stock_market_current`，以 `stock_code + market` 唯一。
- 新增 `stock_quote_refresh` 处理任务、下发 agent、接收回调和 upsert 股票行情能力；股票刷新范围直接取 `stock_market_current` 表内所有股票。
- 升级基金刷新回调契约，基金结果不再接收 `top_holdings.daily_return`、`data_sources`、`field_sources`、`missing_reasons`。
- 查询账户基金详情时，从 `portfolio` 持仓关联 `fund_detail_item`、`fund_top_holding` 和 `stock_market_current`。

## Capabilities

### New Capabilities

- `fund-stock-refresh-persistence`: server 将基金当前详情、基金当前重仓关系和股票当前行情拆分持久化，并支持基金/股票两类 agent 刷新任务。

### Modified Capabilities

- `agent-async-fund-refresh`: 基金刷新回调结果改为当前表 upsert，不再写入 `fund_detail_snapshot`。
- `processing-schema`: `processing_task` 支持 `stock_quote_refresh`，`processing_log` 统一记录基金和股票刷新诊断。
- `portfolio-fund-detail-query`: 查询链路增加股票行情关联，且不再依赖基金快照排序。

## Impact

- 影响 server 数据库初始化 SQL、PO、DAO、Mapper XML、Repository、领域模型和 Case 编排。
- 影响 server API/Trigger：基金刷新回调 DTO 需要瘦身，新增股票刷新任务与回调 DTO。
- 影响 agent 集成：需要与 agent change `split-fund-stock-refresh-tasks` 对齐 schema version、请求和回调 payload。
- 影响测试：需要覆盖基金当前表 upsert、重仓覆盖、股票行情 upsert、重复回调幂等、组合查询和安全日志。
- 影响兼容：当前阶段直接修改数据库初始化 SQL，不做历史快照在线迁移，也不兼容旧 agent 回调字段。

## Non-Goals

- 不实现基金详情历史快照回放。
- 不保存每次刷新得到的完整历史基金详情和重仓列表。
- 不在 server 中实现东方财富、天天基金、AkShare 等外部 provider。
- 不实现股票历史行情表；第一阶段只维护当前行情。
- 不引入字段级来源追踪或字段级缺失原因展示。
