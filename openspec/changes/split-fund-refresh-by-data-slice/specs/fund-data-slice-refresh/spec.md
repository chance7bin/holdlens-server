## ADDED Requirements

### Requirement: 四类基金刷新任务具有独立契约

server SHALL 将基金目录、申购状态、阶段收益和前十大重仓作为四类独立任务处理，且每类任务只能写入自己的数据 slice。

#### Scenario: 创建基金目录刷新任务
- **WHEN** server 创建 `fund_catalog_refresh` 任务
- **THEN** server SHALL 向 `/tasks/fund-catalog-refresh` 派发 `fund-catalog-refresh-task/v1`
- **AND** server SHALL 只在 `/internal/agent/fund-catalog-refresh/callback` 接受 `fund-catalog-refresh-result/v1`

#### Scenario: 创建基金申购状态刷新任务
- **WHEN** server 创建 `fund_purchase_status_refresh` 任务
- **THEN** server SHALL 向 `/tasks/fund-purchase-status-refresh` 派发 `fund-purchase-status-refresh-task/v1`
- **AND** server SHALL 只在 `/internal/agent/fund-purchase-status-refresh/callback` 接受 `fund-purchase-status-refresh-result/v1`

#### Scenario: 创建基金阶段收益刷新任务
- **WHEN** server 创建 `fund_period_return_refresh` 任务
- **THEN** server SHALL 向 `/tasks/fund-period-return-refresh` 派发 `fund-period-return-refresh-task/v1`
- **AND** server SHALL 只在 `/internal/agent/fund-period-return-refresh/callback` 接受 `fund-period-return-refresh-result/v1`

#### Scenario: 创建基金重仓刷新任务
- **WHEN** server 创建 `fund_top_holding_refresh` 任务
- **THEN** server SHALL 向 `/tasks/fund-top-holding-refresh` 派发 `fund-top-holding-refresh-task/v1`
- **AND** server SHALL 只在 `/internal/agent/fund-top-holding-refresh/callback` 接受 `fund-top-holding-refresh-result/v1`

#### Scenario: 拒绝错误任务类型或 schema
- **WHEN** callback 的 task type、endpoint 或 schema version 与 server 任务记录不一致
- **THEN** server SHALL 拒绝该 callback
- **AND** server SHALL NOT 写入任何基金 slice

### Requirement: 四类刷新按自然日独立调度

server SHALL 在 `Asia/Shanghai` 时区提供四个默认关闭且可独立配置的调度入口，不依赖证券交易日历。

#### Scenario: 每日调度全市场接口
- **WHEN** 基金目录、申购状态或阶段收益调度已启用并到达各自每日 cron
- **THEN** server SHALL 分别创建一个对应 task type 的全市场刷新任务
- **AND** 任一任务的派发或执行 SHALL NOT 阻止其他 task type 的调度

#### Scenario: 每月一日和十五日调度重仓
- **WHEN** 重仓调度已启用
- **AND** `Asia/Shanghai` 自然日期为当月 1 日或 15 日且到达 cron
- **THEN** server SHALL 查询本次重仓目标并创建 `fund_top_holding_refresh` 批次

#### Scenario: 非重仓调度日不创建批次
- **WHEN** `Asia/Shanghai` 自然日期不是当月 1 日或 15 日
- **THEN** server SHALL NOT 因定时调度创建重仓刷新批次

#### Scenario: 调度默认关闭
- **WHEN** 新版本首次以默认配置启动
- **THEN** 目录、申购状态、阶段收益和重仓调度 SHALL 全部保持关闭
- **AND** server SHALL NOT 自动调用新 agent endpoint

#### Scenario: 同类型任务未结束时跳过新一轮
- **WHEN** 任一调度准备创建任务
- **AND** 在本轮启动前相同 task type 已存在非终态 `processing_task`
- **THEN** server SHALL 跳过该 task type 的本轮调度
- **AND** 其他 task type SHALL 仍可独立调度

#### Scenario: 当前重仓轮次创建全部批次
- **WHEN** 重仓调度本轮启动时不存在历史非终态同类型任务
- **AND** 本轮目标需要创建多个批次
- **THEN** server SHALL 允许创建本轮全部 `fund_top_holding_refresh` 批次
- **AND** 本轮先创建的非终态批次 SHALL NOT 阻止本轮后续批次创建

### Requirement: 重仓定时刷新只覆盖活跃目标基金

