# CLAUDE.md

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


## Compact Instructions

Preserve:

1. Architecture decisions (NEVER summarize)
2. Modified files and key changes
3. Current verification status (pass/fail commands)
4. Open risks, TODOs, rollback notes

## 5. Project-Specific Guidelines

### OpenSpec Source of Truth

- 始终使用简体中文回复；OpenSpec 产物、实现计划、验证摘要、评审记录和项目文档默认使用简体中文。
- OpenSpec 是本项目单次研发变更的行为增量、详细验收场景、研发设计、任务范围、变更历史和完成状态的事实源。
- 本项目 `docs/requirements/` 记录用户主动创建的 PRD 和版本规划，本项目 ADR 记录长期架构决策，已确认的外部契约作为集成输入；OpenSpec 只记录本项目单次变更的增量，不复制维护这些资料的完整内容。
- PRD 不是 OpenSpec 的必备前置。未经用户明确要求，不得创建或修改 PRD 的产品内容；没有 PRD 时，范围和成功标准已经清楚的需求仍可直接进入 OpenSpec。
- 除非用户明确要求，不要生成额外任务、实现计划、评审记录、验证报告或类似 Markdown 文档，避免形成双重事实来源。

### Superpowers

Superpowers 相关技能默认不自动加载，也不替代本文件中的 OpenSpec、实现授权、Git 和质量门规则。仅当用户明确要求，或本文件明确允许的触发条件满足时，才读取并使用对应 skill。

当前允许使用的 Superpowers skills：
- `brainstorming`

#### Brainstorming

当用户明确要求 brainstorming、需求仍模糊、存在多个可行方向，或需要先发散再收敛时，应优先读取并使用 `brainstorming` skill。

`brainstorming` 只负责澄清需求、探索方案、明确假设、范围边界和成功标准。

项目级约束优先：
- brainstorming 适用于需求澄清、方案探索和范围收敛场景。
- 用户授权创建或更新 OpenSpec 时，非琐碎的研发结论应收敛进 proposal、design、spec 和 tasks。
- OpenSpec 负责单次研发变更，不替代本项目 PRD、已确认契约或 ADR。
- 不默认写入 `docs/superpowers/specs/**`，除非用户明确要求。
- 不默认 commit，仍遵循本文件的 Git Standard。
- 不自动进入实现；生产代码、测试、迁移、脚手架等实现动作必须等待用户明确授权。
- 琐碎修复不强制进入 brainstorming。


### Required Project Skills

- 后端开发工作应加载 `xfg-ddd-skills`，并按其 DDD/六边形架构约定处理领域、用例/应用、基础设施和部署相关决策。
- 在生成涉及领域建模、限界上下文、分层边界、仓储/适配器或跨层设计的 DDD/六边形方案、架构设计、OpenSpec proposal 或设计文档前，加载 `xfg-ddd-skills` 并用它指导设计。普通的轻量分析如果不涉及 DDD 或架构，则不要加载它。

### Runtime Environment

- 默认后端项目已由用户启动，服务端口为 `8091`；不要自行尝试启动后端应用，也不要改用其他端口运行。
- 改动后端项目后，如果需要重新运行 application，应告知用户重新运行方式，并等待用户处理后再继续验证。

### Implementation Authorization

**不要因为一个方向听起来合理就直接写代码。**

- 探索、分析、方案评估、设计、创建或完善 OpenSpec proposal，默认不等同于实现授权。
- 在编写生产代码、测试、数据库迁移、生成产物或脚手架前，必须先确认需求、范围、假设和成功标准，并等待用户明确说“开始实现”“写代码”“按这个方案实现”“执行这个计划”“应用这个变更”或等价指令。
- 如果用户措辞既可能是在批准设计，也可能是在授权实现，编辑代码前先询问。
- OpenSpec proposal、design、spec 和 tasks 是规划产物；用户只要求创建或完善这些产物时，不要同时修改生产代码。

### Work Classification

编码前先分类工作：
- 琐碎修复：范围清楚的错别字、注释、格式或单行机械修正。OpenSpec 可选；保持范围狭窄并完成验证。
- 现有 OpenSpec change 内的小任务：阅读对应 OpenSpec 产物，按任务实现，并同步任务状态。
- 非琐碎变更：用户可见行为、跨模块变更、数据模型变更、接口变更、架构决策、风险较高的 bugfix、遗留兼容工作或多步骤实现。编码前先创建或更新 OpenSpec change。

