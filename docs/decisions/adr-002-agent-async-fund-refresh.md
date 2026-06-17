# ADR-002 Agent 异步基金刷新集成

## 1. 背景

此前 agent 侧基金刷新能力以本地 Python 命令、JSON 快照和 Markdown 报告为主。随着 server 成为长期业务事实源，基金明细刷新需要从“本地文件生成报告”升级为“server 编排任务、agent 刷新公开数据、server 接收结构化结果并提供查询”。

该决策会调整 `holdlens-agent/docs/decisions/adr-001-agent-runtime-boundary.md` 中“第一阶段优先 CLI/本地进程方式集成”的默认假设：本次需求直接采用 HTTP 异步任务和回调方式，但仍保持 server 是业务事实源、agent 是数据处理运行时的边界。

## 2. 决策

- server 负责通过后台任务创建基金刷新任务、记录任务状态、下发 agent 任务、接收回调、保存全局基金公开数据结果和提供查询接口。
- agent 负责根据 server 提供的基金代码刷新公开基金数据，并以结构化 JSON 回调 server。
- server 与 agent 之间采用异步 HTTP 任务 + 回调模式，而不是同步阻塞刷新，也不是读取 agent 本地 Markdown 文件。
- server 不直接接入东方财富、天天基金、AkShare 等外部数据源；外部数据源适配继续留在 agent。
- server 只向 agent 发送必要的基金代码、任务标识、契约版本、回调地址和系统级授权信息，不发送账户金额、真实账户名称或完整持仓组合。
- agent 回调结果进入 server 后，由 `processing` 域处理任务状态，由 `funddata` 域保存基金公开数据，由 `portfolio` 域提供账户资产事实；Case 层负责跨领域编排。
- 回调接口必须具备鉴权、契约版本校验、任务归属校验和幂等处理。

## 3. 备选方案

- 方案 A：server -> agent HTTP 异步任务，agent 完成后回调 server。
- 方案 B：server 调用 agent 同步接口并等待所有基金刷新完成。
- 方案 C：server 调用 agent CLI 或读取 agent 生成的 JSON/Markdown 文件。
- 方案 D：server 直接实现外部基金数据 provider，不再调用 agent。

## 4. 取舍原因

- 选择方案 A，因为基金刷新依赖外部公开接口，耗时和失败不可控；异步任务更适合表达后台运行中、部分失败、失败记录和查询数据新鲜度。
- 不选择方案 B，因为同步等待会让 server API 容易超时，并把外部行情接口波动直接暴露给用户请求链路。
- 不选择方案 C，因为本地 CLI/文件集成会让部署路径、文件权限、临时文件、Markdown 生成和读取时序变复杂，不利于 server 作为业务事实源。
- 不选择方案 D，因为外部数据源适配、字段清洗和 provider fallback 更适合 Python 生态；把这些能力搬进 server 会模糊 agent/server 边界。

## 5. 影响

- 正向影响：
  - server 保持长期事实源和任务编排职责。
  - agent 可以专注公开基金数据刷新和结构化结果输出。
  - 前端或外部调用方只依赖 server API，不关心 agent 本地文件。
  - 任务状态、warning、部分失败和回调诊断可以被 server 统一管理。
- 负向影响：
  - 引入 HTTP 服务化、回调鉴权、幂等、超时和重试复杂度。
  - server 与 agent 需要维护共享契约和版本兼容策略。
  - 本地开发需要同时启动 server 和 agent 服务，调试链路更长。
- 后续动作：
  - 在 server OpenSpec change `agent-async-fund-refresh` 中定义任务、回调、落库和查询契约。
  - 在 agent OpenSpec change `agent-fund-refresh-http-task` 中定义 HTTP 任务接口、刷新结果和回调行为。
  - 已确认：第一阶段系统级基金刷新默认允许联网，不新增联网授权记录或独立审计模型；回调鉴权第一阶段采用内部调用约定，后续再健全；回调失败最多由 agent 尝试 3 次，仍失败则标记 `callback_failed`，server 等待下一次后台调度补偿。