server SHALL 将当前持有、关注和近 90 天查看基金的并集作为重仓定时刷新范围，而不是扫描完整 `fund` 目录。

#### Scenario: 纳入当前持有基金
- **WHEN** 有效基金资产关联 `status = active` 的当前持仓
- **THEN** server SHALL 将该基金代码纳入重仓刷新目标

#### Scenario: 纳入关注基金
- **WHEN** 有效 `asset_info` 表示 `asset_kind = fund` 且 `status = enabled` 的关注基金
- **THEN** server SHALL 将该基金代码纳入重仓刷新目标

#### Scenario: 纳入近九十天查看基金
- **WHEN** `fund.last_detail_view_time` 不早于当前时间前 90 天
- **THEN** server SHALL 将该基金代码纳入重仓刷新目标

#### Scenario: 排除仅存在于目录的基金
- **WHEN** 基金只存在于完整目录
- **AND** 该基金不属于当前持有、关注或近 90 天查看范围
- **THEN** server SHALL NOT 将其纳入 1 日或 15 日重仓定时刷新

#### Scenario: 目标按基金代码去重
- **WHEN** 同一基金同时属于持有、关注和近期查看中的多个集合
- **THEN** server SHALL 在本轮目标集合中只保留一个基金代码

### Requirement: 详情查询先返回数据库快照并异步回源

server SHALL 让基金详情查询始终优先返回数据库数据，且只以 best-effort 异步任务刷新缺失或陈旧的重仓。

#### Scenario: 查询已有基金详情
- **WHEN** 用户查询已入库基金详情
- **THEN** server SHALL 立即返回当前数据库快照
- **AND** server SHALL 更新该基金的全局 `last_detail_view_time`
- **AND** server SHALL NOT 同步等待外部数据源

#### Scenario: 重仓缺失时异步刷新
- **WHEN** 用户查询基金详情
- **AND** 该基金没有有效当前重仓快照
- **THEN** server SHALL 在返回详情后 best-effort 创建单基金 `fund_top_holding_refresh`
- **AND** 返回结果 SHALL 表明重仓正在刷新或当前缺失

#### Scenario: 重仓抓取时间陈旧时异步刷新
- **WHEN** 用户查询基金详情
- **AND** `top_holding_fetched_at` 早于配置的陈旧阈值且默认阈值为 15 天
- **THEN** server SHALL 在返回旧快照后 best-effort 创建单基金 `fund_top_holding_refresh`
- **AND** 返回结果 SHALL 继续携带旧快照的 `top_holdings_as_of`

#### Scenario: 异步派发失败不影响详情响应
- **WHEN** 详情查询已获得数据库快照
- **AND** 异步重仓任务创建或派发失败
- **THEN** server SHALL 仍成功返回数据库快照
- **AND** server SHALL 记录不含敏感信息的失败日志

### Requirement: 基金目录按返回行非破坏性 upsert

server SHALL 允许目录 slice 插入新基金并更新基金代码、名称、类型、拼音简称、完整拼音和目录抓取时间，且不得把全量响应的缺失行当成删除指令。

#### Scenario: 首次导入完整目录
- **WHEN** 有效目录 callback 返回数据库中不存在的基金代码
- **THEN** server SHALL 插入对应 `fund` 记录
- **AND** server SHALL 只初始化目录字段及技术审计字段

#### Scenario: 更新已存在目录行
- **WHEN** 有效目录 callback 返回已存在基金代码
- **THEN** server SHALL 更新该基金的目录字段和 `catalog_fetched_at`
- **AND** server SHALL NOT 修改申购、收益或重仓 slice 字段

#### Scenario: 全量结果缺少存量基金
- **WHEN** 有效非空目录 callback 未包含某个已存在基金
- **THEN** server SHALL 保留该基金记录及全部字段
- **AND** server SHALL NOT 删除、禁用或清空该基金

#### Scenario: 目录整体为空或不可解析
- **WHEN** agent 报告目录外部接口整体失败、返回空集合或结果不可解析
- **THEN** server SHALL 将任务标记为 `failed`
- **AND** server SHALL NOT 写入任何目录记录

### Requirement: 申购状态只更新已知基金的申购 slice

server SHALL 只以申购状态 callback 更新已存在基金的 `buy_status`、`daily_purchase_limit` 和 `purchase_status_fetched_at`。

