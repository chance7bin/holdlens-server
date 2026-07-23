# ADR-001 Server 领域边界划分

> `portfolio`、`funddata`、`processing` 的领域边界继续有效；本 ADR 中 `asset_account`、`asset_info`、`asset_holding` 的具体模型建议已由 [ADR-005](adr-005-asset-record-and-watchlist-model.md) 替代。

## 1. 背景

HoldLens 的目标是“看清你的真实持仓”。当前仓库按运行时职责拆分为两个项目：

- `holdlens-server`：Spring Boot 常规业务系统，是长期业务事实源，负责核心业务模型、持久化、API、权限、审计、任务状态、调度和写入决策。
- `holdlens-agent`：Python agent 工具箱和数据处理运行时，负责外部行情/基金数据源适配、基金刷新、Markdown/JSON 转换、OCR/PDF、报告生成和实验性分析能力。

`holdlens-agent/docs/decisions/adr-001-agent-runtime-boundary.md` 已明确：agent 不直接拥有长期业务事实源；agent 接收输入并输出结构化结果，例如基金清单、刷新参数、`FundDetailSnapshot`、diff、warning 和 Markdown report；server 决定这些输出是否保存、覆盖、展示、归档或回滚。

server 当前仍主要是 DDD 分层骨架，真实业务代码较少；但数据库脚本和 agent 契约已经暴露出几组稳定业务概念：

- `asset_account`、`asset_info`、`asset_holding`、`asset_holding_change`：账户、资产、当前持仓和持仓变更记录。
- `fund`、`fund_top_holding`：基金公开信息和基金前十大重仓。
- `agent_warning`：解析、刷新、OCR、导入或 agent 处理过程中的警告。
- agent 契约中的 `AccountPortfolio`、`Position`、`FundPosition`、`FundDetailSnapshot`、`FundDetail`、`TopHolding` 等结构化模型。

因此需要先明确 server 的领域模型拆分方向，避免后续实现时把技术模块、agent provider、账户解析中间模型和长期业务事实源混在一起。

## 2. 决策

server 现阶段按 **3 个主领域模型 + 1 个预留横切领域** 划分。这里的“领域”首先指 `holdlens-server-domain` 下的业务包边界，不新增 Maven 模块；现有分层模块继续保持 `Trigger -> API -> Case -> Domain <- Infrastructure` 的依赖方向。

### 2.1 `portfolio`：资产持仓域

`portfolio` 是当前核心域，负责用户长期、权威、可审计的资产持仓事实源。

建议承载的业务对象：

- 资产账户：账户名称、账户类型、状态、备注和用户隔离。
- 资产主数据：资产代码、资产名称、资产大类、资产类型、市场和状态。
- 当前持仓：账户、资产、分类、来源、金额、币种、缺失原因和状态。
- 持仓变更记录：创建、更新、删除、导入、OCR、agent 或 API 同步造成的持仓变化。

建议聚合方向：

- `PortfolioAggregate` 或 `HoldingBookAggregate`：围绕某个用户的一组账户、资产和当前持仓，维护账户/资产/持仓的一致性。
- `HoldingChangeEntity`：作为持仓变化审计事实，不直接替代当前持仓。
- `MoneyAmountVO`、`AssetCodeVO`、`AccountTypeVO`、`HoldingSourceVO` 等值对象，用于统一金额、代码、账户类型和来源语义。

关键边界：

- agent 的 `AccountPortfolio`、`Position`、`FundPosition` 是输入契约或解析结果，不是 server 的长期事实源本身。
- 进入 server 后，是否新增账户、更新资产、覆盖持仓、记录变更或丢弃低置信度字段，由 `portfolio` 域和对应 Case 层用例决定。

### 2.2 `funddata`：基金公开数据域

`funddata` 是支撑域，负责保存和查询基金公开数据刷新结果。

建议承载的业务对象：

- 基金详情快照：契约版本、生成时间、快照状态、来源类型、来源引用和数据来源元信息。
- 基金详情项：基金代码、基金名称、申购状态、日申购限额、阶段收益、数据日期、公开重仓状态、字段来源和缺失原因。
- 基金前十大重仓：排名、股票名称、股票代码、市场、当日涨跌幅、持仓占比、较上季度变化和缺失原因。

建议聚合方向：

- `FundDetailSnapshotAggregate`：以一次刷新快照为聚合根，包含多只基金详情和每只基金的重仓项。
- `FundDetailEntity`：表达单只基金在某次快照中的公开数据。
- `TopHoldingEntity`：表达基金公开披露的前十大重仓。
- `PeriodReturnsVO`、`PercentageValueVO`、`PublicHoldingsStatusVO`、`BuyStatusVO` 等值对象。

关键边界：