对于遗留或测试薄弱区域：
- 设计或实现前先阅读现有代码。
- 在相关 OpenSpec spec 或 design 中记录当前行为、接口契约、依赖版本、代码风格和隐藏业务规则。
- 不要借一次功能变更清理无关的遗留问题。
- 为新行为和被触及的遗留路径添加测试。除非明确要求，不要尝试大范围补测试。

### Change Decomposition

- 在开始非琐碎变更前，必须先评估本次变更的 OpenSpec 粒度是否合适；评估依据包括变更边界、未确认问题、涉及模块、验证成本、回滚边界、发布原子性和实现顺序。
- 如果判断本次需求更适合拆成多个 OpenSpec change，必须先向用户说明拆分理由、建议拆分边界和预期收益，并等待用户确认。
- 拆分优先服务于实现效果：让每次改动更容易理解、验证、回滚和暂停，而不是为了同时推进或形式上拆小；不要把必须原子交付的契约、事务、权限校验、审计或数据迁移强行拆成多个可独立发布的 change。
- 能独立交付和独立验收的范围，可以拆成多个连续 OpenSpec change；不能独立交付但内部复杂的范围，应放在同一个 OpenSpec change 内分阶段实现和验证。

### OpenSpec Workflow

- 如需求仍模糊、存在多个可行方向，或用户明确要求发散思考，先按 `Superpowers` 中的 `Brainstorming` 规则澄清并收敛范围。
- 代码实现默认应从 OpenSpec 派生，优先使用 `openspec-apply-change`。
- 用户已经创建 PRD 时，将其视为 `openspec propose` 的产品输入材料；PRD 不替代 proposal、design、spec 或 tasks，OpenSpec 也不得反向复制维护完整 PRD。
- 如果没有 PRD 且想法仍模糊，先探索和澄清；范围和成功标准清楚后，询问是否可以基于当前上下文创建 OpenSpec change，不得自动补建 PRD。
- 创建或更新 OpenSpec 产物后，必须直接列出需要用户确认的点，包括关键假设、开放问题、范围边界和会影响实现/验收的决策；如果没有需要确认的点，也应明确说明“当前无待确认事项”，并说明是否已经可以等待实现授权。
- 非琐碎变更编码前必须先创建或更新 OpenSpec change；实现时遵循 `openspec-apply-change`。

### PRD 与版本总览

- 本项目 PRD、模板和版本总览统一维护在本项目 `docs/requirements/`；这些文档必须在独立检出本项目时仍然可用，不依赖上级仓库的需求文档。
- PRD 不记录 OpenSpec Change 名称，也不包含“OpenSpec 衔接”章节；PRD 与 Change 的映射只在对应版本的 `version-overview.md` 中维护。
- OpenSpec Change 明确承担某个 PRD 的交付时，在本项目版本总览中关联本地 `<change-name>`；没有 PRD 的技术性 Change 无需建立产品映射。
- 用户授权实现已经纳入版本规划的 PRD 时，开始实现后将 PRD 和版本总览状态同步为 `实现中`；关联范围全部完成并通过必要验证后同步为 `已实现`。
- 状态和映射可以随交付机械同步，但改变 PRD 的产品目标、范围、优先级、业务规则或验收标准仍需用户明确确认。

### 项目边界与安全

- `holdlens-server` 是账户、资产、持仓、基金清单、任务状态、权限和审计等长期业务事实源，负责持久化、写入决策、API 和任务编排。
- 外部数据处理服务只提供结构化处理结果；结果是否保存、覆盖、展示、归档或丢弃，由本项目决定。
- 默认不得读取、打印、上传或提交真实个人资产明细、账户标识、导出原始文件、API key、token、cookie、`.env`、`credentials*`、`secrets*` 或其他敏感信息。
- 涉及账户、资产、持仓、权限、审计或外部输入的变更，完成前必须检查用户隔离、数据暴露、幂等、事务和审计影响。
- 外部契约在当前会话不可用时，不得猜测跨系统字段或错误语义；先基于本项目已有代码、测试和 OpenSpec 判断，仍无法确认时向用户说明缺失输入。

### Command and Runbook Rules

- 执行环境、依赖、构建、测试、权限、端口或命令相关工作前，必须先阅读 `docs/agent-runbook.md`；遇到相同或高度相似的场景时，应优先尝试其中已验证解法。
- 当 AI / agent 遇到项目相关、可复用且已验证的问题解法后，应按 `docs/agent-runbook.md` 的流程和模板更新记录，并在回复中说明沉淀或更新了哪条经验。

### Git Standard

