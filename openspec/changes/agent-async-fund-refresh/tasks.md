## 1. 契约与文档

- [x] 1.1 确认 server PRD、ADR 与本 OpenSpec change 的范围一致。
- [x] 1.2 与 agent change `agent-fund-refresh-http-task` 对齐任务请求、回调结果、状态枚举、错误语义和 schema version。
- [x] 1.3 明确待用户确认事项：系统级联网授权口径；回调鉴权和回调失败处理策略已确认。

## 2. processing 任务模型

- [x] 2.1 设计并实现基金刷新处理任务持久化结构，包含任务状态、来源、agent 引用、错误摘要和时间字段。
- [x] 2.2 实现任务状态流转规则，覆盖 `created`、`dispatched`、`running`、`succeeded`、`partial_failed`、`failed`、`dispatch_failed`。
- [x] 2.3 实现回调幂等记录或等价机制，防止重复回调重复落库。
- [x] 2.4 验证：任务创建、状态流转和重复回调处理测试通过。

## 3. agent 下发与回调

- [x] 3.1 在 Domain 定义 agent 基金刷新 Port，Infrastructure 实现 HTTP 调用 agent。
- [x] 3.2 新增任务创建 Case，负责任务落库、调用 agent 和状态更新。
- [x] 3.3 新增 agent 回调 Trigger，完成鉴权、schema version、任务归属和幂等校验。
- [x] 3.4 支持记录 agent 最终 `callback_failed` 诊断状态，第一阶段不主动轮询 agent 补结果。
- [x] 3.5 验证：agent 下发成功、下发失败、未授权回调、未知任务回调、重复回调、callback_failed 诊断测试通过。

## 4. funddata 落库

- [x] 4.1 映射 agent 回调结果到基金详情快照、基金详情项、前十大重仓、数据来源和 warning。
- [x] 4.2 支持成功、部分失败和失败回调的差异化落库与任务状态更新。
- [x] 4.3 保留字段来源和缺失原因，避免用空字符串表达未知状态。
- [x] 4.4 验证：成功回调、部分失败回调、缺失字段和 warning 保存测试通过。

## 5. portfolio 查询编排

- [x] 5.1 新增账户资产与全局最新基金详情组合查询 Case。
- [x] 5.2 确保查询先读取当前用户 `portfolio` 持仓，再用持仓基金代码查询 `funddata` 全局最新基金详情。
- [x] 5.3 确保查询以 `portfolio` 持仓事实为主，不使用基金详情覆盖账户、资产金额或持仓。
- [x] 5.4 在缺少基金详情时返回可表达的 unavailable/missing/stale 状态。
- [x] 5.5 验证：有基金详情、无基金详情、未持有基金不返回、跨用户隔离和 stale 状态测试通过。

## 6. 质量门

- [x] 6.1 运行相关单元测试、集成测试或 Maven 模块测试。
- [x] 6.2 运行 `openspec validate --strict agent-async-fund-refresh`。
- [x] 6.3 从产品、工程、QA、发布、安全五个视角做轻量评审，并确认没有真实资产明细、账户标识或凭据进入日志和文档。

## 7. 分层边界修正

- [x] 7.1 API 层定义基金刷新与组合查询接口，Trigger 层实现这些接口。
- [x] 7.2 Case 层改用自身命令/结果模型，不再依赖 API DTO 或 `holdlens-server-api`。
- [x] 7.3 验证：`holdlens-server-case` 不存在 `holdlens-server-api` Maven 依赖和 API import，相关 Maven 测试与 OpenSpec 校验通过。
