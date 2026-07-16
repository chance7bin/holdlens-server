## ADDED Requirements

### Requirement: 基金资产配置使用独立当前快照

server SHALL 将基金资产配置保存为独立于前十大重仓的当前快照，并维护报告期、状态和认可抓取时间。

#### Scenario: 保存可用资产配置
- **WHEN** 已知基金收到合法报告期和非空合法资产配置且状态为 `available`
- **THEN** server SHALL 把报告期保存为 `asset_allocation_as_of`
- **AND** server SHALL 把状态保存为 `available`
- **AND** server SHALL 保存 `asset_allocation_fetched_at`
- **AND** server SHALL 在 `fund_asset_allocation` 保存该基金当前配置

#### Scenario: 资产配置不混入重仓
- **WHEN** server 保存或查询基金资产配置
- **THEN** server SHALL NOT 写入或解释为 `fund_top_holding`
- **AND** server SHALL NOT 改变已有 `topHoldings`、`topHoldingsAsOf` 或公开股票重仓语义

#### Scenario: 保持占比精度和类型唯一
- **WHEN** server 保存某基金资产配置明细
- **THEN** `allocation_ratio` SHALL 使用 `DECIMAL(12,4)` 且单位为百分点
- **AND** 同一基金同一 `asset_type + asset_type_name` 组合 SHALL 最多存在一条当前记录
- **AND** 同一基金相同 `asset_type` 但不同 `asset_type_name` 的记录 SHALL 分别保留
- **AND** 明细 SHALL 保留 `asset_type_name` 和 `display_order`
- **AND** `display_order` SHALL 为从 1 开始的正整数
- **AND** `asset_type` SHALL 为 `stock/bond/cash/fund/other/unknown` 之一
- **AND** `allocation_ratio` SHALL 位于 0 到 100 个百分点之间

### Requirement: 有效快照按报告期原子覆盖

server SHALL 只用 `available` 且非空的合法快照覆盖当前资产配置，并在同一事务内更新元数据和明细。

#### Scenario: 新报告期替换当前快照
- **WHEN** incoming 报告期晚于当前 `asset_allocation_as_of` 且明细合法非空
- **THEN** server SHALL 删除该基金旧资产配置并插入新配置
- **AND** server SHALL 在同一事务内更新基金报告期、状态和抓取时间

#### Scenario: 同报告期相同内容 no-op
- **WHEN** incoming 报告期与当前报告期相同且规范化明细内容相同
- **THEN** server SHALL 将该基金处理为成功 no-op
- **AND** server SHALL NOT 重复删除或插入配置明细

#### Scenario: 同报告期内容修正
- **WHEN** incoming 报告期与当前报告期相同但规范化内容不同
- **THEN** server SHALL 将 incoming 视为数据源修正并原子替换当前快照

#### Scenario: 较旧报告期被忽略
- **WHEN** incoming 报告期早于当前报告期
- **THEN** server SHALL 保留当前元数据和配置明细
- **AND** server SHALL 记录不含敏感信息的 warning

#### Scenario: 原子替换失败
- **WHEN** 删除旧明细、插入新明细或更新基金元数据任一步失败
- **THEN** server SHALL 回滚 callback 幂等、元数据、明细、日志和任务终态变更
- **AND** server SHALL NOT 暴露部分新快照

#### Scenario: 并发旧报告期写入被 guard 拒绝
- **WHEN** Case 比较后、Repository 写入前，另一个 callback 已提交更晚报告期
- **THEN** server SHALL 在事务行锁内拒绝较旧报告期更新
- **AND** server SHALL NOT 删除或插入资产配置明细
- **AND** 当前较新快照 SHALL 保持不变

### Requirement: 无效和不可用结果保护历史数据

server SHALL 把失败、空结果、未知基金和不可用状态与有效覆盖指令区分，且不得因此删除历史有效配置。

#### Scenario: 空明细保护
- **WHEN** callback item 声称 `available` 但 `asset_allocations` 为空或没有合法行
- **THEN** server SHALL 拒绝该 item 的业务写入
- **AND** server SHALL 保留已有状态、报告期、抓取时间和配置明细

#### Scenario: 解析或网络失败保护
- **WHEN** agent 返回 task failed、报告期不可解析或配置行不可解析
- **THEN** server SHALL NOT 覆盖该基金已有资产配置状态或快照

#### Scenario: 未知基金跳过
- **WHEN** callback 包含 server 基金目录中不存在的 `fund_code`
- **THEN** server SHALL 跳过该基金且不插入基金目录
- **AND** server SHALL 记录 warning

