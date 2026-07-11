## Context

`fund_top_holding` 只保存基金当前重仓，业务唯一键为 `fund_code + rank_no`。当前 `FundDataRepository.saveCurrentData` 在同一事务内先 upsert `fund`，随后按 `fund_code` 删除全部重仓，再使用带 `ON DUPLICATE KEY UPDATE` 的 INSERT 重建本次结果。该实现能清理旧排名，但会让未变化排名失去原 ID，并在正常刷新中持续申请自增值。

现有 agent 回调契约为 `fund-detail-refresh-result/v2`。Case 层已经过滤空基金代码和空 `rank_no`，`top_holdings == null` 会收敛为空列表；`succeeded` 与 `partial_failed` 回调均可保存返回的基金数据。回调处理、基金保存、股票目标注册和任务状态更新位于同一个 REQUIRED 事务内。

本变更只调整 Infrastructure 层的持久化算法。按照现有六边形边界，`FundDataRepository` 继续实现 Domain 层 `IFundDataRepository`，并只通过 `IFundTopHoldingDao` 访问数据库；SQL 继续放在 app 模块的 MyBatis Mapper XML 中。无需修改 Domain、Case、Trigger 或 API 层。

## Goals / Non-Goals

**Goals:**

- 以 `fund_code + rank_no` 为当前重仓身份，对已有排名执行 UPDATE 并保留 ID。
- 只对本次新增排名执行 INSERT。
- 删除本次结果中不再存在的旧排名，保持查询只返回真实当前重仓。
- 避免正常的同排名更新通过 upsert INSERT 申请新的自增值。
- 保持现有事务原子性、幂等处理、回调契约和数据库结构不变。

**Non-Goals:**

- 不让 ID 跟随股票跨排名移动；同一股票从第 7 名变为第 5 名时，使用第 5 名现有槽位的 ID。
- 不保留空排名槽位、软删除记录或历史重仓。
- 不保证自增序列连续；新增、删除后再次新增以及失败 INSERT 仍可能消耗自增值。
- 不调整 `fund` 表自身的 upsert 或自增策略。

## Decisions

### 使用“查询现状、更新已有、插入新增、删除失效”的差量同步

每只基金仍先执行现有 `fundDao.upsert`，再读取该基金现有的 `fund_top_holding`，以 `rank_no` 建立映射。本次重仓同样按排名建立映射：

1. 排名已存在时，以现有记录 ID 执行 UPDATE，更新股票名称、股票代码、市场、持仓比例、季度变化字段和 `update_time`，同时保留 `id` 与 `create_time`。
2. 排名不存在时，执行普通 INSERT。
3. 现有排名未出现在本次结果时，按现有记录 ID 批量 DELETE。
4. 本次结果为空时，删除该基金全部当前重仓。

保持 `fundDao.upsert` 在前，可以沿用其基于 `fund_code` 唯一键取得的数据库写锁，使同一基金的并发回调在差量读取前串行化。所有步骤继续加入现有回调事务，任一步失败都回滚基金数据、重仓同步及后续状态更新。

备选方案一是保留按基金全删后重插，无法满足 ID 稳定目标。备选方案二是保留空槽并增加有效状态，会改变表语义、DDL 和所有查询条件，因此不采用。

### 已有排名使用明确 UPDATE，不使用 INSERT upsert

MySQL/InnoDB 的 `INSERT ... ON DUPLICATE KEY UPDATE` 可能在重复键更新时仍申请自增值，因此仅保留原行 ID 并不足以控制自增序列增长。实现应为已有排名提供明确的 UPDATE SQL；INSERT 改为只负责新增排名，不再承担更新分支。

Repository 先查询现有记录来判断 UPDATE/INSERT，不能单纯依赖 UPDATE 影响行数：当写入值与数据库完全相同时，影响行数可能为 0，但记录仍然存在，若据此继续 INSERT 会触发重复键错误。

### 继续以排名而非股票作为记录身份

保持现有唯一键 `fund_code + rank_no`，因此不需要 DDL 迁移。股票换仓但排名不变时保留该排名记录 ID；股票在排名之间移动时，不保证同一股票保持 ID。该选择与当前“基金当前排名关系”的查询和排序语义一致。

### 重复排名保持现有“最后一条生效”兼容语义

当前全删后逐条 upsert 在单次 payload 出现重复 `rank_no` 时，后写记录覆盖前写记录。差量同步前应按输入顺序去重并让最后一条覆盖，以避免一次同步对同一排名执行多次互相冲突的写入，同时保持现有结果语义。

## Risks / Trade-offs

- [风险] 同一基金并发刷新可能在读取现状与写入之间产生竞争 → 保持 `fundDao.upsert` 位于重仓读取之前，并让整个同步加入现有 REQUIRED 事务，利用基金唯一键写入串行化同基金回调。
- [风险] 差量算法比全删后重插包含更多分支 → 在 Repository 单元测试中分别覆盖更新、插入、删除、空集合清空和重复排名。
- [权衡] 排名删除后未来再次出现会分配新 ID → 接受该行为，因为旧排名关系已经结束，新行是真实新增；不为复用技术 ID 保留无效记录。
- [风险] 生产环境存在不符合唯一键约束的历史数据 → 当前 DDL 已有 `fund_code + rank_no` 唯一键，变更不新增数据迁移；发布前通过现有约束保证输入前提。
- [安全] 本变更不新增外部输入、权限或数据暴露面；仍只处理公开基金重仓数据。

## Migration Plan

1. 发布包含 Repository、DAO Mapper 和测试变更的新版本，无需执行 DDL 或存量数据迁移。
2. 首次新逻辑刷新时直接读取并复用现有记录 ID；失效排名按最新结果删除。
3. 回滚时恢复原“按基金删除后重插”实现即可，表结构和存量数据保持兼容。

## Open Questions

当前无待确认事项。
