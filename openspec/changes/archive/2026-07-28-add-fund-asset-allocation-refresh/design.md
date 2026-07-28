## Context

现有基金切片刷新已经提供目录、申购状态、阶段收益和前十大重仓的独立 `processing_task`、agent dispatch/callback、目标发现、事务写入与详情查询。资产配置与前十大重仓是不同业务概念：前者描述股票、债券、现金等资产类别占比，后者描述公开披露的具体股票排名，因此不能复用 `fund_top_holding` 表、字段或 DTO。

server 是长期业务事实源，负责基金目标选择、任务状态、callback 幂等、快照新旧比较、原子覆盖和详情暴露；agent 只探测外部报告期、清洗并返回结构化结果。新增链路固定为：

| 项目 | 值 |
| --- | --- |
| task type | `fund_asset_allocation_refresh` |
| dispatch path | `/tasks/fund-asset-allocation-refresh` |
| callback path | `/internal/agent/fund-asset-allocation-refresh/callback` |
| task schema | `fund-asset-allocation-refresh-task/v1` |
| result schema | `fund-asset-allocation-refresh-result/v1` |
| callback idempotency key | `<server_task_id>:result:1` |

实现继续遵守 `Trigger -> API -> Case -> Domain <- Infrastructure`。Domain 定义实体和 Repository/Port 接口；Infrastructure 的 `adapter/repository` 只通过 `dao` 与 Mapper XML 访问数据库，`adapter/port` 负责 agent HTTP；Trigger 只做认证、转换和 cron 路由。当前项目使用 Java 17、Spring Boot 3.4.3、MyBatis 和 MySQL，未使用 Flyway/Liquibase。

本变更不直接关联 PRD。已确认方案与成功标准完整，当前无待确认事项，并已获得创建 OpenSpec 和实现代码的明确授权。

## Goals / Non-Goals

**Goals:**

- 建立独立基金资产配置任务、调度、dispatch、callback、持久化和详情响应，不改变前十大重仓语义。
- 只把合法、非空、可用的资产配置写成当前有效快照，并以报告期和规范化内容决定替换、修正或 no-op。
- 任何空结果、解析/网络失败、较旧报告期、未知基金或显式 unavailable 都不得删除已有有效快照。
- 用 `available/unavailable/missing` 区分已保存有效快照、数据源明确不可用和尚无可信结果，并对 unavailable 实施至少 7 天重试间隔。
- 为重建环境和已有环境提供一致 DDL；通过测试验证事务、幂等、详情映射、dispatch 路由、调度和敏感信息保护。

**Non-Goals:**

- 不保存多个历史报告期，不计算资产配置变化趋势，不把配置占比推导为具体证券持仓。
- 不改变 `fund_top_holding`、`topHoldings` 或相关报告期/抓取时间字段。
- 不在 server dispatch 中指定报告期，不让 agent 直接写 server 数据库。
- 不增加逐基金 `processing_task_item`、精确失败续跑或同步阻塞详情页的外部回源。
- 不自动启动、重启或部署后端应用。

## Decisions

### 1. 使用独立任务类型并复用现有基金切片基础设施

新增 `fund_asset_allocation_refresh` 常量、schema/path 映射、Controller callback 入口、Case 调度/派发入口、Port 路由和 Job 配置。通用 `processing_task`、callback 幂等、日志及状态机继续复用，但新任务拥有独立开关、cron、URL、callback path 和 DTO 字段。Trigger 同时暴露 `POST /api/agent/fund-asset-allocation-refresh/schedule-runs` 手动入口，该入口直接委托 Case 并绕过 cron 开关，同时保留 batch size 和同类型非终态任务跳过规则。

备选方案是把资产配置塞入 `fund_top_holding_refresh`，会让任一数据源失败影响另一 slice，并使空数组和报告期语义冲突，因此不采用。

### 2. 目标发现复用重仓人群规则，再应用资产配置新鲜度过滤

