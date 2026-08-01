## REMOVED Requirements

### Requirement: 阶段收益只保存当前快照和覆盖状态

**Reason**: 详情阶段业绩已经迁移到活动基金 `period_performance` 切片，不再维护全市场阶段收益任务。

**Migration**: 删除旧任务调度、派发和 callback；保留既有收益摘要只读兼容。

#### Scenario: 拒绝旧阶段收益任务回调

- **WHEN** 调用方继续访问旧阶段收益 callback
- **THEN** Server SHALL 不再提供该业务路由
- **AND** MUST NOT 更新基金收益摘要。

## MODIFIED Requirements

### Requirement: 四类基金刷新任务具有独立契约

Server SHALL 只维护基金目录、申购状态、重仓和资产配置刷新任务；阶段业绩 SHALL 由市场详情 `period_performance` 切片负责。

#### Scenario: 不再创建全市场阶段收益任务

- **WHEN** 定时调度或手动入口执行基金切片刷新
- **THEN** Server MUST NOT 创建 `fund_period_return_refresh` 任务
- **AND** MUST NOT 向 Agent 调用 `/tasks/fund-period-return-refresh`。