- `funddata` 不拥有“用户持有哪些基金”的事实；它只保存某次刷新得到的公开基金数据。
- `fund` 以 `fund_code` 表达基金身份，不直接引用 `portfolio` 中的资产主数据。
- 东方财富、天天基金、AkShare 等 provider 细节不进入 `funddata` 领域模型；server 只关心结构化契约和写入决策。

### 2.3 `processing`：数据处理任务域

`processing` 是支撑域，负责 agent 调用、导入/刷新任务、处理状态、来源引用、联网授权、失败摘要和 warning。

建议承载的业务对象：

- 处理任务：任务类型、来源类型、来源引用、状态、启动/结束时间、执行摘要和错误信息。
- agent 调用记录：CLI 或未来服务化调用的输入摘要、输出引用、退出码、stdout/stderr 摘要和资源限制。
- 处理警告：解析警告、刷新警告、OCR 警告、导入警告和 agent 警告。
- 联网授权记录：在刷新公开行情数据前，记录用户是否明确授权发送基金代码和公开重仓股票代码到外部接口。

建议聚合方向：

- `ProcessingTaskAggregate`：围绕一次导入、OCR、基金刷新或 agent 分析任务维护状态流转和结果引用。
- `ProcessingWarningEntity`：保留可操作的 warning，不静默丢弃异常信息。
- `SourceRefVO`、`ProcessingStatusVO`、`WarningSeverityVO`、`NetworkAuthorizationVO` 等值对象。

关键边界：

- `processing` 决定任务生命周期和诊断信息，不直接改写资产持仓或基金公开数据。
- 任务完成后，由 Case 层编排 `processing`、`portfolio` 和 `funddata` 的协作；最终写入仍落在各自领域的仓储接口和领域规则中。

### 2.4 `identity`：身份与权限域，先预留

`identity` 是预留横切领域，短期不急于完整建模。

当前 SQL 已普遍出现 `user_id`，但还没有用户、角色、权限、家庭账户、共享账户或审计策略相关表。现阶段应先把 `user_id` 作为所有查询、写入和任务处理的强隔离条件；等真正出现登录、授权、共享资产、家庭成员、权限审计等需求时，再将其独立为 `identity` 或 `access` 域。

## 3. 备选方案

- 方案 A：按 `portfolio`、`funddata`、`processing` 三个主领域划分，并预留 `identity`。
- 方案 B：只建一个 `asset` 或 `holding` 大领域，把账户、资产、持仓、基金详情、agent warning 和任务状态都放在一起。
- 方案 C：按数据表一表一域，例如 `account`、`asset`、`holding`、`fund`、`warning`、`task` 等。
- 方案 D：把基金 provider、数据刷新和持久化策略都放到 server 的基金域中，由 Spring Boot 直接承接外部数据处理能力。

## 4. 取舍原因

- 选择方案 A，因为它符合“server 是长期业务事实源，agent 是数据处理工具箱”的边界，同时不会在业务代码尚少时过度拆分。
- 不选择方案 B，因为单个大领域会让持仓事实、基金公开快照、任务状态和 warning 混在一起，后续权限、审计、回滚和写入决策会变得模糊。
- 不选择方案 C，因为一表一域是数据结构驱动，不是领域边界驱动；账户、资产和当前持仓在业务上一致性很强，应优先归入 `portfolio`。
- 不选择方案 D，因为外部数据源适配、字段清洗、Markdown/JSON 转换和实验性分析更适合留在 Python agent；server 应通过契约接收结构化结果，并决定写入和展示。

## 5. 影响

- 正向影响：
  - `portfolio` 保持为“真实持仓”的核心事实源。
  - `funddata` 可以独立演进公开基金数据快照，不污染用户持仓事实。
  - `processing` 可以承接 agent 调用、导入、刷新、OCR 和 warning 的通用任务语义。
  - `identity` 延后完整建模，避免在权限需求未清晰前引入空壳复杂度。
  - 后续 Java DTO/Entity、Python Pydantic Model 和 JSON Schema 可以围绕明确边界对齐。
- 负向影响：
  - Case 层需要编排多个领域，例如一次基金刷新可能同时涉及 `processing`、`funddata` 和 `portfolio`。
  - 跨领域引用需要谨慎控制，避免 Domain Service 直接依赖其他领域的 Repository。
  - 需要维护 agent 契约和 server 领域模型之间的映射，不能简单复刻 Python 模型。
- 后续动作：
  - 新增 server OpenSpec change 时，应优先判断需求落在哪个领域。
  - 第一阶段优先实现 `portfolio` 和 `funddata` 的模型与仓储边界。
  - 引入 agent CLI 集成或刷新任务时，再补齐 `processing` 的任务表和状态流转。
  - 涉及登录、用户隔离、共享账户或审计策略时，再正式创建 `identity`/`access` 领域模型。
  - 不单独拆 `account`、`audit`、`taxonomy` 或 provider 领域，除非后续出现独立生命周期、独立规则和独立用例。