基础目标仍是已知基金中的并集：当前有效资产主数据/关注基金、真实 active 持仓基金、近 90 天详情查看基金。Mapper 使用同一并集规则，但增加资产配置候选条件：

- `missing` 或状态为空：可进入本周任务；
- `available` 且 `asset_allocation_as_of` 早于最近一个已结束自然季度末：可进入任务；
- `unavailable`：只有 `asset_allocation_fetched_at` 为空或早于当前时间 7 天及以上才可重试。

最近已结束季度由 server 按 `Asia/Shanghai` 自然季度计算，只用于判断是否值得请求；dispatch payload 不携带报告期，agent 自行探测数据源实际最新报告期。每周任务按独立可配置 batch size 切分，默认 20；本轮开始前已有同类型非终态任务时整轮跳过。

备选方案是每周刷新目标并集中的全部基金，虽简单但会对 unavailable 和已是最新季度的基金产生无效调用，因此采用候选过滤。

### 3. `fund` 保存元数据，独立子表保存当前配置明细

`fund` 新增：

- `asset_allocation_as_of DATE NULL`；
- `asset_allocation_status VARCHAR(20) NOT NULL DEFAULT 'missing'`，限定 `available/unavailable/missing`；
- `asset_allocation_fetched_at DATETIME NULL`。

新增 `fund_asset_allocation` 当前快照表，包含 `fund_code`、`asset_type`、`asset_type_name`、`allocation_ratio DECIMAL(12,4)`、`display_order`、创建/更新时间；以 `fund_code + asset_type + asset_type_name` 唯一。该约束允许多个不同原始名称映射为同一标准类型（尤其是 `unknown`）而不互相覆盖，同时禁止同一标准类型与原始名称组合重复。`allocation_ratio` 单位为百分点，例如 `35.6700` 表示 35.67%。详情查询在 Infrastructure 中分别查询基金、重仓和资产配置子表，再聚合为领域当前快照。

备选方案是把配置明细序列化到 `fund` JSON 字段，难以保证精度、排序、唯一性和可查询性，因此不采用。

### 4. 领域 Case 决定覆盖，Repository 保证单基金原子提交

callback item 先校验基金代码、报告期、状态和明细：

- `available` 必须有合法报告期和至少一条合法明细；占比按 `BigDecimal` 保存，标准类型、原始类型名称为空、占比为空/负数或 `display_order < 1` 的行无效；以 `asset_type + asset_type_name` 组合按输入最后一条去重，最终按 `display_order`、标准类型、原始类型名称稳定排序。相同标准类型但原始名称不同的行分别保留。
- incoming 报告期晚于当前报告期：替换当前明细并更新 `fund` 元数据。
- incoming 与当前同报告期且规范化内容相同：业务 no-op，不重复删除/插入。
- incoming 与当前同报告期但内容不同：视为数据源修正，原子替换。
- incoming 早于当前报告期：warning 并保留当前值。
- `unavailable`：若本地已有 `available` 有效快照则完全保留；只有从未有有效快照时才把状态和抓取时间更新为 unavailable。
- `missing`、普通空明细、报告期/行解析失败、网络失败：warning 或失败终态，不覆盖任何已有状态或明细。
- 未知基金：warning 并跳过。

有效替换调用 Domain Repository 的单一方法；Infrastructure Repository 在同一 Spring 事务中先更新 `fund` 元数据，再删除该基金旧配置并批量插入新明细。任一步异常使 callback 幂等记录、明细、元数据、日志和任务终态整体回滚。通常的新旧报告期业务决策由 Case 完成，Repository 只额外执行防并发回退的原子 guard。

为防止两个不同任务 callback 并发时旧报告期回退新快照，Repository 在事务内先以 `SELECT ... FOR UPDATE` 锁定 `fund` 行并再次比较报告期；元数据 UPDATE 同时带 `asset_allocation_as_of IS NULL OR asset_allocation_as_of <= incoming` guard。锁内发现数据库报告期已更新得更晚时，不执行删除/插入，并让 Case 记录 stale warning 后按 no-op 收口。

