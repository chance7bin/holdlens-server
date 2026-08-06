# 设计

## Context

当前 Server 的资产事实位于 `portfolio` 领域，资产记录金额表示用户确认的当前余额。新需求中的日常收入和支出只用于独立记账，第一版不选择资产账户，也不改变资产记录金额。若把收支条目放入 `portfolio` 或复用资产记录，将混淆当前余额与历史流水，并为未来可选的资产联动制造隐含耦合。

项目采用 `Trigger → API → Case → Domain ← Infrastructure` 的 DDD/六边形依赖方向，业务 `DATETIME` 使用 `Asia/Shanghai`。数据库由初始化脚本和显式迁移共同维护，MyBatis SQL 必须位于 Mapper XML。

## Goals / Non-Goals

**Goals:**

- 建立独立、按用户隔离的记账事实源。
- 支持 UI 原型需要的新增、编辑、删除、明细、筛选、趋势、分类排行、月账单和年账单。
- 统一由 Server 定义分类、日期范围、统计补点、平均值和结余语义。
- 为未来可选的资产联动保留清晰边界，但不提前实现联动模型。

**Non-Goals:**

- 不提供账户、转账、预算、附件、标签、自定义分类、周期记账、多币种或 OCR 导入。
- 不读取、更新或关联资产目录、资产记录和资产变更。
- 不创建预计算账单或统计表，不实现事件溯源或完整修订历史。
- 不改变现有资产、自选和市场数据接口。

## Decisions

### 1. 使用独立 bookkeeping 领域

新增 `domain.bookkeeping`、`cases.bookkeeping` 及其适配器。核心术语为收支条目、收支分类、账单、收支统计和结余，并已补充到 `CONTEXT.md`。`portfolio` 不依赖 bookkeeping，bookkeeping 也不依赖 portfolio。

第一版收支条目是聚合根，包含：

```text
id, userId, requestId, type, categoryCode,
amount, currency, entryDate, note, status,
createTime, updateTime
```

`type` 只允许 `EXPENSE/INCOME`，`status` 只允许 `ACTIVE/DELETED`，`currency` 固定 `CNY`。金额保存正数绝对值；正负号只属于客户端展示和汇总计算。

### 2. 固定分类由 Domain 定义，不新增分类表

第一版不支持自定义分类。领域层以稳定代码、类型、中文名称和排序定义固定分类，并提供按类型查询和一致性校验。数据库条目只保存 `category_code`；返回时由 Domain 补充当前分类名称。

分类代码属于契约，不能通过中文名称判断。后续若增加自定义分类，应单独建模和迁移，不在本次预埋空表或扩展字段。

### 3. 创建幂等，修订完整替换，删除软删除

`bookkeeping_entry` 使用 `(user_id, request_id)` 唯一键。Case 在事务内先按业务键查询，再创建；Mapper 使用 `ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)` 将并发唯一键冲突收敛为不改业务字段的空更新，随后重新读取首次结果。相同请求键后续携带不同内容仍返回第一次结果。

修订接口 `POST /api/bookkeeping/entries/{entryId}/revise` 接收完整可编辑字段快照，避免可空字段与“未提交字段”歧义。删除接口使用 `POST .../delete` 将状态改为 `DELETED`。修订和删除都只操作 `user_id + id + ACTIVE` 命中的条目。

### 4. 明细与汇总使用同一查询过滤语义

明细接口接收包含首尾的 `startDate/endDate`，范围最大 366 天，并允许按类型和分类过滤。Repository 使用参数化 SQL 查询同一组活动条目并按 `entry_date DESC, id DESC` 返回，Domain 从这组结果计算收入、支出和结余，避免明细与汇总产生过滤差异。

分类筛选必须与显式类型一致；未传类型时由分类确定类型。空集合返回金额零和空条目数组，不作为错误。

### 5. 查询时聚合图表和账单

不持久化统计结果。Repository 按自然日期范围读取活动条目，Case 负责查询编排，Domain 统计服务负责：

- 根据北京时间计算自然周一至周日、自然月和自然年；
- 周/月按日、年按月补齐零值点；
- 当前周期平均值只除以截至今天已经历的日或月，历史周期使用完整周期；
- 分类占比按所选收支类型总额计算，总额为零时返回空排行；
- 月账单返回指定年份逐月汇总，当前年截至当前月，历史年完整十二个月；
- 年账单返回存在活动条目的年份并计算全量合计。

金额计算使用 `BigDecimal`，输出最多两位小数，不使用浮点数参与金额和占比计算。

### 6. 持久化与索引

新增 `bookkeeping_entry`：

- 主键 `id`；
- 幂等唯一键 `(user_id, request_id)`；
- 明细索引 `(user_id, status, entry_date, id)`；
- 分类统计索引 `(user_id, status, type, category_code, entry_date)`。

同时更新 `holdlens.sql` 并新增 `20260806_standalone_bookkeeping.sql`。迁移只增加新表，不修改资产表，因此可以独立部署和回滚；回滚会删除所有记账数据，执行前必须先备份或确认无须保留。

## API and Layer Boundaries

- Trigger 绑定参数、映射 DTO 和统一响应，不计算日期范围或统计。
- API 模块定义请求、响应和服务接口，不依赖持久化类型。
- Case 校验用户输入、编排事务、Repository 和统计结果。
- Domain 维护条目不变量、固定分类和周期/统计业务规则，不依赖 Spring/MyBatis。
- Infrastructure 只实现 DAO、PO、Mapper XML 和 Repository，不承载分类或日期业务判断。

共享接口事实源为 `contracts/holdlens-server/client/bookkeeping.md`。响应不得泄露 PO、数据库技术字段或其他用户关系。

## Security / Privacy

- 所有列表、详情、修订、删除、统计和账单均以 `userId` 作为强制过滤条件。
- 不存在、跨用户和已删除详情统一返回“收支条目不存在或不可见”。
- 金额、备注、条目列表和用户关系不得进入应用日志、错误摘要或 SQL 拼接。
- `type`、`categoryCode`、日期、金额、备注长度和请求键均由 Case/Domain 校验，Mapper XML 只使用参数绑定。
- 创建、修订和删除不得触碰资产表；测试中显式验证这一边界。

## Compatibility / Rollback

- 所有 API 和表均为新增，不改变现有客户端接口。
- Server 可先于 Client 部署；没有调用时不会影响现有功能。
- 回滚应用代码后新表可暂时保留；如需回滚数据库，单独删除 `bookkeeping_entry`，但会丢失记账数据。

## Open Questions

当前无待确认事项。第一版明确固定 CNY、固定分类、只含收入和支出，并与资产模块完全独立。
