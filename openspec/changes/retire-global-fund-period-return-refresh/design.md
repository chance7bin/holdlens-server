# Design: 退役全市场基金阶段收益任务

## Decisions

1. Server 不再暴露 `fund_period_return_refresh` 任务创建、派发和 callback 能力。
2. `processing_task` 的基金切片任务集合移除该类型；已有历史终态记录继续作为普通历史数据保留，不迁移或删除。
3. `fund` 表中的收益摘要和 `period_return_fetched_at` 暂时只读保留，避免本次清理引入破坏性数据库迁移和旧客户端响应变化。
4. 基金详情阶段业绩继续由 `market_detail_data_refresh` 的 `period_performance` 切片负责，活动集合和 7 天新鲜度规则不变。

## Rollback

回滚代码和配置即可恢复旧 endpoint；本次没有数据库结构变更。

## Open Questions

无。
