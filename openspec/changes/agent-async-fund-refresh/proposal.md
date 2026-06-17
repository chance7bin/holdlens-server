## Why

server 需要从依赖 agent 本地生成文件，升级为通过后台任务编排基金公开数据刷新，并通过结构化回调接收 agent 结果。这样账户资产和基金明细查询都可以由 server API 提供，保持 server 作为长期业务事实源和写入决策方。

## What Changes

- 新增基金明细异步刷新任务能力：server 后台创建全局任务、下发 agent、记录状态和处理结果。
- 新增 agent 回调接收能力：server 校验回调来源、任务、幂等键和契约版本后保存结构化结果。
- 新增账户资产与基金明细组合查询能力：server 以账户资产为主事实，按基金代码关联全局最新基金公开明细。
- 调整集成方式：server 不再读取 agent 本地 Markdown 报告，也不要求 agent 为 server 链路生成 Markdown。
- 强化安全边界：server 只向 agent 发送必要基金代码，不发送账户金额、真实账户名称或完整持仓组合。

## Capabilities

### New Capabilities

- `agent-async-fund-refresh`: server 编排 agent 异步基金明细刷新任务、接收回调并提供账户资产与基金明细查询。

### Modified Capabilities

- 无。

## Impact

- 影响 server 的 `processing`、`funddata`、`portfolio` 三个领域边界和 Case 层编排。
- 影响 server 对外 API：新增刷新任务创建、任务查询、agent 回调和账户资产基金明细查询接口。
- 影响数据库：需要新增或调整处理任务、基金详情快照、warning、幂等记录等持久化结构。
- 影响 agent 集成：需要 HTTP 任务接口、回调契约、鉴权、超时和重试约定。
- 影响安全：需要防止未授权回调、后台任务伪造写入和敏感资产信息外泄。
