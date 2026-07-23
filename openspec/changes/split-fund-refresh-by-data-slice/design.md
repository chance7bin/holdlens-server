## Context

当前 `AgentRefreshScheduleJob` 同时依赖 `IAgentFundRefreshCase` 和 `IFundDataRepository`，通过 `fund.id` keyset 扫描整张 `fund` 表，逐批创建 `fund_detail_refresh`。`AgentFundRefreshCaseImpl` 的单次回调把目录名称、申购状态、阶段收益、重仓元数据和 `fund_top_holding` 一并保存；`fund_mapper.xml` 的统一 upsert 也会同时覆盖多个 slice。该结构在基金数量较少时可运行，但完整基金目录导入后会把全市场基金都纳入逐基金重仓请求，并让任一外部接口失败影响其他数据。

server 是长期业务事实源，负责调度、刷新目标选择、`processing_task` 状态、callback 幂等、写入取舍和回滚；agent 只负责外部数据源访问、清洗和结构化结果。跨项目契约由根目录 `contracts/` 维护，本设计固定使用四组契约：

| task type | agent dispatch | server callback | task schema | result schema |
| --- | --- | --- | --- | --- |
| `fund_catalog_refresh` | `/tasks/fund-catalog-refresh` | `/internal/agent/fund-catalog-refresh/callback` | `fund-catalog-refresh-task/v1` | `fund-catalog-refresh-result/v1` |
| `fund_purchase_status_refresh` | `/tasks/fund-purchase-status-refresh` | `/internal/agent/fund-purchase-status-refresh/callback` | `fund-purchase-status-refresh-task/v1` | `fund-purchase-status-refresh-result/v1` |
| `fund_period_return_refresh` | `/tasks/fund-period-return-refresh` | `/internal/agent/fund-period-return-refresh/callback` | `fund-period-return-refresh-task/v1` | `fund-period-return-refresh-result/v1` |
| `fund_top_holding_refresh` | `/tasks/fund-top-holding-refresh` | `/internal/agent/fund-top-holding-refresh/callback` | `fund-top-holding-refresh-task/v1` | `fund-top-holding-refresh-result/v1` |

当前 `fund` 是基金当前数据表，`fund_top_holding` 以 `fund_code + rank_no` 保存当前重仓；`asset_info` 保存用户关注/持仓涉及的资产主数据，`asset_holding` 标识当前持仓。现有详情查询根据通用 `update_time` 判断陈旧，并且只读数据库。变更后继续保持详情查询不阻塞外部接口，但改用 slice 级抓取时间表达新鲜度。

本变更不直接关联 `docs/requirements/**/prd-*.md`。实现仍遵循 `Trigger -> API -> Case -> Domain <- Infrastructure`：Trigger 只接收 HTTP/cron 并调用 Case；Case 编排任务与事务；Domain 定义刷新目标、写入规则及 Port/Repository；Infrastructure 的 `adapter/port` 调 agent gateway，`adapter/repository` 调 DAO；MyBatis SQL 只放 Mapper XML。

## Goals / Non-Goals

**Goals:**

- 四个基金数据 slice 独立创建任务、调度、回调、幂等、写入和失败，不再由复合回调互相牵连。
- 全目录基金只参加全市场接口刷新；逐基金重仓只刷新当前持有、关注、近 90 天查看和详情页临时请求的基金。
- 任何空、无效、较旧或失败响应都不清除仍有效的基金当前数据。
- 区分数据日期和抓取时间，使页面能够明确展示持仓报告期，同时由 server 判断是否需要异步回源。
- 支持重建初始化 SQL 与无迁移框架环境下的前向 ALTER，并提供可分阶段启用、可关闭调度的发布路径。

**Non-Goals:**

- 不保存每日净值/收益历史序列，不保存历史重仓报告期、全量股票持仓或债券持仓。
- 不使用基金公告接口判断新报告，不做跨数据源自动 fallback。
- 不新增 `processing_task_item`、持久化逐基金进度、失败基金自动精确重试或可恢复调度游标。
- 不让详情请求同步等待 agent，不把 agent 变成基金业务事实源。
- 不在本轮 OpenSpec 产出中修改业务代码、测试、`holdlens.sql` 或 migration 文件。

## Decisions

