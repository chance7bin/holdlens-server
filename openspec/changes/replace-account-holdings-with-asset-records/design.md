## Context

当前数据库使用：

- `asset_account`：要求持仓归属账户；
- `asset_info`：同时保存用户级资产字段和自选关系；
- `asset_holding`：通过 `account_id + asset_id` 表示当前持仓；
- `asset_holding_change`：依赖账户和旧资产 ID 的变更历史。

当前代码中的 portfolio 查询会逐条读取账户和资产信息；自选列表、搜索自选状态与基金刷新目标也读取 `asset_info`。`fund` 按 `fund_code` upsert，`stock_market` 按 `stock_code + market` upsert，现有刷新不会删除后重建公共主记录，因此其技术 ID 可以作为新的应用层多态引用，但必须继续保持稳定。

ADR-005 和根目录 `CONTEXT.md` 是本设计的领域语言与边界输入。

## Goals / Non-Goals

**Goals:**

- 用最少业务对象覆盖普通金额、未细分投资金额和具体基金/股票持仓。
- 让资产目录只承担层级、系统/用户范围、稳定系统编码、金额方向、排序和状态。
- 保持每笔金额只计算一次，并支持从未细分金额渐进拆出具体持仓。
- 保持自选关系、用户资产事实和公共市场数据相互独立。
- 保留可审计但不过度事件溯源化的资产变更历史。
- 支持原币记录和按指定币种的当前资产汇总。

**Non-Goals:**

- 不保留资产账户、持有平台或来源位置模型；用户只能使用可选备注。
- 不保存数量、成本、成本价、盈亏或交易记录。
- 不实现具体标的集中度、基金穿透、重复持仓或分析覆盖率。
- 不自动使用行情、数量、成本或汇率覆盖用户记录金额。
- 不实现自动汇率抓取、定时刷新或历史汇率。
- 不实现资产记录移动、目录纠错组合操作或旧数据迁移。
- 不在本变更确定基金和股票系统目录最终作为一级目录还是某个系统分组的子目录。

## Decisions

### 1. 资产目录保持两级树和最小业务语义

`asset_catalog` 建议字段：

```text
id
user_id              SYSTEM 为空，USER 必填
parent_id
catalog_code         SYSTEM 必填且全局唯一，USER 为空
catalog_name
catalog_scope        SYSTEM / USER
balance_direction    ADD / SUBTRACT
sort_order
status               ENABLED / DELETED
create_time
update_time
```

目录规则：

- 最多两级；只有启用叶子目录可以创建资产记录。
- 父子目录金额方向必须一致，资产记录金额始终非负。
- 系统目录全局可见、不可编辑、不可停用、不可删除，由版本初始化维护。
- 基金、股票、现金、银行卡等系统目录使用稳定 `catalog_code`；基金、股票编码触发前端专用流程，后端按目录编码重新校验。
- 用户目录只属于单个用户，可以构建自己的层级或挂在已有子目录的系统分组下，但不能挂在系统叶子目录下。
- 用户目录仅在没有子目录和活跃资产记录时允许软删除。
- 基金、股票系统目录最终初始层级暂缓决定，但稳定编码和叶子行为不变。

### 2. 资产记录统一三种数据状态

`asset_record` 建议字段：

```text
id
user_id
catalog_id
record_name
asset_kind           FUND / STOCK / null
asset_id             fund.id / stock_market.id / null
amount
currency
remark
status               ACTIVE / ARCHIVED / DELETED
create_time
update_time
```

合法状态：

| asset_kind | asset_id | 含义 |
| --- | --- | --- |
| null | null | 普通金额记录 |
| FUND | null | 未细分基金金额 |
| STOCK | null | 未细分股票金额 |
| FUND | fund.id | 具体基金持仓 |
| STOCK | stock_market.id | 具体股票持仓 |
| null | 非空 | 非法 |