#### Scenario: 更新已知基金申购状态
- **WHEN** callback 包含已存在基金的有效申购状态
- **THEN** server SHALL 原子更新该基金的申购 slice
- **AND** server SHALL NOT 修改目录、收益或重仓 slice

#### Scenario: 跳过未知申购基金
- **WHEN** callback 包含数据库中不存在的基金代码
- **THEN** server SHALL 跳过该行并产生 warning
- **AND** server SHALL NOT 因申购 callback 插入基金记录

#### Scenario: 返回缺失不清空申购状态
- **WHEN** 某个存量基金未出现在申购状态全市场 callback 中
- **THEN** server SHALL 保留该基金已有申购状态和限额

### Requirement: 阶段收益只保存当前快照和覆盖状态

server SHALL 只保存基金当前单位净值、累计净值、日增长率、近 1 月/3 月/6 月/1 年/3 年收益、数据日期、覆盖状态和抓取时间，不保存每日历史序列。

#### Scenario: 保存已覆盖基金的收益快照
- **WHEN** callback 返回已知基金且 `coverage_status = covered` 的有效收益记录
- **THEN** server SHALL 更新该基金当前收益字段、`returns_as_of` 和 `period_return_fetched_at`
- **AND** server SHALL NOT 新增每日收益历史行

#### Scenario: 标记数据源不覆盖
- **WHEN** callback 将已知基金标记为 `source_not_covered`
- **THEN** server SHALL 将该状态作为有效结果保存
- **AND** server SHALL NOT 将该基金计为失败
- **AND** server SHALL NOT 伪造零值收益

#### Scenario: 跳过未知收益基金
- **WHEN** callback 包含数据库中不存在的基金代码
- **THEN** server SHALL 跳过该行并产生 warning
- **AND** server SHALL NOT 因收益 callback 插入基金记录

#### Scenario: 收益整体为空或不可解析
- **WHEN** agent 报告阶段收益接口整体失败、返回空集合或结果不可解析
- **THEN** server SHALL 将任务标记为 `failed`
- **AND** server SHALL 保留所有基金的已有收益快照

#### Scenario: 同日期同内容业务 no-op
- **WHEN** incoming `returns_as_of` 与当前日期相同且规范化收益内容相同
- **THEN** server SHALL NOT 重写收益业务字段
- **AND** server MAY 更新 `period_return_fetched_at` 以记录本次成功抓取

### Requirement: 重仓只保存最新公开报告期前十大股票

server SHALL 以 `top_holdings_as_of` 和规范化内容决定是否同步 `fund_top_holding`，且每只基金最多保存排名 1 至 10 的当前股票重仓及增减字段。

#### Scenario: 新报告期替换当前重仓
- **WHEN** 已知基金的 incoming `top_holdings_as_of` 晚于库中日期
- **AND** incoming 前十大列表有效且非空
- **THEN** server SHALL 在一个事务内同步该基金排名 1 至 10 的当前重仓
- **AND** server SHALL 更新 `top_holdings_as_of`、`public_holdings_status` 和 `top_holding_fetched_at`

#### Scenario: 同报告期同内容 no-op
- **WHEN** incoming 报告期与库中相同
- **AND** 规范化后的排名、股票、占比和增减内容相同
- **THEN** server SHALL NOT 重写重仓业务内容
- **AND** server MAY 更新 `top_holding_fetched_at`

#### Scenario: 同报告期允许数据源修正
- **WHEN** incoming 报告期与库中相同
- **AND** 规范化内容不同且 incoming 列表有效非空
- **THEN** server SHALL 将 incoming 视为数据源修正并原子同步当前前十大

#### Scenario: 较旧报告期保留当前重仓
- **WHEN** incoming 报告期早于库中 `top_holdings_as_of`
- **THEN** server SHALL 跳过该基金并产生 warning
- **AND** server SHALL 保留已有重仓元数据和子表记录

#### Scenario: 日期缺失或空列表不得清仓
- **WHEN** incoming 报告期缺失、解析失败，或 `public_holdings_status = public` 但前十大列表为空
- **THEN** server SHALL 跳过该基金并产生 warning
- **AND** server SHALL NOT 删除或清空已有 `fund_top_holding`

#### Scenario: 明确无公开股票持仓可以替换旧快照
- **WHEN** 数据源明确返回有效报告期的 `public_holdings_status = no_public_stock_holdings`
- **AND** incoming 报告期新于或等于当前报告期
- **THEN** server SHALL 更新该基金的报告期、状态和抓取时间
- **AND** server SHALL 清空该基金不再成立的当前 `fund_top_holding`

