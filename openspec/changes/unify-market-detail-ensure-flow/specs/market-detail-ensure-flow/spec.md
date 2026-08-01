## ADDED Requirements

### Requirement: 统一详情确保并读取当前快照

Server SHALL 提供基金和股票共用的详情 ensure-and-read POST，一次返回当前可展示详情、用户自选状态、切片状态和可选操作标识。该 POST SHALL 替代详情页原有主详情 GET；现有 GET SHALL 保持无副作用兼容查询。

#### Scenario: 已有新鲜数据

- **WHEN** 用户打开已有完整且新鲜公共数据的基金或股票详情
- **THEN** Server SHALL 返回当前详情和 `status=AVAILABLE`
- **AND** `operationId` SHALL 为空
- **AND** MUST NOT 派发 Agent 任务。

#### Scenario: 首次访问缺失数据

- **WHEN** 必需切片没有事实且不在可信空或失败冷却期
- **THEN** Server SHALL 返回详情目录快照和 `status=PROCESSING`
- **AND** SHALL 创建或复用一个可查询操作
- **AND** 同一标的并发请求 MUST NOT 重复派发 Agent。

#### Scenario: 已有陈旧数据

- **WHEN** 某切片有可展示事实但超过配置化新鲜度阈值
- **THEN** Server SHALL 返回该切片 `status=AVAILABLE`、`freshness=STALE` 和 `hasData=true`
- **AND** SHALL 后台创建或复用刷新操作
- **AND** 整体响应 MUST NOT 要求客户端阻塞轮询。

### Requirement: 由 Server 表达完整性和空结果

Server SHALL 对每个切片返回 `MISSING/PROCESSING/AVAILABLE/EMPTY/FAILED`，并与 `FRESH/STALE` 分开表达。Client MUST NOT 通过数组为空或字段 null 决定是否创建任务。

#### Scenario: 上游确认没有数据

- **WHEN** Agent 成功完成切片请求并显式报告空结果
- **THEN** Server SHALL 保存 `EMPTY` 及确认时间
- **AND** 冷却期内后续 ensure MUST NOT 再次派发该切片。

#### Scenario: 从未请求

- **WHEN** 切片无事实、无成功空确认且无失败尝试
- **THEN** Server SHALL 将其解释为 `MISSING`
- **AND** ensure SHALL 尝试领取刷新任务。

### Requirement: 查询统一详情操作

Server SHALL 提供统一的无副作用操作状态 GET，基金和股票使用相同状态结构。

#### Scenario: 无可用数据且处理中

- **WHEN** Client 查询一个有效活动操作且相关切片仍无可用事实
- **THEN** Server SHALL 返回 `PROCESSING` 和建议重试间隔
- **AND** GET MUST NOT 更新访问时间、租约或任务状态。

#### Scenario: 操作完成

- **WHEN** callback 已收敛切片状态
- **THEN** Server SHALL 返回持久化后的 `AVAILABLE/EMPTY/FAILED`
- **AND** Client SHALL 能据此决定重读数据或展示终态。

### Requirement: 维护活动标的集合

Server SHALL 仅在详情 ensure 时更新标的级全局最近访问时间，并把持有、自选或最近 90 天访问的标的纳入活动详情刷新目标。

#### Scenario: 高频重复打开详情

- **WHEN** 同一标的在访问写入节流窗口内被重复 ensure
- **THEN** Server SHALL 保持最近访问语义
- **AND** MUST NOT 为每次打开都执行数据库更新。

#### Scenario: 不活跃标的退出刷新

- **WHEN** 标的无人持有、无人自选且最后详情访问早于活动窗口
- **THEN** Server SHALL 从常规定时详情刷新目标中排除它
- **AND** MUST NOT 删除标的目录或既有详情事实。

### Requirement: 按市场和数据切片调度刷新

Server SHALL 对股票当前行情使用收盘后批量刷新，对活动详情切片使用各自新鲜度阈值和收盘后或披露周期调度。

#### Scenario: 股票市场日终刷新

- **WHEN** 市场日历声明当日开市且时间命中常规或提前收盘后的日终刷新点
- **THEN** Server SHALL 创建或跳过复用中的对应全市场任务
- **AND** 休市日、盘中及非日终刷新点 MUST NOT 派发。

#### Scenario: 活动详情定时刷新

- **WHEN** 定时入口扫描到活动基金或股票
- **THEN** Server SHALL 复用 ensure 的完整性、新鲜度、冷却和 single-flight 规则
- **AND** 只为缺失或陈旧切片派发任务。

#### Scenario: 披露类基金切片检查

- **WHEN** 每周检查活动基金的重仓和资产配置
- **THEN** 重仓 SHALL 只领取超过配置化天数未成功获取的基金
- **AND** 资产配置 SHALL 只领取报告期落后于最近结束季度或达到失败重试窗口的基金。

### Requirement: 区分股票行情时间

Server SHALL 区分批次抓取时间与交易所行情时间。

#### Scenario: 来源未提供行情时间

- **WHEN** 当前行情来源只提供批次抓取时间
- **THEN** Server SHALL 返回 `quoteFetchedAt`
- **AND** MUST NOT 把它标记为 `quoteAsOf`。
