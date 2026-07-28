# Agent Guidelines

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.


## 5. Project-Specific Guidelines

### Skill Rules

- 默认不自动读取或调用 `brainstorming`；只有用户在当前请求中明确指定时才允许使用。
- 需求模糊、存在多个方向或需要方案探索，不构成自动触发条件；未显式指定时按普通对话澄清。
- `brainstorming` 只负责需求澄清、方案探索和成功标准收敛，不自动创建文档、进入实现或执行 Git 操作。

### Implementation Authorization

- 探索、分析、评审、设计或创建 OpenSpec 产物不等于实现授权；只要求这些内容时，不修改生产代码、测试、迁移或脚手架。
- 用户明确要求修改、修复、构建、实现或应用变更时视为实现授权；只有措辞存在会实质改变范围的歧义时才先询问。

### OpenSpec and Product Documents

- OpenSpec 只维护本项目单次变更的行为增量、设计、验收场景、任务和状态，默认使用简体中文；不得复制完整 PRD、ADR 或已确认契约。
- PRD 记录产品意图和业务范围，由用户按需创建或明确授权 Agent 创建；PRD 不是 OpenSpec 前置条件。
- `docs/requirements/README.md` 说明本项目 PRD、版本总览及其映射规则；本地 PRD 与 Change 的映射只维护在对应版本的 `version-overview.md`。
- ADR、外部契约、代码和 Git 各自保持原有事实源职责。除非用户明确要求，不新增额外计划、评审或验证 Markdown 文档。

### OpenSpec Routing

- 琐碎变更（错别字、注释、格式或单处机械修正）可直接实现并完成必要验证，OpenSpec 可选。
- 已有 OpenSpec change 内的任务，先读取对应 proposal、design、spec 和 tasks，再按任务实现并同步状态。
- 用户可见行为、跨模块、数据模型、接口、架构、高风险 bugfix、遗留兼容或多步骤实现属于非琐碎变更；编码前必须已有范围匹配的 OpenSpec change。
- 需要新建或调整 change 时，先确认范围和成功标准；用户未授权创建或更新 OpenSpec 时，先说明原因并等待确认，不自动补建 PRD。
- 只有能独立交付、验证和回滚的范围才拆分 change；若拆分会影响原子交付，则在同一 change 内分阶段实现。建议拆分多个 change 前先说明边界和收益，并等待用户确认。
- 创建或更新 OpenSpec 后，列出仍需用户确认的关键假设、开放问题和范围边界；没有时明确说明。
- 具体的提案、实现和归档流程分别交由 `openspec-propose`、`openspec-apply-change` 和 `openspec-archive-change` skill 处理。
- 用户授权实现已纳入版本规划的 PRD 时，可以随交付同步状态和映射；改变产品目标、范围、优先级、业务规则或验收标准仍需用户明确确认。

### Project Boundaries and Security

- `holdlens-server` 是账户、资产、持仓、基金清单、任务状态、权限和审计等长期业务事实源，负责持久化、写入决策、API 和任务编排。
- 外部数据处理服务只提供结构化处理结果；结果是否保存、覆盖、展示、归档或丢弃，由本项目决定。
- 默认不得读取、打印、上传或提交真实个人资产明细、账户标识、导出原始文件、API key、token、cookie、`.env`、`credentials*`、`secrets*` 或其他敏感信息。
- 涉及账户、资产、持仓、权限、审计或外部输入的变更，完成前必须检查用户隔离、数据暴露、幂等、事务和审计影响。
- 外部契约在当前会话不可用时，不得猜测跨系统字段或错误语义；先基于本项目已有代码、测试和 OpenSpec 判断，仍无法确认时向用户说明缺失输入。

### Required Project Skills

- 涉及领域建模、限界上下文、分层边界、仓储/适配器、跨层设计或相关 OpenSpec/架构文档时，加载 `xfg-ddd-skills` 并按其 DDD/六边形约定处理。
- 普通轻量分析和不改变上述边界的局部实现不自动加载该 skill，优先匹配现有代码结构与惯例。