#### Scenario: 保存重仓增减字段
- **WHEN** 有效重仓行包含相对上期的变化类型和值
- **THEN** server SHALL 保存 `quarter_change_type` 和 `quarter_change_value`
- **AND** 页面 SHALL 能返回这些字段及 `top_holdings_as_of`

#### Scenario: 跳过未知重仓基金
- **WHEN** callback 包含数据库中不存在的基金代码
- **THEN** server SHALL 跳过该基金并产生 warning
- **AND** server SHALL NOT 因重仓 callback 插入基金记录

### Requirement: Slice 写入具有字段隔离和独立抓取时间

server SHALL 通过 slice 专用 Repository 和 Mapper XML 方法写入 `fund`，并把通用 `update_time` 仅作为技术时间戳。

#### Scenario: 任一 slice 更新不覆盖其他 slice
- **WHEN** server 成功处理任一目录、申购、收益或重仓 callback
- **THEN** 对应 SQL SHALL 只更新该 slice 的业务字段和抓取时间
- **AND** 其他三个 slice 的字段 SHALL 保持不变

#### Scenario: 业务日期和抓取时间分别保存
- **WHEN** 一个有效收益或重仓结果被接受
- **THEN** server SHALL 分别保存外部数据日期和本次成功抓取时间
- **AND** server SHALL NOT 使用通用 `update_time` 代替任一语义

#### Scenario: 单条无效记录不覆盖旧值
- **WHEN** callback 中某条基金记录缺少该 slice 的必要字段或无法解析
- **THEN** server SHALL 保留该基金该 slice 的全部旧值
- **AND** server SHALL 将该记录计入 warning

### Requirement: Callback 写入和幂等在同一事务内完成

server SHALL 在同一个业务事务中提交首次 callback 幂等记录、有效 slice 写入、warning/日志和任务终态。

#### Scenario: Callback 全部成功
- **WHEN** callback 的全部基金记录有效且事务提交成功
- **THEN** server SHALL 将任务标记为 `succeeded`
- **AND** callback 幂等记录和 slice 数据 SHALL 同时可见

#### Scenario: Callback 部分成功
- **WHEN** callback 同时包含有效与无效或未知基金记录
- **THEN** server SHALL 保存有效记录并保留无效记录的旧数据
- **AND** server SHALL 将任务标记为 `partial_failed`
- **AND** server SHALL 保存可定位到基金代码的安全 warning

#### Scenario: Callback 整体失败
- **WHEN** 外部接口整体失败、整体空结果或整体不可解析
- **THEN** server SHALL 将任务标记为 `failed`
- **AND** server SHALL NOT 写入该任务的基金 slice

#### Scenario: 重仓批次完全重叠时成功 no-op
- **WHEN** 重仓 callback 为 `succeeded` 且 `funds` 为空
- **AND** warning 明确表示全部基金仅因 `inflight_fund_codes` 与其他执行中任务重叠而跳过
- **THEN** server SHALL 将该批次视为成功 no-op
- **AND** server SHALL NOT 将其误判为外部接口异常空结果

#### Scenario: 事务内任一步失败
- **WHEN** callback 幂等记录、slice 写入、warning 或任务终态中的任一步失败
- **THEN** server SHALL 回滚本次 callback 的全部业务写入
- **AND** server SHALL 返回非 2xx
- **AND** 任务 SHALL 保持原非终态以等待 agent 重试

#### Scenario: 重复成功 callback
- **WHEN** agent 以相同 idempotency key 重试已经成功提交的 callback
- **THEN** server SHALL 返回已有任务结果
- **AND** server SHALL NOT 重复写入 slice、warning 或任务终态

#### Scenario: Agent callback 状态范围
- **WHEN** server 接收四类新基金任务的 callback body
- **THEN** callback `status` SHALL 只接受 `succeeded`、`partial_failed` 或 `failed`
- **AND** server SHALL 拒绝 agent 传入 `callback_failed`

### Requirement: 任务状态区分派发失败和回调超时

server SHALL 使用批次级 `processing_task` 跟踪四类任务，并由 server 自己收口 callback 超时。

#### Scenario: 派发失败
- **WHEN** server 无法把已创建任务派发给 agent
- **THEN** server SHALL 将任务标记为 `dispatch_failed`
- **AND** server SHALL NOT 等待该任务 callback