### 1. 以四个 Case/Port 能力替代复合基金刷新

四个 task type 各有独立的创建/派发/回调用例与 agent Port 能力。Controller 和 Job 可以按同一模式组织，但不得把四个结果重新合并为一个写入事务。通用 `processing_task` Repository、状态机、日志和 callback 幂等组件继续复用。

Trigger 不直接读取 Repository 或调用 agent Port。定时 Job 只检查开关并调用对应的调度 Case；目标查询、批次切分、非终态检查和任务创建由 Case 编排。这样修正现有 Job 跨过 Case 直接依赖 `IFundDataRepository` 的边界，同时避免在 Trigger 中沉淀业务规则。

备选方案是保留一个 Case 并传入 slice 枚举。该方案会把四类不同目标、数据校验和事务规则重新集中成条件分支，削弱独立演进能力，因此不采用。

### 2. 三类全市场任务每日一次，重仓每月 1 日和 15 日

所有调度使用 `Asia/Shanghai` 自然日，不引入交易日历：

- 目录、申购状态、阶段收益分别每天一次，每次创建一个全市场任务。
- 重仓在每月自然日 1 日、15 日启动，按默认 20 个基金代码切分任务。
- 四个调度分别具有 `enabled` 和 `cron`；新配置默认关闭。重仓另有 `batch-size`，默认 20。
- 在新一轮启动前发现同 task type 存在非终态任务时跳过该轮；该检查不作用于已经开始的当前轮，因此 server 可先创建当前轮全部重仓批次。重仓批次的外部执行并发上限为 2，由 agent 固定 worker/信号量控制，server 记录各批次状态。

全市场任务的结果与前一次完全相同时，Repository 通过数据日期和内容比较执行 no-op，不依赖“非交易日”判断。备选方案是接入交易日历，但基金目录和申购状态并非只在交易日变化，且会增加日历维护成本。

### 3. 重仓目标使用持有/关注/近 90 天查看的并集

重仓目标查询只返回以下去重后的已知基金代码：

1. `asset_holding.status = active` 关联的基金资产；
2. `asset_info.status = enabled AND asset_kind = fund` 中已关注或已进入资产主数据的基金；
3. `fund.last_detail_view_time >= now - 90 days` 的基金。

`asset_info` 目前同时承载关注资产和持仓资产主数据，因此查询并集时以 `asset_holding` 明确“当前持有”，以有效基金 `asset_info` 覆盖“关注”，最终按基金代码去重。若后续关注关系独立建模，应通过另一个 change 调整目标查询，不在本次额外拆表。

详情查询命中任意基金时，在 `fund.last_detail_view_time` 记录全局最近查看时间，不保存新的用户访问明细，避免为了调度目标扩散个人访问历史。若该基金重仓缺失或 `top_holding_fetched_at` 超过可配置阈值（默认 15 天），详情 Case 在返回数据库快照后 best-effort 创建单基金异步 `fund_top_holding_refresh`；同基金已在 agent 进程内执行时由 agent 内存集合去重。异步派发失败只记录安全日志，不使详情查询失败。

备选方案“全目录基金每月逐只刷新”调用量随目录线性放大；备选方案“每次详情同步实时查”会把第三方延迟和故障传给用户请求，均不采用。

### 4. `fund` 保持当前快照主表，各 slice 只写自己的列

不拆四张当前快照表。`fund` 继续以 `fund_code` 唯一，并补齐：

- 目录：`fund_type`、`pinyin_abbr`、`pinyin_full`、`catalog_fetched_at`；
- 申购：现有 `buy_status`、`daily_purchase_limit`、新增 `purchase_status_fetched_at`；
- 收益：`return_coverage_status`、`returns_as_of`、`unit_nav`、`accumulated_nav`、`daily_growth_rate`、现有五个阶段收益、`period_return_fetched_at`；
- 重仓：现有 `top_holdings_as_of`、`public_holdings_status`、新增 `top_holding_fetched_at`；
- 目标辅助：`last_detail_view_time`。

`*_as_of` 是外部数据业务日期，`*_fetched_at` 是 server 成功接收并认可该 slice 的抓取时间；通用 `update_time` 只用于技术审计，不再判断任何 slice 新鲜度。

