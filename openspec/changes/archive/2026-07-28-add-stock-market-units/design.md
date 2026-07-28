# 设计

## 当前行为

`stock_market` 使用同一组 DECIMAL 字段保存 A 股和美股价格、成交额、市值等数值。A 股和美股刷新链路分别通过 `a_share_market_refresh`、`us_stock_market_refresh` 回调进入 server，Case 层解析字符串数值后通过 `IStockMarketRepository#upsertMarkets` 写入 `stock_market`。

当前接口契约没有单位字段，`stock_market` DDL、Domain Entity、PO 和 MyBatis Mapper 也没有币种或成交量单位字段。

## 设计决策

1. 在 `stock_market` 增加两个非空字段：
   - `currency VARCHAR(3) NOT NULL DEFAULT 'CNY'`
   - `volume_unit VARCHAR(20) NOT NULL DEFAULT 'LOT'`
2. Case 层按业务市场确定单位：
   - `A_SHARE` -> `CNY` / `LOT`
   - `US_STOCK` -> `USD` / `SHARE`
3. 回调请求和 Command 保留 `currency`、`volume_unit` 字段，便于契约对齐；落库时以业务市场兜底和校正，避免错误 payload 污染长期事实源。
4. 不改 schema version。该变更是向现有 v1 回调增加字段，server 对缺失字段仍可按市场默认值处理。

## 兼容性

- 老 agent 未发送 `currency` 或 `volume_unit` 时，server 仍按市场写入默认单位。
- 初始化 SQL 直接更新，不做历史迁移；现有数据库如需在线升级，应另行补迁移脚本。

## 风险

- 旧数据如果已经存在，新增 NOT NULL 字段需要数据库侧补默认值。当前仓库只维护初始化 SQL，本变更不覆盖线上迁移。
