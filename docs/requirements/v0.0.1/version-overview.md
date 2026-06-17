# v0.0.1

## 迭代目标

建立 HoldLens 第一阶段的 server 与 agent 集成需求入口：由 server 作为长期业务事实源和任务编排方，向 agent 下发基金公开数据刷新任务；agent 完成刷新后回调 server；server 保存结构化结果，并向前端或外部调用方提供账户资产与基金明细查询接口。

## 范围内 PRD

| 模块 | PRD | 涉及端 | 状态 | 关联 OpenSpec Change |
| --- | --- | --- | --- | --- |
| 估值与收益 | [基金明细异步刷新任务](shared/prd-shared-fund-detail-refresh-task.md) | 跨端 | 已实现 | `agent-async-fund-refresh` |

## 不包含范围

- 不实现投资建议、交易建议、买卖信号或风险评分。
- 不要求 server 直接接入东方财富、天天基金、AkShare 等外部基金数据源。
- 不继续依赖 agent 本地 Markdown 报告作为 server 查询数据源。
- 不在本版本解决完整登录、角色、家庭账户或共享账户权限模型。

## 依赖与风险

- 依赖 agent 提供基于基金代码的结构化刷新任务接口和回调能力。
- 依赖 server 与 agent 之间约定任务请求、回调结果、错误语义、鉴权、超时和重试规则。
- 风险在于回调失败、重复回调、部分基金刷新失败或外部公开行情接口波动时，需要产品上清楚表达任务状态与可见结果。

## 决策记录

- [ADR-001 Server 领域边界划分](../../decisions/adr-001-server-domain-boundaries.md)
- [ADR-002 Agent 异步基金刷新集成](../../decisions/adr-002-agent-async-fund-refresh.md)
