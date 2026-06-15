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
- OpenSpec 是产品意图、需求、验收标准、设计决策、任务范围、变更历史和完成状态的唯一事实来源。
- 除非用户明确要求，不要生成额外任务、实现计划、评审记录、验证报告或类似 Markdown 文档，避免形成双重事实来源。

### Superpowers

Superpowers 相关技能默认不自动加载，也不替代本文件中的 OpenSpec、实现授权、Git 和质量门规则。仅当用户明确要求，或本文件明确允许的触发条件满足时，才读取并使用对应 skill。

当前允许使用的 Superpowers skills：
- `.codex/skills/superpowers/skills/brainstorming`

#### Brainstorming

当用户明确要求 brainstorming、需求仍模糊、存在多个可行方向，或需要先发散再收敛时，应优先读取并使用 `.codex/skills/superpowers/skills/brainstorming`。

`brainstorming` 只负责澄清需求、探索方案、明确假设、范围边界和成功标准。

项目级约束优先：
- brainstorming 适用于单 Agent 和多 Agent 场景。
- brainstorming 不代表已获得实现授权。
- 非琐碎结论必须收敛进 OpenSpec proposal、design、spec 和 tasks。
- OpenSpec 仍是需求、设计、验收标准和任务状态的唯一事实来源。
- 不默认写入 `docs/superpowers/specs/**`，除非用户明确要求。
- 不默认 commit，仍遵循本文件的 Git Standard。
- 不自动进入实现；生产代码、测试、迁移、脚手架等实现动作必须等待用户明确授权。
- 琐碎修复不强制进入 brainstorming。


### Required Project Skills

- 前端开发工作应加载并遵循 `mall-frontend-development`；该技能负责前端设计参考、样式、交互和模块边界指导。
- 后端开发工作应加载 `xfg-ddd-skills`，并按其 DDD/六边形架构约定处理领域、用例/应用、基础设施和部署相关决策。
- 在生成涉及领域建模、限界上下文、分层边界、仓储/适配器或跨层设计的 DDD/六边形方案、架构设计、OpenSpec proposal 或设计文档前，加载 `xfg-ddd-skills` 并用它指导设计。普通的轻量分析如果不涉及 DDD 或架构，则不要加载它。

### Runtime Environment

前端运行环境：
- 默认前端项目已由用户启动；不要自行尝试启动或重新运行前端开发服务。
- 测试前端页面时，优先按模块打开对应端口：`admin-web` 使用 `http://localhost:5673`，`user-map` 使用 `http://localhost:5675`，`merchant-mp` 使用 `http://localhost:5676`。
- 如果识别到目标前端项目未启动，或改动后需要重新运行前端项目，应告知用户对应启动或重新运行方式，并等待用户处理后再继续验证。

后端运行环境：
- 默认后端项目已由用户启动，服务端口为 `8091`；不要自行尝试启动后端应用，也不要改用其他端口运行。
- 改动后端项目后，如果需要重新运行 application，应告知用户重新运行方式，并等待用户处理后再继续验证。

### Implementation Authorization

**不要因为一个方向听起来合理就直接写代码。**

- 探索、分析、方案评估、设计、创建或完善 OpenSpec proposal，默认不等同于实现授权。
- 在编写生产代码、测试、数据库迁移、生成产物或脚手架前，必须先确认需求、范围、假设和成功标准，并等待用户明确说“开始实现”“写代码”“按这个方案实现”“执行这个计划”“应用这个变更”或等价指令。
- 如果用户措辞既可能是在批准设计，也可能是在授权实现，编辑代码前先询问。
- OpenSpec proposal、design、spec 和 tasks 是规划产物；用户只要求创建或完善这些产物时，不要同时修改生产代码。
- 契约骨架、DTO、接口、mock、controller 路由等都属于代码实现，必须在用户明确授权实现后才能修改。

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

### Multi-Agent Collaboration