### Runtime Environment

- 本地后端服务统一使用 `8091` 端口；非联调任务不要为了普通代码修改额外启动应用，也不要改用其他端口。
- 进行本地前后端联调、API 验证或浏览器验收时，可按 `docs/agent-runbook.md` 自行启动、停止或重启 `holdlens-server-app`，无需等待用户操作。
- 自行管理运行状态时，只处理已确认的本项目进程和 `8091` 端口，遵循 runbook 中的构建顺序与安全边界，并在完成时说明最终运行状态。

### Command and Runbook Rules

- 执行环境、依赖、构建、测试、权限、端口或命令相关工作前，必须先阅读 `docs/agent-runbook.md`；遇到相同或高度相似的场景时，应优先尝试其中已验证解法。
- 当 AI / agent 遇到项目相关、可复用且已验证的问题解法后，应按 `docs/agent-runbook.md` 的流程和模板更新记录，并在回复中说明沉淀或更新了哪条经验。

### Git Standard

- 除非用户明确要求，不要运行 `git add`、`git commit` 或 `git push`。
- 在执行 `git commit` 前，先展示计划使用的提交信息和纳入文件范围，并请求确认。
- 提交前检查纳入范围是否包含个人资产、账户标识、导出文件、凭据或其他敏感信息；未经明确确认不得提交。
- Commit message 使用英文 Conventional Commit 类型和简体中文描述，例如 `feat(asset): 新增资产账户事实源`。scope 只在业务域或边界上下文清晰时使用；跨多个技术层时省略，独立能力应拆分提交。

### Coding Standards

- 生成代码时，默认留空 author 字段。
- DDD/六边形架构实现应遵循 `xfg-ddd-skills` 约定，除非用户明确覆盖。
- MyBatis DAO SQL 默认必须使用 Mapper XML 实现。DAO 接口只声明方法，并使用必要的 MyBatis 绑定注解，例如 `@Mapper` / `@Param`；除非明确批准，不要使用 `@Select`、`@Insert`、`@Update` 或 `@Delete` 实现 SQL。
- 所有对外及内部 HTTP API 仅允许使用 `GET` 和 `POST`：`GET` 只能执行无副作用的查询，创建、更新、删除、归档、恢复等所有会改变状态的操作统一使用 `POST`，不得使用 `PUT`、`PATCH` 或 `DELETE`。
- 非创建类的 `POST` 写操作应使用能够表达业务意图的动作路径，例如 `/{id}/update-amount`、`/{id}/delete`，避免使用含义模糊的通用 `/update` 路径。
- 使用 `POST` 不得放宽写入边界：请求 DTO 只能暴露当前操作允许写入的字段，用例层和领域层必须继续执行权限校验、业务校验、审计、事务及必要的幂等控制，确保局部更新不会修改未授权字段。

### Commenting Guidelines

- 匹配周围代码的注释密度，只解释业务规则来源、非显然取舍、边界条件、兼容原因和必须保持的不变量，不复述代码已经清楚表达的行为。
- 注释应简洁、准确并随行为同步更新；失真的注释应修改或删除。

### Quality Gates and Review

- 在声明工作已经修复、完成、可提交、可推送或可归档前，根据风险运行相关单元测试、集成测试、回归测试、build、lint、typecheck 或 package 命令。
- OpenSpec 相关变更必须确认 `tasks.md` 与实际完成状态一致，并运行 `openspec validate --strict <change>`。
- 对鉴权、支付、权限、数据暴露、外部输入或集成边界变更，必须明确检查安全影响。
- 如果检查失败，先复现并定位原因，再做最小修复并重新测试。
- 可复用仍然有效的检查结果；检查失败后修复、或通过后又修改会影响结果的代码、测试或 OpenSpec 产物时，只重跑受影响的检查。
