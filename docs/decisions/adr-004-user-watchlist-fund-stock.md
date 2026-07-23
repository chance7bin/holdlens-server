# ADR-004：用户基金与股票自选

> 状态：业务边界保留，存储决策由 [ADR-005](adr-005-asset-record-and-watchlist-model.md) 替代。

## 保留的决策

- 自选只表示用户关注既有公共基金或股票，不代表用户持有该资产。
- 加入、查询和移除自选不得创建或修改资产记录，不得注册公共数据，不得触发刷新任务。
- server 向客户端签发 `assetRef`；客户端必须将其视为不透明字符串。基金当前编码为 `fund:{fundCode}`，股票当前编码为 `stock:{market}:{stockCode}`。
- 新请求只提交 `assetKind + assetRef`。server 必须校验引用格式、类型和公共标的存在性；请求内重复与重复加入按幂等成功处理。
- 自选列表和搜索状态必须按用户隔离，展示名称与行情实时读取 `fund`、`stock_market`。

## 被替代的存储决策

自 ADR-005 起，自选关系保存到 `watchlist_item`，内部唯一身份为 `user_id + asset_kind + asset_id`。API 层解析或签发 `assetRef`，客户端不得接触 `fund.id` 或 `stock_market.id`。

`asset_kind` 只允许 `FUND`、`STOCK`，`asset_id` 分别引用对应公共表。公共数据刷新必须保持技术 ID 稳定；自选关系本身不保存代码、名称、市场、金额、币种或目录。

## 结果

- 同一代码在不同股票市场可通过不同 `stock_market.id` 分别加入自选。
- 移除自选只删除关系，不影响相同标的资产记录。
- 基金刷新目标由自选基金和具体基金资产记录取去重并集；未细分基金金额不产生公共刷新目标。