- 多 Agent 协作默认遵循以下 workflow：
  1. 先用 OpenSpec 产出或完善 `proposal.md`、`design.md`、`specs/*/spec.md` 和 `tasks.md`。
  2. 总结假设、开放问题和待用户确认点。
  3. 等用户明确授权实现后，才进入代码实现。
  4. 如果变更属于中大型、跨模块、跨端、权限、交易、数据一致性等高风险场景，或用户明确要求规划/执行多 Agent 协作，则加载 `openspec-multi-agent-collaboration` 并按该 skill 组织角色分工、质量门和最终集成。
- 小修复、小任务默认由单 Agent 完成。
- `openspec-apply-change` 是 OpenSpec change 的默认实现入口，负责读取 change、推进 `tasks.md`、同步任务状态和处理暂停条件。
- `openspec-multi-agent-collaboration` 不替代 `openspec-apply-change`；它只在复杂或高风险变更中补充角色分工、契约先行、评审和集成收口。
- 使用多 Agent 时，应由一个主执行者按 `openspec-apply-change` 读取上下文和任务状态，再按 `openspec-multi-agent-collaboration` 分配清晰任务切片。主执行者指定一个 Integration Agent 负责最终集成、质量门验证和 `tasks.md` 同步；主执行者对最终状态确认负责。单 Agent 场景下，主执行者同时承担 Integration Agent 职责。
- 避免“双调度”：不要让多个 Agent 各自独立推进同一批 OpenSpec tasks，最终完成状态只能由统一收口者确认。

### OpenSpec Workflow

- 代码实现默认应从 OpenSpec 派生，优先使用 `openspec-apply-change`。
- 创建新的 OpenSpec change 前，优先从 `docs/requirements/prd-*.md` 中的 PRD 开始。
- 将 PRD/需求文档视为 `openspec propose` 的输入材料，而不是 OpenSpec proposal、design、spec 或 tasks 产物的替代品。
- 如果没有 PRD 且想法仍模糊，先探索和澄清；如果范围和成功标准已清楚，询问是否可以基于当前上下文创建 OpenSpec change。
- 创建或更新 OpenSpec 产物后，总结已记录的决策、假设、开放问题和仍需用户确认的点，并说明是否已经可以等待实现授权。
- 非琐碎变更编码前必须先创建或更新 OpenSpec change；实现时遵循 `openspec-apply-change`。

### PRD Linkage

- 如果 OpenSpec change 直接覆盖 `docs/requirements/**/prd-*.md` 下的 PRD，应更新对应版本 README 的 `关联 OpenSpec Change`。
- 只关联 proposal、design、spec 或 tasks 直接覆盖的 PRD；共享数据模型、命名边界或下游影响本身，不代表所有受影响 PRD 都应关联。
- 关联 PRD 的实现开始时，将状态更新为 `实现中`；关联范围完成并通过实现、测试、评审和 OpenSpec 验证后，将状态更新为 `已实现`。
- 如果实现过程中发现覆盖范围变化，应同步更新 README 中的关联关系和状态。

### Command and Runbook Rules

- 执行环境、依赖、构建、测试、权限、端口或命令相关工作前，如存在失败或重复排查风险，应先查阅 `docs/agent-runbook.md`，并优先尝试其中已验证解法。
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

### Quality Gates and Review

质量门是在进入下一阶段前必须通过的检查。它不是新的任务事实来源。

- 在声明工作已经修复、完成、可提交、可推送或可归档前，根据风险运行相关单元测试、集成测试、回归测试、build、lint、typecheck 或 package 命令。
- OpenSpec 相关变更必须确认 `tasks.md` 与实际完成状态一致，并运行 `openspec validate --strict <change>`。
- 直接关联 PRD 的变更，必须确认 PRD README 状态和 OpenSpec change 关联准确。
- 对鉴权、支付、权限、数据暴露、外部输入或集成边界变更，必须明确检查安全影响。
- 对较大、风险较高、跨模块或面向用户的变更，用产品、工程、QA、发布、安全五个视角做轻量评审。
- 如果检查失败，先复现并定位原因，再做最小修复并重新测试。
