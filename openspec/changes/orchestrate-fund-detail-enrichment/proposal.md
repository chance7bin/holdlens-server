# Change: 编排基金详情补全任务

## Why

基金详情页当前只能读取已经落库的净值历史，缺少数据时没有受控的补全入口；阶段业绩也没有同类平均和同类排名的长期事实。若由客户端直接触发 Agent，或让查询接口产生隐式副作用，会破坏 Server 的业务边界，并在多人同时访问时造成重复采集。

## What Changes

- 新增基金阶段业绩规范化存储与查询接口，保存同一来源快照中的基金收益、同类平均、同类排名、样本总数和排名变化。
- 新增客户端显式请求基金详情补全的 Server API；原有 GET 查询继续保持只读。
- 新增持久化 slice 状态，通过事务锁合并并发请求，并对可信空结果和失败结果设置冷却窗口。
- 扩展 `market_detail_data_refresh` 任务、dispatch 和 callback，使基金可按需刷新 `nav_history`、`period_performance` 或两者组合。
- callback 按 slice 独立事务保存数据并同步更新 slice 状态；派发失败和超时可安全恢复。

## Capabilities

### Modified Capabilities

- `market-detail-data-persistence`: 扩展基金详情任务、回调、长期事实查询和并发补全编排。

## Impact

- 数据库新增基金阶段业绩表和基金详情 slice 状态表。
- Server 新增一个只读阶段业绩接口和一个显式补全请求接口。
- 既有基金净值、股票历史和公司资料接口保持兼容。
- 不保存用户持仓、凭据、cookie 或完整上游响应。
