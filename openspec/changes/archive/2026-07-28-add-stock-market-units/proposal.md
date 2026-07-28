# 补充股票行情币种和成交量单位

## 背景

当前 `stock_market` 同时保存 A 股和美股当前行情。A 股价格、成交额、市值来自人民币口径，美股对应字段来自美元口径；A 股成交量按“手”，美股成交量按“股”。现有表结构和回调模型只保存数值，没有保存这些单位语义，后续跨市场展示、排序或汇总时容易误读。

## 范围

- 在 A 股和美股全量行情回调契约中补充 `currency` 和 `volume_unit`。
- server 在 `stock_market` 中持久化 `currency` 和 `volume_unit`。
- server 根据业务市场兜底单位：A 股为 `CNY` / `LOT`，美股为 `USD` / `SHARE`。
- 更新相关 DTO、Command、Domain Entity、PO、Mapper XML、初始化 SQL 和测试。

## 非目标

- 不进行汇率换算。
- 不迁移线上历史数据。
- 不改变 `latest_price`、`turnover_amount`、`total_market_value` 等既有数值本身。
- 不引入港股或其他市场单位。

## 成功标准

- A 股刷新落库记录包含 `currency = CNY`、`volume_unit = LOT`。
- 美股刷新落库记录包含 `currency = USD`、`volume_unit = SHARE`。
- 既有数值解析、幂等和任务状态行为保持不变。
