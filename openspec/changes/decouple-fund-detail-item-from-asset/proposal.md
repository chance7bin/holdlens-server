## 背景

`fund_detail_item` 当前用于保存基金当前公开详情，但表名仍带有 `item` 历史语义，且表结构中曾保留 `fund_asset_id`。这两点都容易暗示基金公开信息是某个快照、任务或 `portfolio` 资产主数据的明细。

现有实现中基金查询、自选校验、定时刷新扫描和组合详情拼接都以 `fund_code` 为业务键。`fund_asset_id` 已无业务写入来源，保存基金详情时曾固定写入 `null`。

## 范围

- 将 `fund_detail_item` 表重命名为 `fund`，并同步 DAO、PO、MyBatis Mapper 命名。
- 从基金表 DDL、PO、MyBatis Mapper 和写入转换中移除 `fund_asset_id`。
- 更新 server 领域边界文档，明确 `fund` 只保存基金公开信息，不直接引用用户资产主数据。
- 检查基金表与其他业务的耦合点，并保留当前合理的读取/编排关系。

## 非目标

- 不调整 `asset_info`、`asset_holding` 或用户自选/持仓表结构。
- 不变更基金详情刷新、定时扫描、自选存在性校验或组合详情查询的行为语义。
- 不新增数据库迁移脚本；当前仓库以初始化 SQL 为主，本次同步调整初始化 DDL。

## 成功标准

- 生产代码、初始化 DDL 和 Mapper 中不再存在 `fund_asset_id` 或 `fundAssetId`。
- 生产代码、初始化 DDL 和 Mapper 中使用 `fund` 表名，不再使用 `fund_detail_item`。
- `fund` 以 `fund_code` 唯一保存基金当前公开信息。
- OpenSpec 严格校验通过，相关 Maven 测试通过。
> 历史变更说明：本文涉及的旧用户资产表边界已由 `replace-account-holdings-with-asset-records` 替代；基金公共数据独立边界继续有效。
