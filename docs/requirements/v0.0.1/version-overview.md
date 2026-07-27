# v0.0.1

## 迭代目标

建立 server 与基金数据处理服务的异步集成能力：server 作为长期业务事实源和任务编排方，下发基金公开数据刷新任务、接收结构化结果、保存可用数据，并提供账户资产与基金明细查询能力。

## 范围内 PRD

| 模块 | PRD | 涉及端 | 状态 | 关联 OpenSpec Change |
| --- | --- | --- | --- | --- |
| 估值与收益 | [基金明细异步刷新任务](shared/prd-shared-fund-detail-refresh-task.md) | 跨端 | 已实现 | [`agent-async-fund-refresh`](../../../openspec/changes/agent-async-fund-refresh/proposal.md) |

## 不包含范围

- 不实现投资建议、交易建议、买卖信号或风险评分。
- 不要求 server 直接接入外部基金数据源。
- 不继续依赖数据处理服务的本地 Markdown 报告作为查询事实源。
- 不在本版本解决完整登录、角色、家庭账户或共享账户权限模型。

## 依赖与风险

- 依赖外部数据处理服务提供结构化刷新能力。
- 依赖双方约定任务、结果、状态和错误语义。
- 外部数据源波动和异步结果交付会增加部分失败、幂等和状态一致性风险。

## 决策记录

- [ADR-001 Server 领域边界划分](../../decisions/adr-001-server-domain-boundaries.md)
- [ADR-002 Agent 异步基金刷新集成](../../decisions/adr-002-agent-async-fund-refresh.md)