#### Scenario: unavailable 保留历史有效快照
- **WHEN** incoming 状态为 `unavailable` 且本地已有 `available` 有效快照
- **THEN** server SHALL 保留原状态、报告期、抓取时间和配置明细

#### Scenario: 首次 unavailable
- **WHEN** incoming 状态为 `unavailable` 且本地从未有有效资产配置
- **THEN** server SHALL 把基金状态标记为 `unavailable`
- **AND** server SHALL 保存本次认可抓取时间
- **AND** server SHALL NOT 创建空配置明细
- **AND** 该基金 SHALL 计为可信成功结果而不是失败

#### Scenario: missing 不覆盖已有状态
- **WHEN** incoming 状态为 `missing`
- **THEN** server SHALL NOT 覆盖任何已有资产配置状态或快照

### Requirement: Agent dispatch 和 callback 使用 v1 独立契约

server SHALL 通过独立路径和 schema 派发基金资产配置任务并接收任务级 callback。

#### Scenario: 派发资产配置任务
- **WHEN** server 创建 `fund_asset_allocation_refresh` 任务
- **THEN** server SHALL POST `/tasks/fund-asset-allocation-refresh`
- **AND** payload SHALL 使用 `fund-asset-allocation-refresh-task/v1`
- **AND** payload SHALL 包含去重后的 `fund_codes`
- **AND** payload SHALL NOT 包含目标报告期

#### Scenario: 接收合法 callback
- **WHEN** callback 使用 `fund-asset-allocation-refresh-result/v1`、已知 task id、合法回调认证和 `<server_task_id>:result:1` 幂等键
- **THEN** server SHALL 接受 `succeeded/partial_failed/failed` 之一作为 task 级状态
- **AND** server SHALL 按基金处理 `fund_code`、`asset_allocation_as_of`、`allocation_status` 和 `asset_allocations`

#### Scenario: 拒绝错误 schema 或幂等键
- **WHEN** callback schema、task type 或幂等键不符合 v1 契约
- **THEN** server SHALL 拒绝 callback
- **AND** server SHALL NOT 写入资产配置数据或任务成功终态

#### Scenario: callback 重复投递
- **WHEN** 相同合法幂等键的 callback 已成功处理后再次投递
- **THEN** server SHALL 返回既有任务结果
- **AND** server SHALL NOT 重复替换快照、写日志或改变终态

#### Scenario: 全部基金 unavailable
- **WHEN** callback 中全部基金都是可信 `unavailable` 结果
- **THEN** server SHALL 接受该批结果并保持任务 `succeeded`
- **AND** server SHALL NOT 因没有资产配置明细而改判 failed

#### Scenario: 全 in-flight 成功 no-op
- **WHEN** agent 回传 `succeeded`、空 `funds` 和顶层 `fund_already_inflight` warning
- **THEN** server SHALL 将任务作为成功 no-op
- **AND** server SHALL NOT 因空基金结果改判 failed

#### Scenario: 部分基金失败
- **WHEN** callback 同时包含可信结果和被跳过的无效、未知、较旧或 missing 基金
- **THEN** server SHALL 提交有效基金结果并保留其他基金旧数据
- **AND** task 终态 SHALL 为 `partial_failed`

### Requirement: 资产配置任务独立调度候选

server SHALL 使用独立开关和每周 cron 调度资产配置任务，并在重仓目标人群规则上应用资产配置新鲜度过滤。

#### Scenario: 发现基础目标人群
- **WHEN** server 扫描资产配置刷新目标
- **THEN** 候选基础集合 SHALL 包含当前真实持有、有效关注/资产主数据和近 90 天查看的已知基金并集
- **AND** 仅存在于完整基金目录但不满足这些条件的基金 SHALL NOT 自动进入逐基金任务

#### Scenario: missing 基金进入候选
- **WHEN** 基础目标基金的资产配置状态为空或为 `missing`
- **THEN** server SHALL 允许其进入本周刷新候选

#### Scenario: 存在新已结束季度
- **WHEN** 基础目标基金状态为 `available` 且当前报告期早于最近已结束自然季度末
- **THEN** server SHALL 允许其进入本周刷新候选
- **AND** server SHALL 让 agent 探测实际最新报告期

#### Scenario: unavailable 七天退避
- **WHEN** 基础目标基金状态为 `unavailable` 且距离 `asset_allocation_fetched_at` 不足 7 天
- **THEN** server SHALL NOT 将该基金纳入本轮任务