- 除非用户明确要求，不要运行 `git add`、`git commit` 或 `git push`。
- 在执行 `git commit` 前，先展示计划使用的提交信息和纳入文件范围，并请求确认。
- 纳入文件范围是提交前确认辅助信息，默认不是 commit message 的正文内容。
- 只有当纳入文件范围能解释跨模块影响、迁移风险、评审重点或其他不明显的分组原因时，才在 commit body 中提及。
- Commit message 应使用英文 Conventional Commit 类型，并配合简体中文描述，默认省略 scope，例如 `feat: 新增资产账户事实源` 或 `docs: 补充 git 提交规范`。
- 当前项目提交 scope 应优先体现 Spring Boot 业务事实源或边界上下文，例如 `feat(asset): 新增资产账户事实源`、`feat(holding): 新增基金持仓记录`、`fix(audit): 修复资产变更审计缺失`、`fix(auth): 修复持仓访问权限校验`。
- 如果账户、资产、持仓、权限、审计等多个业务能力相互独立，应拆成多个提交，而不是强行合并成一个带 scope 的提交。
- 如果一个业务能力横跨 api、case、domain、infrastructure 等多个 Spring Boot 模块，优先省略 scope，或使用清晰的业务/领域 scope，而不是罗列技术模块。
- 避免使用层名或模块名拼接 scope，例如 `feat(api,domain,infrastructure): 新增资产账户事实源`。

### Coding Standards

- 生成代码时，默认留空 author 字段。
- DDD/六边形架构实现应遵循 `xfg-ddd-skills` 约定，除非用户明确覆盖。
- MyBatis DAO SQL 默认必须使用 Mapper XML 实现。DAO 接口只声明方法，并使用必要的 MyBatis 绑定注解，例如 `@Mapper` / `@Param`；除非明确批准，不要使用 `@Select`、`@Insert`、`@Update` 或 `@Delete` 实现 SQL。
- 所有对外及内部 HTTP API 仅允许使用 `GET` 和 `POST`：`GET` 只能执行无副作用的查询，创建、更新、删除、归档、恢复等所有会改变状态的操作统一使用 `POST`，不得使用 `PUT`、`PATCH` 或 `DELETE`。
- 非创建类的 `POST` 写操作应使用能够表达业务意图的动作路径，例如 `/{id}/update-amount`、`/{id}/delete`，避免使用含义模糊的通用 `/update` 路径。
- 使用 `POST` 不得放宽写入边界：请求 DTO 只能暴露当前操作允许写入的字段，用例层和领域层必须继续执行权限校验、业务校验、审计、事务及必要的幂等控制，确保局部更新不会修改未授权字段。

### Commenting Guidelines

- 关键方法和关键逻辑应添加注释，但不要为了注释而注释；注释应作为给未来维护者和 AI 的“意图锚点”。
- 优先注释“为什么这么做”：业务规则来源、边界条件、非显然取舍、临时兼容逻辑、风险点，以及权限、事务、并发、金额计算等容易误改的逻辑。
- 避免复述代码已经清楚表达的“做了什么”，例如不要在 `queryUser()` 上方写“查询用户”这类低信息量注释。
- 方法级注释适用于领域服务、用例入口、复杂校验和跨系统适配器；重点说明业务语义、输入约束、失败条件和必须保持的不变量。
- 复杂代码块可以添加一两句短注释，说明该逻辑保护的业务约束或不能简化的原因。
- 注释应简洁、准确、可维护；当代码行为变更时，同步更新或删除已经失真的注释。

### Quality Gates and Review

质量门只定义进入下一阶段前的检查要求，不替代 OpenSpec 作为任务事实来源。

- 在声明工作已经修复、完成、可提交、可推送或可归档前，根据风险运行相关单元测试、集成测试、回归测试、build、lint、typecheck 或 package 命令。
- OpenSpec 相关变更必须确认 `tasks.md` 与实际完成状态一致，并运行 `openspec validate --strict <change>`。
- 对鉴权、支付、权限、数据暴露、外部输入或集成边界变更，必须明确检查安全影响。
- 如果检查失败，先复现并定位原因，再做最小修复并重新测试。

补充说明：
1. 如果代码写完后已经运行过必要质量门，并且之后没有再修改生产代码、测试、OpenSpec 文件或其他会影响检查结果的文件，提交前可以复用该次结果，不必重复运行。
2. 如果质量门失败后进行了修复，或者在质量门通过后又调整了实现、测试、tasks.md、spec 等相关文件，提交前应重新运行受影响的检查。
3. 如果质量门通过后间隔较久，或工作区发生较多变化，提交前应重新运行关键检查，避免基于过期结果判断可提交。