目录 Repository 使用 slice 专用 upsert，可插入新基金并只更新目录列。申购、收益和重仓 Repository 使用 `UPDATE ... WHERE fund_code = ?`，不插入未知代码；影响行数为 0 时产生 warning 并跳过。四个 Mapper 方法不得调用现有会覆盖多 slice 的统一 `fund.upsert`。

备选方案是拆为四张一对一表，隔离性更强但会增加详情查询 join、DTO 聚合和迁移复杂度；当前均为同一基金的唯一“当前值”，使用专用 SQL 已能获得写隔离。

### 5. 每类回调按完整响应判定后原子写入

回调入口先校验 `server_task_id`、task type、schema version 和 idempotency key，再在一个业务事务内执行“首次 callback 记录 + 有效 slice 写入 + warning/日志 + 任务终态”。规则如下：

- 全市场接口整体失败、返回空集合或不可解析：任务 `failed`，不写该 slice。
- 返回部分有效：只写有效基金；无效行和未知代码进入 warning；任务 `partial_failed`。
- 全部有效：任务 `succeeded`。与库中数据日期和内容相同的记录 no-op，不视为失败。
- 重仓批次仅因进程内 `inflight_fund_codes` 与其他执行中任务完全重叠而返回空 `funds`，且带有约定的 `fund_already_inflight` warning 时：任务 `succeeded` 且业务写入 no-op；这不是外部接口异常空结果。
- 目录结果中未出现的存量基金不删除、不禁用、不清空；异常空结果也不会把目录当成全量删除指令。
- `source_not_covered` 是收益记录的有效覆盖状态，不属于失败；它只更新该基金的覆盖状态与抓取时间，不伪造收益值。

如果写入、callback 幂等或任务终态任一步失败，整个业务事务回滚并返回非 2xx；任务保持原非终态，让 agent 按立即、10 秒、60 秒重试相同 idempotency key。server 另设超时收口任务，在可配置窗口后把仍未完成 callback 的任务标记为 `callback_failed`，从而解除下一轮调度阻塞。超时默认 30 分钟，并且配置校验必须保证它覆盖预期最长任务执行时间与完整 callback 重试窗口，避免 20 只基金重仓批次仍在执行时被提前终结。该行为只适用于四个新 task type；实现时不得沿用现有复合回调“业务事务失败后立即以独立事务写 callback_failed”的路径。

重复成功 callback 发现幂等记录已成功时只返回既有任务结果，不重复 slice 写入。`dispatch_failed` 表示派发阶段失败；正常流转为 `created -> dispatched/running -> succeeded|partial_failed|failed`，超时收口为 `callback_failed`。

### 6. 重仓以报告期优先并保护旧快照

`fund_top_holding_refresh` 对每只基金独立比较：

- incoming `top_holdings_as_of` 晚于库中日期且持仓列表非空：同步排名 1 至 10，更新元数据与抓取时间；
- incoming 日期相同、规范化内容相同：no-op，可更新抓取时间；
- incoming 日期相同、内容不同：视为数据源修正，在同一事务内同步当前前十大并更新抓取时间；
- incoming 日期早于库中日期、日期缺失、解析失败或列表为空：保留旧元数据和 `fund_top_holding`，产生 warning；
- 数据源明确返回有效报告期的 `no_public_stock_holdings`：仅当报告期新于或等于当前报告期时更新状态并清空当前重仓；普通空列表仍不得清空；
- incoming 基金未知：warning 并跳过。

同步继续以 `fund_code + rank_no` 为当前重仓身份，但删除旧排名只能发生在“已验证的非空新快照替换/同期修正”中。绝不把空数组解释为清空指令。页面始终返回 `top_holdings_as_of`，明确这是最近公开报告期而非实时持仓。

### 7. 收益仅保存当前快照和覆盖状态

收益 slice 保存单位净值、累计净值、日增长率、近 1 月/3 月/6 月/1 年/3 年收益及 `returns_as_of`。不新增日序列表。agent 对全市场接口未覆盖的基金类型返回 `source_not_covered`；server 将其作为显式覆盖状态保存，不将该行计入失败，也不把缺失收益字段写成零。

同一 `returns_as_of` 且内容一致执行 no-op；更新日期或同期源修正只影响收益列。缺少基金代码/覆盖状态或本应 covered 却整体字段不可解析的记录进入 warning，保留旧收益。

