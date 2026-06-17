## Context

server 已通过 ADR-001 明确领域边界：`portfolio` 是真实持仓事实源，`funddata` 保存公开基金数据快照，`processing` 管理 agent 调用、任务状态和 warning。agent 的 ADR-001 明确 agent 不拥有长期业务事实源，只输出结构化结果。

本 change 覆盖 server 侧异步基金刷新编排：server 后台任务触发刷新，向 agent 发送基金代码，agent 刷新完成后回调 server，server 保存全局结构化基金详情，并通过 API 组合返回账户资产和基金明细。

## Goals / Non-Goals

**Goals:**

- 定义 server 创建基金刷新任务、下发 agent、接收回调和更新任务状态的流程。
- 定义 server 保存 agent 回调结果的领域落点和事务边界。
- 定义账户资产与基金明细组合查询能力。
- 定义 server 与 agent 之间的最小数据暴露、安全校验、幂等和错误语义。
- 明确 server 不读取 agent Markdown 文件，不要求 agent 为 server 链路生成 Markdown。

**Non-Goals:**

- 不实现 server 直接抓取外部公开基金数据。
- 不在 server 中实现东方财富、天天基金或 AkShare provider。
- 不实现完整登录、角色、家庭账户或共享账户权限体系。
- 不实现投资建议、交易建议、风险评分或自动调仓。
- 不迁移或解析 agent 历史 Markdown 报告内容。

## Decisions

### server 采用异步任务 + 回调集成 agent

server 后台调度创建 `fund_detail_refresh` 类型处理任务，持久化任务状态，然后通过 `IAgentFundRefreshPort` 下发给 agent。agent 完成后回调 server 的内部接口。server 校验回调后保存全局基金公开数据结果并更新任务状态。

同步接口会把外部公开行情接口的耗时和失败直接放大到用户请求链路；本地 CLI/文件集成会引入部署路径、进程治理和文件时序问题。因此本 change 采用异步 HTTP 集成。

### 回调结果由 processing 与 funddata 分工处理

`processing` 负责任务状态、回调幂等、失败摘要和 warning 归档；`funddata` 负责基金公开详情快照、基金详情项和前十大重仓；`portfolio` 只提供账户资产事实，不被基金详情反向覆盖。

Case 层负责编排多个领域，Domain Service 不直接依赖其他领域的 Repository。

### API 契约由 Trigger 实现，Case 不依赖 API DTO

API 层只定义对外接口、请求、响应和 DTO 契约；Trigger 层实现 API 接口并负责 HTTP 入口、鉴权、参数校验以及 API DTO 与 Case 命令/结果模型之间的转换。Case 层只接收自身的命令/结果模型并编排 Domain，不直接依赖 `holdlens-server-api`，避免外部契约变更污染用例编排层。

### 账户资产查询以 portfolio 为主事实

账户资产与基金明细组合查询必须以 `portfolio` 的账户、资产和当前持仓为主。基金明细按基金代码从全局最新成功或部分成功中可用的基金详情取数。若某个基金缺少最新明细，查询仍返回账户资产主体，并在基金明细部分表达暂无数据、刷新失败或已过期。

查询编排顺序应固定为：先按当前用户读取 `portfolio` 持仓，再从持仓中的基金代码或资产代码提取 `fund_code` 集合，最后用这些 `fund_code` 到 `funddata` 查询全局最新基金详情并拼接返回。`funddata` 中存在但用户未持有的基金明细不得出现在该用户账户资产结果中。

### server 只向 agent 暴露必要基金代码

server 下发任务时只发送 `server_task_id`、`fund_codes`、`schema_version`、`callback_url` 和系统级授权信息。默认不发送账户名称、资产金额、完整持仓组合或个人资产明细。

### 回调必须可校验且幂等

