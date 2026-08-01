# Change: 退役全市场基金阶段收益刷新任务

## Why

基金详情已经通过活动基金 `period_performance` 切片获取阶段业绩，旧 `fund_period_return_refresh` 全市场任务默认关闭且没有独立消费方，继续保留会形成重复抓取和两套字段所有权。

## What Changes

- 删除 Server 的旧任务类型、定时入口、手动编排能力、Agent Port 路由和 callback API。
- 删除旧任务配置及 callback 处理分支，不再创建或接受该任务。
- 保留既有基金收益摘要字段的只读兼容，不执行数据库删列。

## Impact

- 影响 Processing/FundData Case、Domain、Infrastructure、Trigger、配置、契约和测试。
- 新的 `period_performance` 活动基金刷新继续作为详情页阶段业绩事实源。