### 8. 首期只维护批次任务，不维护逐基金任务项

每个重仓批次对应一个 `processing_task`，安全参数摘要只保存基金代码列表、trigger 和 batch 信息；不创建 `processing_task_item`。callback warnings 可指出失败基金，但 server 不承诺逐基金进度查询、崩溃恢复或自动精确补偿。

这是对复杂度的有意限制。代价是 agent/server 进程在本轮中断时不能从最后一只基金恢复，只能由下一次 1/15 调度或详情异步回源补偿。若实际失败率或任务规模证明需要可靠续跑，再单独设计 task item 与重试策略。

### 9. SQL 同时维护重建基线和前向迁移

后续实现必须同时：

- 直接更新 `docs/dev-ops/mysql/sql/holdlens.sql`，使新环境可重建；
- 新建 `docs/dev-ops/mysql/sql/migrations/20260712_fund_refresh_slices.sql`，以可重复核对的 ALTER/索引语句升级已有环境；
- 为目录字段、收益字段、四个抓取时间和 `last_detail_view_time` 提供一致定义；不创建 `processing_task_item`；
- 在迁移验证中分别从旧基线执行 ALTER、从新基线直接建库，并核对表结构和已有基金/重仓数据保留。

项目当前未引入 Flyway/Liquibase，因此 migration 作为显式运维脚本，不在应用启动时自动执行。

## Risks / Trade-offs

- [同一 `fund` 表仍可能被错误 SQL 跨 slice 覆盖] → 只允许 slice 专用 Mapper 方法，并为“其他 slice 字段保持不变”增加 Repository 测试。
- [重仓仅 1/15 调度可能晚于披露日] → 详情缺失/陈旧时异步回源；页面显示报告期，不承诺实时调仓。
- [无 task item 导致批内失败无法自动精确补偿] → warning 保留失败代码，旧数据不变；下一次调度或详情触发补偿。
- [agent 内存队列/去重在重启后丢失] → processing task 超时收口避免永久阻塞；首期接受 best-effort，未来按观测数据决定是否持久化。
- [全局 `last_detail_view_time` 不区分用户] → 只用于公共基金数据调度，不记录用户访问明细；减少隐私数据和查询复杂度。
- [目录全量接口异常缺行] → 只 upsert 返回行，永不以缺失行删除；监控返回数量和 `partial_failed`/`failed`。
- [callback 事务失败后短时间阻塞同类型调度] → agent 重试窗口内保持非终态是幂等一致性的代价；默认 30 分钟且覆盖最长执行与完整重试窗口后才标记 `callback_failed`。
- [旧 endpoint 仍被调用] → 分阶段发布并观测调用方，稳定后移除；完整目录导入后禁止回滚到旧全表复合调度。

## Migration Plan

1. 先发布实现四个任务 endpoint、schema 和重试/并发约束的 agent 版本，保持 server 不调用。
2. 发布包含新字段、前向 migration、四类 server Case/Port/callback 和默认关闭 Job 的 server；先执行并验证数据库迁移。
3. 明确关闭旧 `fund_detail_refresh` 定时任务，然后启用目录任务，完成首次全目录 upsert 并核对数量；此后禁止恢复旧全表复合调度。
4. 依次启用申购状态、阶段收益，验证独立字段和覆盖状态；再启用重仓 1/15 调度及详情异步回源。
5. 观察任务终态、warning、callback 重试、外部调用量和数据库 no-op 后，退役旧 endpoint、旧 Case/Port/DTO 和统一 upsert 路径。

回滚时只关闭对应新 Job 和详情异步回源，不回滚已经写入的兼容新增列；可回退 server 应用但不得重新开启旧“扫描完整 fund 表并逐基金复合刷新”的 Job。若某一 slice 有问题，只关闭该 slice，其他 slice 继续运行。

## Open Questions

当前无待确认事项。进入实现前仍需由用户明确授权；实现中若发现现有 `asset_info` 无法可靠区分“关注”与非持仓资产主数据，应停止扩大表意并提出独立的关注关系建模变更。
> 历史变更说明：本文的旧刷新目标表来源已由 `replace-account-holdings-with-asset-records` 替代；当前目标取 `watchlist_item` 与具体基金 `asset_record` 的去重并集。