#### Scenario: 按批派发并跳过重叠轮次
- **WHEN** 本轮没有同类型非终态任务且候选非空
- **THEN** server SHALL 按独立可配置 batch size 创建并派发任务
- **AND** 默认 batch size SHALL 为 20
- **WHEN** 本轮开始前已有 `fund_asset_allocation_refresh` 非终态任务
- **THEN** server SHALL 跳过整轮调度

#### Scenario: 调度开关独立
- **WHEN** 资产配置调度开关关闭
- **THEN** cron 入口 SHALL NOT 创建或派发资产配置任务
- **AND** 目录、申购、收益和重仓调度 SHALL 不受影响

#### Scenario: 通过 HTTP 手动触发刷新逻辑
- **WHEN** 调用方 POST `/api/agent/fund-asset-allocation-refresh/schedule-runs`
- **THEN** server SHALL 直接调用资产配置刷新 Case 并记录 `manual` 触发来源
- **AND** 手动调用 SHALL 不受 cron 调度开关影响，并遵守 batch size 和同类型非终态任务跳过规则
- **AND** server SHALL 返回标准成功响应且不等待 agent callback

### Requirement: 基金详情独立返回资产配置

基金详情 API SHALL 返回独立资产配置元数据和明细，且保持已有字段兼容。

#### Scenario: 返回可用资产配置
- **WHEN** 基金存在可用资产配置快照
- **THEN** 详情 SHALL 返回 `assetAllocationAsOf`、`assetAllocationStatus=available` 和 `assetAllocationFetchedAt`
- **AND** 详情 SHALL 返回按 `displayOrder`、`assetType`、`assetTypeName` 排序的 `assetAllocations`
- **AND** 每条 SHALL 包含 `assetType`、`assetTypeName`、`allocationRatio` 和 `displayOrder`

#### Scenario: 返回缺失或不可用状态
- **WHEN** 基金没有有效配置或被标记为 unavailable
- **THEN** 详情 SHALL 返回对应的 `missing` 或 `unavailable` 状态
- **AND** 详情 SHALL 返回空资产配置列表
- **AND** 已有收益和前十大重仓字段 SHALL 保持原语义

#### Scenario: 详情缺失时异步回源
- **WHEN** 单基金详情或持仓基金详情读取到 missing/空状态的已知基金
- **THEN** server SHALL 在返回数据库响应后 best-effort 异步派发独立资产配置任务
- **AND** 详情响应 SHALL NOT 等待 agent

#### Scenario: 详情发现新已结束季度
- **WHEN** 详情读取到 available 快照且报告期早于最近已结束季度末
- **THEN** server SHALL best-effort 异步派发独立资产配置任务
- **AND** server SHALL NOT 将该代码混入 top holding dispatch

#### Scenario: 详情遵守 unavailable 退避
- **WHEN** 详情读取到 unavailable 且认可抓取时间不足 7 天
- **THEN** server SHALL NOT 派发资产配置任务
- **WHEN** unavailable 认可抓取时间已满 7 天
- **THEN** server SHALL 允许 best-effort 异步派发资产配置任务

### Requirement: 数据库基线和前向迁移一致

server SHALL 同时提供新环境重建基线和已有环境前向迁移，并保护现有基金数据。

#### Scenario: 新环境重建
- **WHEN** 新环境执行更新后的 `holdlens.sql`
- **THEN** `fund` SHALL 包含三个资产配置元数据字段
- **AND** `fund_asset_allocation` SHALL 包含精度、排序、时间字段和业务唯一键

#### Scenario: 已有环境迁移
- **WHEN** 已有环境执行 `20260716_fund_asset_allocation_refresh.sql`
- **THEN** migration SHALL 添加与重建基线一致的列、表和索引
- **AND** migration SHALL 保留已有基金、收益和重仓数据

### Requirement: 外部输入和日志保持数据安全

server SHALL 对资产配置 callback 执行既有认证、长度限制和敏感字段脱敏，且不得记录不必要的个人资产或凭据数据。

#### Scenario: 未授权 callback
- **WHEN** callback 未携带正确的 agent 回调认证头
- **THEN** server SHALL 返回未授权错误
- **AND** server SHALL NOT 写入任务或资产配置数据

#### Scenario: warning 包含敏感键值
- **WHEN** agent warning 或错误摘要包含 token、authorization、cookie、password、secret 或 API key 形式的值
- **THEN** server SHALL 在持久化或日志输出前脱敏并限制长度

#### Scenario: 安全任务摘要
- **WHEN** server 保存资产配置任务参数摘要
- **THEN** 摘要 SHALL 只包含触发来源和基金代码等执行所需非敏感字段
- **AND** 摘要 SHALL NOT 包含回调认证、凭据、报告原文或个人持仓金额