目录编码与资产类型必须一致：基金目录只允许 `FUND`，股票目录只允许 `STOCK`，其他目录只允许空类型。后端加载目录后校验，不相信客户端自行传入的类型。

每条资产记录只以 `id` 唯一。相同用户、相同 `asset_kind + asset_id` 可以存在多条独立记录；该组合只建立非唯一查询索引。模型不保存账户、持有位置或来源平台。

`catalog_id`、`asset_kind`、`asset_id`、`currency` 创建后不可修改。当前不支持移动资产或组合纠错；目录选错、币种选错或具体标的选错时，用户手动删除误录并重新新增。

### 3. 名称、金额和生命周期

- 普通金额记录名称由用户填写并可编辑。
- 未细分基金/股票名称由服务端默认生成，之后允许用户编辑。
- 具体持仓名称由服务端从 `fund` 或 `stock_market` 复制为创建时快照，只读且不随公共名称变化自动更新。
- `remark` 对所有记录可选且可编辑。
- `amount` 是用户确认的当前原币金额，必填、非负，是资产汇总的权威事实。
- 行情、数量、成本和汇率不得自动覆盖 `amount`。
- 金额为零不会自动改变状态；零金额记录可以保持 `ACTIVE` 并在以后继续更新。
- `ARCHIVED` 表示暂不计入当前资产且允许恢复；`DELETED` 表示误录软删除。两者都不物理删除。

### 4. 未细分金额拆分为单条具体持仓

只有满足以下条件的源记录可以拆分：

```text
status = ACTIVE
amount > 0
asset_kind IN (FUND, STOCK)
asset_id IS NULL
```

一次拆分只选择一只同类型公共基金或股票，并填写大于零且不超过源金额的拆分金额。目标记录：

- 继承源 `catalog_id`、`asset_kind`、`currency`；
- 使用所选公共标的 ID；
- 从公共表复制名称快照；
- 可以带可选备注；
- 始终创建新记录，不自动合并同标的既有记录。

部分拆分扣减源金额并保持 `ACTIVE`；全部拆分把源金额更新为零并改为 `ARCHIVED`。拆分在一个数据库事务内完成，源记录需要加锁或使用等价并发控制，防止并发超额拆分。前端只有在选中可拆分记录时显示“拆分现有金额”，普通入口仍显示“新增资产”，拆分表单隐藏目录、类型和币种选择。

### 5. 资产变更历史只追加

`asset_record_change` 建议字段：

```text
id
operation_id
record_id
change_type
before_amount
after_amount
currency
before_status
after_status
operator_id
create_time
```

变更类型包括 `CREATE`、`UPDATE_AMOUNT`、`SPLIT_OUT`、`SPLIT_IN`、`ARCHIVE`、`RESTORE`、`DELETE`。一次拆分对源和目标写两条相同 `operation_id` 的历史。历史只追加，不更新、不删除；纠错通过新增补偿事实表达。当前状态直接读取 `asset_record`，不通过重放历史计算。

名称和备注变化只更新 `asset_record.update_time`，不写变更历史，避免重复保存用户备注等敏感文本。

### 6. 自选关系独立且最小

`watchlist_item` 建议字段：

```text
id
user_id
asset_kind           FUND / STOCK
asset_id             fund.id / stock_market.id
create_time
```

唯一键为 `user_id + asset_kind + asset_id`。加入自选、创建资产、删除自选和删除资产互不产生隐式副作用。自选不关联目录、金额、币种、名称快照或备注，展示数据实时读取公共表。

数据库无法用一个外键约束 `asset_id` 指向两张表，Case/Domain 必须按 `asset_kind` 校验公共记录存在且类型一致。公共 `fund.id` 与 `stock_market.id` 必须稳定，公共行不能因刷新删除后重建。

现有外部 `assetRef` 保持不透明 API 引用，由 server 解析后映射到 `asset_kind + asset_id`；客户端不得直接依赖数据库 ID。