agent 回调必须携带 server 任务标识和结果契约版本。server 必须校验鉴权、任务存在、任务归属、状态可迁移和幂等键。重复回调不得重复写入同一快照，也不得让任务状态从终态回退。

### Markdown 从业务链路中移除

server 查询不得读取 `holdlens-agent/finance/基金明细跟踪.md` 或其他 agent 本地 Markdown 报告。Markdown 可继续作为 agent 本地工具输出，但不是 server 查询事实源。

## Data / Contract Sketch

server 下发给 agent 的任务请求建议包含：

```json
{
  "schema_version": "fund-detail-refresh-task/v1",
  "server_task_id": "task_123",
  "fund_codes": ["000001", "161725"],
  "allow_network": true,
  "callback_url": "http://server/internal/agent/fund-detail-refresh/callback"
}
```

agent 回调 server 的结果建议包含：

```json
{
  "schema_version": "fund-detail-refresh-result/v1",
  "server_task_id": "task_123",
  "idempotency_key": "task_123:result:1",
  "status": "succeeded",
  "generated_at": "2026-06-16T10:00:00Z",
  "funds": [],
  "data_sources": [],
  "refresh_warnings": []
}
```

具体 DTO、字段枚举、错误码和表结构在实现阶段根据 OpenSpec spec 细化。

## Persistence Impact

预计需要新增或调整：

- `processing_task`：记录 server 创建的刷新任务、状态、来源、agent 引用、错误摘要和时间。
- `processing_callback` 或等价幂等记录：记录回调幂等键、处理状态和关联任务。
- `agent_warning`：继续保存解析、刷新、agent warning，并关联任务或快照。
- `fund_detail_snapshot`、`fund_detail_item`、`fund_top_holding`：保存结构化基金公开数据。

数据库设计应避免保存真实账户金额到 agent 调用参数或回调诊断字段中。

## Error Semantics

- `dispatch_failed`：server 未能成功把后台任务发送给 agent。
- `running`：agent 已接受任务或任务仍在刷新中。
- `succeeded`：所有请求基金均成功得到可保存结果。
- `partial_failed`：部分基金成功，部分基金失败或字段缺失。
- `failed`：任务无法产生可保存结果，或回调契约无效。
- `callback_rejected`：鉴权、任务归属、幂等或契约版本校验失败。

## Security / Privacy

- 回调接口必须是内部接口或具备签名/token 鉴权。
- server 不向 agent 发送账户金额、账户名称或完整持仓组合。
- 日志只能记录任务 ID、基金代码数量、状态、错误码和摘要，不记录个人资产明细。
- 账户资产查询必须带 `user_id` 隔离；基金公开数据刷新和基金详情写入按全局基金代码维度处理，不绑定具体用户。

## Risks / Trade-offs

- [回调失败或重复回调] -> 使用任务状态机和幂等键保护。
- [外部行情接口不稳定] -> 允许部分失败，保存成功部分和 warning。
- [契约版本演进] -> 请求和回调都必须包含 `schema_version`，未知版本拒绝处理。
- [跨领域编排复杂] -> Case 层编排，Domain 层保持各自领域规则。
- [过早服务化增加本地开发成本] -> 通过 OpenSpec 固定契约，优先实现最小 HTTP 任务接口和回调链路。

## Open Questions

当前无。

## Resolved Decisions

- 第一阶段系统级基金刷新默认允许联网，不新增联网授权记录或独立审计模型；任务审计先复用 `processing_task` 的任务类型、来源、状态、时间、错误摘要和日志 trace。后续如需要合规审计或可配置联网策略，再通过新的 OpenSpec change 扩展。
- 回调鉴权第一阶段采用内部调用约定，不引入复杂签名、网关鉴权或完整服务间认证；后续再通过新的变更健全。
- 回调失败最多由 agent 尝试 3 次，退避节奏为立即、10 秒、60 秒；仍失败则标记 `callback_failed`，server 不主动轮询补结果，等待下一次后台调度补偿。