### 5. callback 复用任务级状态语义并加强新契约幂等校验

callback 只接受 `succeeded/partial_failed/failed` 和精确 `server_task_id:result:1` 幂等键。任务级 failed 不尝试写业务快照；非 failed 逐基金处理。可信 unavailable 计为已接受成功结果；全 unavailable 不会因零明细被改判 failed。全 in-flight 且携带 `fund_already_inflight` warning 时按成功 no-op。有效与真实跳过/失败并存时 partial_failed，其余 succeeded。重复成功 callback 命中既有幂等记录后返回既有任务结果，不重复写入。

外部 warning/error 进入现有安全截断与敏感键脱敏逻辑；日志和 `task_params_json` 不保存凭据、报告原文或个人资产明细。dispatch 只包含 schema、server task id、基金代码、网络许可与 callback URL，不包含目标报告期。

### 6. 详情 API 使用独立字段，不触发语义复用

领域结果、API DTO 和 Controller 映射新增：

- `assetAllocationAsOf`；
- `assetAllocationStatus`；
- `assetAllocationFetchedAt`；
- `assetAllocations[{assetType, assetTypeName, allocationRatio, displayOrder}]`。

配置列表按 `displayOrder`、`assetType`、`assetTypeName` 返回。空列表与 `missing/unavailable` 状态保持可解释，不命名为 position、holding 或 topHolding，也不参与股票行情拼接。

基金详情读取数据库后独立判断资产配置是否需要 best-effort 异步回源：missing/空状态触发；available 但报告期早于最近已结束季度触发；unavailable 只有认可抓取时间已满 7 天才触发。单基金详情和持仓基金详情都使用独立基金代码列表调用资产配置 dispatch，不与 top holding stale 列表混用；派发失败只记录安全日志，不阻塞详情响应。

### 7. SQL 同时维护重建基线和前向迁移

直接更新 `docs/dev-ops/mysql/sql/holdlens.sql`，并新增 `docs/dev-ops/mysql/sql/migrations/20260716_fund_asset_allocation_refresh.sql`。迁移只增加兼容列、子表、唯一键和查询索引，不删除/重命名已有列，不改动 `fund_top_holding` 数据。回滚应用时可关闭新 Job；新增兼容表列保留，避免破坏已保存快照。

## Risks / Trade-offs

- [同一基金并发 callback 可能交叉覆盖] → 有效替换在单事务中更新 `fund` 行并同步子表；callback 幂等与同类型非终态调度降低重复，并用并发/事务测试覆盖关键不变量。
- [同报告期数据源修正会改变已展示结果] → 明确允许同期内容修正，保持原子替换并记录任务 warning/审计时间。
- [自然季度结束不等于数据已公开] → server 只把基金纳入每周候选，实际报告期由 agent 探测；旧/空/unavailable 响应均保护历史。
- [状态与明细发生不一致] → 只有 Repository 原子替换方法能写 available；unavailable 仅在无历史有效数据时写入；详情测试核对状态与列表。
- [批量删除后插入失败] → callback 事务回滚恢复旧明细和元数据，不暴露部分快照。
- [外部 callback 携带敏感文本] → 沿用 callback header 鉴权、长度限制和敏感键脱敏；不记录完整 payload。
- [新增子表增加详情查询] → 使用基金代码索引、批量查询和内存聚合，避免逐基金 N+1。

## Migration Plan

1. 先部署支持 `/tasks/fund-asset-allocation-refresh` v1 的 agent，保持 server 不调用。
2. 执行前向 migration，核对 `fund` 新列默认状态和 `fund_asset_allocation` 唯一键；已有基金、收益和重仓数据保持不变。
3. 发布 server 新任务、callback 和详情字段，保持资产配置 Job 默认关闭。
4. 手动验证小批基金的 dispatch/callback、同报告期修正、旧报告期与 unavailable 保留后，再启用每周 cron。
5. 回滚时关闭独立 Job 并回退应用；保留兼容新增表列，不恢复或修改重仓链路。

## Open Questions

当前无待确认事项。
