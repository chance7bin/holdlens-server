## Context

`fund_detail_item` 已在 `split-fund-stock-refresh-persistence` 中收敛为基金当前详情表，当前模型以 `fund_code` 唯一表达基金身份。`fund_asset_id` 没有外键、索引或业务写入来源，并且 `FundDataRepository` 保存当前基金详情时曾显式写入 `null`。

本次调整把表名收敛为 `fund`，表达“基金公开信息主表/当前表”，避免 `item` 继续暗示它是某个快照、任务或资产主数据的明细。

## Decisions

### 重命名为基金表并删除资产主数据引用字段

将 `fund_detail_item` 重命名为 `fund`，并将 `IFundDetailItemDao`、`FundDetailItemPO` 和 `fund_detail_item_mapper.xml` 分别收敛为 `IFundDao`、`FundPO` 和 `fund_mapper.xml`。

从 `fund` 删除 `fund_asset_id`，并同步移除 Mapper result/column/insert/update 映射和 Repository 中的空值写入。

删除后，`fund` 的身份仍由 `fund_code` 决定，唯一索引命名为 `uk_fund_fund_code`。

### 保留按基金代码的跨业务读取

`portfolio` 查询组合基金详情时仍可通过持仓中的基金代码读取 `fund`。这是读模型拼接关系，不表示 `fund` 拥有或引用用户资产事实。

自选资产批量添加前仍可校验基金代码是否存在于 `fund`。这是公开资产存在性校验，不会写入 `fund`，也不会注册刷新任务。

定时基金刷新仍可扫描 `fund.fund_code` 作为刷新 universe。这里的耦合点是“公开基金数据刷新目标列表”，不是用户资产或持仓关系。后续如果需要把刷新目标与基金信息拆表，应通过单独 OpenSpec change 设计。

## Coupling Review

当前仍存在且可接受的 `fund` 外部使用：

- 基金详情刷新回调：写入或更新基金公开详情。
- 基金详情定时刷新：扫描已有 `fund_code` 创建刷新任务。
- 自选资产添加：按 `fund_code` 校验公开基金已存在，只读不写。
- 组合基金详情查询：按用户持仓中的基金代码拼接公开基金详情，只读不反写持仓。

当前需要移除的耦合：

- `fund_asset_id` 对 `portfolio` 资产主数据的隐式引用。
- `fund_detail_item` 表名与 `FundDetailItemPO`、`IFundDetailItemDao` 对旧快照明细模型的隐式引用。

## Risk / Rollback

- 数据库兼容风险：已有环境如果已经创建了 `fund_detail_item` 表，本次仅更新初始化 SQL，不会自动迁移存量库。需要投产时应补充显式迁移语句。
- 回滚方式：重新使用 `fund_detail_item` 表名并添加 `fund_asset_id` 字段和相关 PO/Mapper 映射即可，但当前业务没有使用该字段。
