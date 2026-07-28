## Context

当前 `asset_info` 保存用户自选关系，唯一键为 `user_id + asset_code + asset_kind`；`fund` 和 `stock_market` 保存公共基金与股票当前数据。批量加入接口要求客户端提交代码和 market，自选列表、统一搜索、股票详情均未开放。客户端首期没有账号体系，暂时显式传入 `userId`。

根目录以下契约是本变更的接口事实源：

- `contracts/holdlens-server/client/market-asset-identity.md`
- `watchlist-asset-list.md`
- `watchlist-asset-batch-add.md`
- `market-asset-search.md`
- `stock-detail-query.md`

## Goals / Non-Goals

**Goals:**

- 让客户端不需要理解或拼装股票市场键。
- 提供自选、搜索、加入和股票当前详情的完整首期查询链路。
- 保持用户自选事实与公开行情事实分离。
- 保持现有调用方可继续使用旧批量加入字段。

**Non-Goals:**

- 不增加登录、鉴权或用户表；`userId` 仍是过渡参数。
- 不提供取消自选、拖动排序或新增/已存在计数。
- 不增加历史曲线、公司资料或交易状态。
- 不修改 `asset_info` DDL、唯一键或持仓表。
- 不因查询或加入自选触发任何刷新任务。

## Decisions

### 1. assetRef 是 API 业务引用

在 Domain 中使用无框架依赖的值对象集中校验和表示资产引用，API/Case 层只能通过该能力生成或解析引用。当前编码为 `fund:{fundCode}`、`stock:{market}:{stockCode}`，但客户端不得依赖编码结构。

`assetRef` 不持久化。解析后仍使用既有基金代码或 `stock_code + market` 查询公开资产，并按现有 `asset_info` 唯一身份写自选关系。

### 2. 批量加入保持兼容

新请求项优先使用 `assetKind + assetRef`。如果 `assetRef` 存在，server 必须校验 kind 一致并忽略客户端名称；如果不存在，继续接受旧 `assetCode/assetName/market`。旧路径只用于兼容，不出现在新客户端调用中。

返回继续只包含 `invalidItems`，并为新路径增加原始 `assetRef`。已存在和请求内重复仍幂等成功。

### 3. 自选列表由用户关系驱动

Case 先查询指定用户启用的 `asset_info`，再按 kind 批量读取 `fund`、`stock_market`。公开名称、类型、价格/净值、涨跌和时间优先于关系表冗余字段。某个公开记录缺失时保留自选行和可识别字段，公开字段返回 `null`，不得读取其他用户关系。

首期按 `asset_info.id DESC` 返回。UI 的“自选顺序”只表示默认加入顺序，不提供排序写入。

### 4. 搜索只查询既有公开资产

基金匹配代码、名称、拼音缩写和完整拼音；股票匹配代码、名称。DAO 使用 Mapper XML 参数化查询，限制 1-50 条，不允许拼接排序或 SQL。Case 批量查询当前用户自选关系生成 `watchlisted`。

搜索不写 `fund`、`stock_market`、`asset_info`，也不触发刷新。空关键字和非法枚举在外部数据访问前拒绝。

### 5. 股票详情只暴露当前事实

股票详情通过 `assetRef` 精确查询 `stock_market`，返回当前价、涨跌、今日指标、币种、行情时间和自选状态。`stock_market.status` 是数据存续状态，不映射为交易状态；响应固定不含 `tradingStatus`，延迟提示只使用“行情可能延迟”。

### 6. 分层与文件边界

- Trigger：HTTP 参数绑定和 DTO 转换。
- API：请求响应 DTO 与服务接口。
- Case：用户隔离、引用校验、批量聚合、搜索和展示模型编排。
- Domain：资产引用值对象、实体与 Repository 查询语义。
- Infrastructure：Repository/DAO/PO/MyBatis XML，只处理查询和映射。

本 change 可以修改既有 `asset_info_mapper.xml`、`fund_mapper.xml`、`stock_market_mapper.xml`；不得创建历史数据表或修改市场详情刷新 Controller，以保持与 `persist-market-detail-data` 的并行边界。

## Risks / Trade-offs

- `assetRef` 当前可读但客户端必须按不透明值处理；后续改变编码需要兼容旧引用。
- 显式 `userId` 不是真实鉴权，首期只保持现有边界；账号体系接入时应由服务端身份上下文替代。
- `asset_info` 唯一键不含 market，同代码跨市场冲突仍是现有约束，本 change 不迁表。
- 旧请求兼容增加短期分支，通过集中归一化和回归测试控制风险。

## Migration / Rollback

新增查询接口是向后兼容的。批量加入保留旧路径；如需回滚，可停止新客户端使用 `assetRef` 并移除新增查询入口，不需要数据库回滚，也不删除已有自选数据。

## Open Questions

当前无待确认事项。接口路径、引用格式、兼容策略、默认顺序和非目标已由契约固定，可以直接实现。
> 历史变更说明：本文的旧自选存储实现已由 `replace-account-holdings-with-asset-records` 替代；当前自选使用 `watchlist_item` 和公共标的技术 ID，外部 `assetRef` 契约继续有效。
