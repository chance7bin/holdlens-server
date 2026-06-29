## Context

ADR-001 已明确 server 的领域边界：`portfolio` 是用户长期资产事实源，`funddata` 保存公开基金数据，`processing` 管理 agent 调用和任务状态。现有实现已经具备基金详情刷新、股票行情刷新、任务回调和定时扫描能力，但用户还没有一个入口主动维护自己关注的基金/股票资产。

当前表结构中：

- `asset_info` 表示用户维度资产主数据/自选关系，现有唯一身份为 `user_id + asset_code + asset_kind`。
- `fund_detail_item` 保存基金当前详情，同时也是已有基金公开数据记录集合。
- `stock_market_current` 保存股票当前行情，同时也是已有股票公开数据记录集合，并通过 `market_key` 处理空 market 唯一性。
- `AgentRefreshScheduleJob` 已支持从 `fund_detail_item` 和 `stock_market_current` 扫描目标并分批创建刷新任务，但批量加入自选不应触发该链路。

本变更基于 ADR-004：用户可以批量把已存在基金/股票加入自选；该能力只写用户自选关系，不注册刷新目标，不触发刷新任务，不创建当前持仓。

## Goals / Non-Goals

**Goals:**

- 提供用户批量添加自选基金/股票资产的 HTTP API：`POST /api/watchlist/assets/batch-add`。
- 批量添加前校验目标基金或股票已经存在于公开数据表。
- 只维护用户维度 `asset_info` 自选关系，不修改公开数据表。
- 保持批量添加接口幂等：已存在、请求内重复都视为成功。
- 让接口响应只暴露 `invalidItems`，隐藏新建/已存在和刷新任务细节。
- 统一命名为 `WatchlistAsset` / `BatchAdd` 语义，避免继续暗示持仓导入或公开数据导入。

**Non-Goals:**

- 不创建、更新或删除 `asset_holding`。
- 不记录 `asset_holding_change`。
- 不实现账户、金额、币种、交易记录或持仓确认流程。
- 不实现 server 直接抓取公开行情数据。
- 不实现股票 market 推断或补全。
- 不调整 `asset_info` 表结构。
- 不写入或更新 `fund_detail_item`。
- 不写入或更新 `stock_market_current`。
- 不注册基金详情刷新目标或股票行情刷新目标。
- 不创建或触发基金详情刷新、股票行情刷新任务。
- 不向前端暴露刷新任务 ID、刷新状态或刷新失败摘要。
- 不新增调度批次表、导入批次表或刷新任务父子关系。

## Decisions

### 自选资产批量添加

批量添加用例只完成一类写入：`asset_info` 用户自选关系。

接口路径固定为 `POST /api/watchlist/assets/batch-add`。请求语义是“把已存在公开资产加入用户自选”，不是资产主数据导入，也不是持仓导入。

### 公开资产必须已存在

批量添加前必须按资产类型校验公开数据表：

- 基金：根据归一化后的 `assetCode` 查询 `fund_detail_item.fund_code`。
- 股票：根据归一化后的 `assetCode + market` 查询 `stock_market_current.stock_code + market`。

不存在的输入项进入 `invalidItems`，不写入 `asset_info`。存在性校验是业务规则，应在 Case/Domain 语义中表达；Infrastructure 只提供查询能力。

股票 `market` 暂时允许为空。空 market 只匹配 `stock_market_current` 中空 market 的既有记录，不因同代码其他市场记录存在而通过校验。

### 不创建当前持仓

批量添加请求只有 `userId`、资产类型、资产代码和可选市场。缺少账户、金额、币种、来源和变更原因时，不能形成可审计的当前持仓事实。因此本变更不写 `asset_holding` 和 `asset_holding_change`。

### 暂不调整 asset_info 表结构

`asset_info` 继续保存 `asset_code`、`asset_name`、`asset_kind`、`asset_type`、`market` 和 `status`。不改成 `asset_kind + ref_id` 多态引用公开数据表。

备选方案 `asset_kind + ref_id` 会让基金和股票分别指向不同表，数据库无法可靠约束多态外键，并让用户自选关系依赖公开数据表的技术主键生命周期。该结构调整如果需要，应通过单独 change 处理。

### 资产名称不得代码占位

批量添加请求不要求提供 `assetName`。展示名称优先来自已存在的公开基金/股票数据；如果需要写入 `asset_info.asset_name`，也不能用 `asset_code` 填充名称。

### asset_info 继续使用现有唯一身份

`asset_info` 唯一身份继续使用当前表结构支持的 `user_id + asset_code + asset_kind`。本变更不新增 `market_key`，也不调整唯一索引。

这意味着同一用户不能在本次变更中分别保存相同 `assetCode + assetKind` 但不同 `market` 的两条自选关系。该限制来自“暂不调整 `asset_info` 表结构”的边界；如果后续确实需要按市场区分自选资产，应单独设计 DDL 和兼容迁移。

### invalidItems only 响应契约

批量添加响应只返回 `invalidItems`。未出现在 `invalidItems` 的输入项表示处理后已处于“已加入自选”状态，包括新建、已存在和请求内重复。

这样前端不依赖新建数量、已存在数量、刷新任务 ID 或后台刷新状态，后续公开数据刷新机制变化不会破坏前端契约。

`invalidItems.index` 固定使用 0 基请求数组下标。

### 不注册刷新目标，不触发刷新

批量添加自选不写入 `fund_detail_item` 或 `stock_market_current`，不创建 `processing_task`，不调用 agent 刷新 Case。

公开数据的刷新 universe 和刷新时机由独立手动刷新、定时刷新或后续专门用例维护。自选接口只消费既有公开数据记录。

### 分层边界

- Trigger 层只接收 HTTP 请求、调用 Case、转换 DTO。
- API 层只定义请求、响应和接口契约。
- Case 层负责批量添加流程编排、幂等归一化、公开资产存在性校验和 `invalidItems` 构建。
- Domain 层定义自选资产实体，以及公开基金/股票存在性查询所需 Repository 接口。
- Infrastructure 层实现 Repository/DAO/PO/MyBatis XML，只做数据读写和技术转换。

## Risks / Trade-offs

- [公开资产不存在] -> 不自动创建公开数据记录，输入项进入 `invalidItems`；用户或系统需先通过独立能力建立公开数据记录。
- [asset_info 仍保留冗余字段] -> 本变更有意避免表结构迁移；后续如需统一资产目录或 ref_id，应单独设计。
- [同代码不同市场无法分别自选] -> 本变更保留 `asset_info` 现有唯一键，后续如需支持应单独引入 market 唯一键或公开资产引用。
- [空 market 股票无法立即刷新行情] -> 允许加入自选，但只匹配既有空 market 股票记录；后续行情刷新能力仍依赖市场补全或 agent 能力升级。
- [前端无法区分新增和已存在] -> 这是有意隐藏幂等细节；如未来需要新增提示，再扩展响应契约。
- [名称为空影响展示] -> 查询和前端必须优先使用公开数据名称，并能表达名称未知，不得用代码伪装名称。

## Migration Plan

1. 将 API 路径和代码命名统一到 `WatchlistAsset` / `BatchAdd`。
2. 在 Case 层新增公开资产存在性校验，不存在项进入 `invalidItems`。
3. 删除批量添加流程中的公开数据表 upsert、刷新目标注册和刷新任务触发。
4. 更新相关测试，覆盖不存在校验、空 market 匹配和无刷新副作用。

回滚时可停止调用批量添加接口；已加入的自选资产属于用户数据，不做自动删除。

## Open Questions

当前无待确认事项。实现前仍需用户明确授权“开始实现”或等价指令。