#### Scenario: Agent 按约定重试 callback
- **WHEN** callback 返回非 2xx
- **THEN** server SHALL 允许 agent 使用相同 idempotency key 按立即、10 秒和 60 秒重试
- **AND** server SHALL NOT 在重试窗口内提前写入 `callback_failed`

#### Scenario: Server 超时收口 callback failed
- **WHEN** 四类新基金任务超过配置的 callback 超时且仍为非终态
- **THEN** server SHALL 在本地将任务标记为 `callback_failed`
- **AND** 该终态 SHALL 解除相同 task type 后续调度的阻塞
- **AND** `callback_failed` SHALL NOT 来自 agent callback body

#### Scenario: Callback 超时覆盖执行与重试窗口
- **WHEN** server 加载 callback 超时配置
- **THEN** 默认超时 SHALL 为 30 分钟
- **AND** 配置值 MUST 覆盖预期最长任务执行时间与立即、10 秒、60 秒完整 callback 重试窗口
- **AND** server SHALL NOT 将仍在有效执行窗口内的重仓批次标记为 `callback_failed`

#### Scenario: 首期不创建逐基金任务项
- **WHEN** server 创建包含多个基金的重仓批次
- **THEN** server SHALL 只创建一个批次级 `processing_task`
- **AND** server SHALL NOT 创建 `processing_task_item`

### Requirement: 重仓批次限制规模并允许有限并发

server SHALL 以可配置批次大小创建重仓任务，默认每批 20 只基金，并与 agent 的最多 2 批并发约束配合。

#### Scenario: 按默认批次切分目标
- **WHEN** 重仓目标数量超过 20 且未覆盖默认配置
- **THEN** server SHALL 按每批最多 20 个去重基金代码创建多个任务

#### Scenario: 批次部分失败不回滚其他批次
- **WHEN** 某个重仓批次返回 `partial_failed` 或 `failed`
- **THEN** server SHALL 保留其他批次已经成功提交的数据和状态
- **AND** server SHALL NOT 清理失败基金的旧重仓

#### Scenario: 不承诺逐基金自动续跑
- **WHEN** server 或 agent 在批次执行期间重启
- **THEN** server SHALL NOT 声称能够从该批次最后一个基金精确恢复
- **AND** 后续 1/15 调度或详情异步回源 SHALL 可再次覆盖这些基金

### Requirement: 数据库基线和前向迁移保持一致

server SHALL 在实现阶段同时提供可直接重建的初始化 SQL 和已有环境可执行的前向 ALTER migration，且不得新增任务明细表。

#### Scenario: 新环境从基线重建
- **WHEN** 新环境执行更新后的 `docs/dev-ops/mysql/sql/holdlens.sql`
- **THEN** `fund` SHALL 包含四个 slice 所需字段、独立抓取时间和 `last_detail_view_time`
- **AND** `fund_top_holding` SHALL 继续保存当前前十大重仓

#### Scenario: 旧环境前向迁移
- **WHEN** 旧环境执行 `docs/dev-ops/mysql/sql/migrations/20260712_fund_refresh_slices.sql`
- **THEN** migration SHALL 添加与新基线一致的字段和索引
- **AND** migration SHALL 保留已有 `fund` 和 `fund_top_holding` 数据
- **AND** migration SHALL NOT 创建 `processing_task_item`

### Requirement: 旧复合基金刷新安全退役

server SHALL 在新链路灰度启用期间关闭并最终移除旧 `fund_detail_refresh` 复合调度，且完整目录导入后不得恢复旧全表扫描。

#### Scenario: 按顺序启用新任务
- **WHEN** agent 四个 endpoint 已部署且 server 新 Job 仍默认关闭
- **THEN** 运维 SHALL 先关闭旧复合调度
- **AND** 运维 SHALL 按目录、申购、收益、重仓顺序启用和验证新任务

#### Scenario: 全目录导入后禁止旧全表刷新
- **WHEN** 首次完整基金目录已经写入 `fund`
- **THEN** server SHALL NOT 再启用扫描整张 `fund` 表并创建逐基金 `fund_detail_refresh` 的旧 Job

#### Scenario: 单 slice 回滚
- **WHEN** 某个新 slice 需要停止运行
- **THEN** 运维 SHALL 能只关闭该 slice 的调度和异步触发
- **AND** 其他 slice SHALL 继续运行
- **AND** 运维 SHALL NOT 以恢复旧复合全表调度作为回滚方式