### 7. 汇率表保持通用结构，当前只写外币兑人民币

`exchange_rate` 建议字段：

```text
id
base_currency
quote_currency
rate
source
source_as_of
fetched_at
create_time
update_time
```

唯一键为 `base_currency + quote_currency`，每个方向只保留最新值。当前业务只允许：

```text
base_currency != CNY
quote_currency = CNY
rate > 0
```

语义固定为 `1 base_currency = rate × CNY`。限制由业务层执行，表结构保留未来通用币种对能力。本次只支持初始化或管理方式写入，不实现外部抓取。

资产汇总由查询参数指定目标币种，默认 `CNY`，不保存用户本位币：

- 同币种直接使用原金额；
- 外币转 CNY 使用乘法；
- CNY 转外币使用除法；
- 两种外币之间先转 CNY，再转目标外币；
- 缺少任一汇率时返回部分汇总及缺失币种，不能按零处理，也不能静默声称完整。

只有 `ACTIVE` 资产记录参与当前汇总；目录 `ADD` 金额增加净资产，`SUBTRACT` 金额减少净资产。

## Data Flow

### 新增资产

1. 客户端读取系统目录与当前用户目录。
2. 用户选择启用叶子目录；基金/股票系统编码进入专用表单。
3. Case 读取目录并校验用户可见性、叶子状态、金额方向和系统编码。
4. 若选择具体标的，按 kind 查询公共表并复制名称；未选择时创建未细分金额。
5. 保存 `asset_record` 和 `CREATE` 变更。

### 拆分现有金额

1. Case 按 `user_id + record_id` 锁定源记录并校验可拆分状态。
2. 校验目标公共标的与源 `asset_kind` 一致。
3. 创建目标记录，扣减源记录；全量拆分时归档源记录。
4. 使用同一 `operation_id` 写入 `SPLIT_OUT`、`SPLIT_IN`。
5. 事务提交后返回源剩余金额与新记录。

### 当前资产汇总

1. 查询用户所有 `ACTIVE` 记录及对应目录金额方向。
2. 按目标币种读取所需最新汇率。
3. 分别计算加项、减项和净资产。
4. 汇率缺失时保留原币明细，返回部分汇总状态和缺失币种。

## Error Handling

- 非叶子、停用、越权目录：拒绝创建。
- 系统目录修改、用户目录跨用户挂载、父子方向不一致或超过两级：拒绝。
- 非法 `asset_kind + asset_id` 状态、公共标的不存在或目录编码不匹配：拒绝。
- 拆分金额不合法、源状态变化或并发后余额不足：整个拆分回滚。
- 汇率缺失：不影响资产记录查询，但汇总明确返回部分结果。
- 任何错误响应和日志不得包含完整个人资产组合、真实备注、原始导入文件或凭据。

## Migration / Rollback

当前数据允许全部重建，因此不做旧数据迁移。实施时通过迁移脚本或受控初始化替换旧表并重新写入系统目录、汇率与开发 Mock 数据。正式执行前仍应确认目标环境无须保留的数据，并保留数据库级备份或可恢复快照。

回滚需要恢复旧 DDL 和旧代码；由于不迁移旧数据，回滚不保证恢复切换后产生的新资产记录。发布应把 DDL、服务端实现和客户端契约视为一个原子版本。

## Security / Privacy

- 所有用户目录、资产记录、变更历史和自选查询必须以 `user_id` 隔离。
- 资产金额、名称和备注不得写入处理任务参数、错误摘要或应用日志。
- 汇率和公共基金/股票数据是全局公共事实，不得反向携带用户信息。
- 多态公共资产引用只能由后端解析或校验，客户端提交的 kind、ID 或引用不能直接信任。

## Open Questions

- 基金和股票系统目录最终作为一级目录，还是作为某个系统分组的二级目录，暂缓决定；实现系统目录初始化前必须确认。
