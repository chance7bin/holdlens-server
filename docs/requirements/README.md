# 需求文档

本目录用于维护产品需求文档（PRD），包括功能需求、业务规则、用户故事、验收标准、依赖风险和未决问题。

PRD 是 OpenSpec 变更的输入材料，不替代 OpenSpec 的 proposal、design、spec 或 tasks。需求评审清楚后，再基于对应 PRD 创建或更新 OpenSpec change。

PRD 只描述产品意图、业务范围、用户流程、业务规则和验收标准。接口、任务、数据模型、迁移、兼容性、技术风险等研发影响评估，应在 OpenSpec proposal 或 design 中完成。

创建 PRD 时，优先从根目录的 [prd-template.md](prd-template.md) 复制模板，并根据目标版本、模块、涉及端和需求复杂度补全内容。模板用于统一常见信息结构，不要求所有 PRD 机械保留每个小节；简单模块、低风险需求或不涉及某类内容时，可以酌情删减、合并或调整小节，但应保留背景、目标、范围、需求明细、验收标准、依赖风险、未决问题和 OpenSpec 衔接等关键信息。

## 目录结构

新需求按迭代版本归档。版本目录优先，版本内再按涉及端存放 PRD；版本总览使用“模块”做索引。

```text
docs/requirements/
  README.md
  prd-template.md
  version-overview-template.md

  v0.0.1/
    version-overview.md
    admin/
      prd-admin-xxx.md
    user/
      prd-user-xxx.md
    agent/
      prd-agent-xxx.md
    shared/
      prd-shared-xxx.md
```

说明：

- `prd-template.md` 是所有 PRD 的统一模板，保留在根目录。
- `version-overview-template.md` 是版本总览的统一模板，保留在根目录。
- `vX.Y.Z/` 表示一个迭代版本，推荐使用三段语义版本号，例如 `v0.0.1`、`v0.1.0`、`v1.0.0`。
- `admin/` 放管理端 PRD。
- `user/` 放用户端 PRD。
- `agent/` 放 AI 分析、自动化处理或智能助理相关 PRD。
- `shared/` 只放确实跨多个端、且不适合归入单一入口的 PRD。
- 根目录只保留总览文档、模板和版本目录；PRD 文件应放入对应版本目录下。

## 版本号规则

版本目录使用三段语义版本号：

```text
v主版本.次版本.修订版本
```

建议含义：

- `v0.0.1`：第一个需求迭代包。
- `v0.0.2`：第二个小迭代。
- `v0.1.0`：一组较完整的业务能力成型。
- `v1.0.0`：第一个稳定业务版本。

版本目录名只放版本号，不带主题。版本目标、范围和主题说明写在对应版本目录的 `version-overview.md` 中。

## 版本总览

每个版本目录必须包含 `version-overview.md`，用于说明该版本的目标、范围、PRD 清单和 OpenSpec 关联。

创建版本总览时，优先从根目录的 [version-overview-template.md](version-overview-template.md) 复制模板，并根据版本复杂度补全内容。模板用于统一常见信息结构，不要求所有版本机械保留每个小节；简单版本、低风险迭代或不涉及某类内容时，可以酌情删减、合并或调整小节，但应保留迭代目标、范围内 PRD、OpenSpec 关联等关键信息。

版本总览的“依赖与风险”只记录产品或业务层面的依赖与风险，例如运营规则、外部流程、业务前置条件、评审未决项。技术影响面不在版本总览中展开，由对应 OpenSpec change 承接。

`关联 OpenSpec Change` 表示 PRD 与 OpenSpec change 的追踪关系，不要求一对一。一个 PRD 可以因实现拆分关联多个 OpenSpec change；一个 OpenSpec change 也可以同时覆盖多个 PRD。多个 change 名称可用逗号分隔，尚未创建时填写 `-`。

## 文件命名

PRD 文件名使用小写英文、数字和连字符。

```text
prd-<涉及端>-<需求主题>.md
```

示例：

- `prd-user-import-broker-statement.md`
- `prd-user-portfolio-overview.md`
- `prd-agent-risk-summary.md`
- `prd-admin-data-source-management.md`
- `prd-shared-asset-class-taxonomy.md`

如果后续版本扩展历史需求，不直接重写旧 PRD，创建新的增量 PRD：

```text
v0.0.2/user/prd-user-portfolio-risk-alert.md
```

## 模块口径

版本总览中的“模块”用于从业务能力视角索引需求，建议使用稳定的业务模块名称。

常用模块：

- 账号与权限
- 数据导入
- 资产账户
- 持仓汇总
- 估值与收益
- 风险分析
- AI 分析
- 报告导出
- 基础配置

模块不是目录强制约束。PRD 的物理路径按涉及端存放，版本总览按模块索引。

## 状态枚举

PRD 状态建议使用以下值：

| 状态 | 含义 |
| --- | --- |
| 待确认 | 需求仍在整理或评审中，范围和验收标准尚未最终确认 |
| 待实现 | 尚未开始实现，或尚未核对到可确认的实现内容 |
| 实现中 | 已经完成部分实现，但尚未覆盖 PRD 的全部范围、验收标准或必要验证；该状态也表示“部分实现” |
| 已实现 | PRD 范围内功能已完成实现，并通过约定的测试、验收或 OpenSpec 校验；是否归档由 OpenSpec change 状态体现 |
| 已归档 | 所属 OpenSpec change 已完成并归档 |
| 废弃 | 需求不再继续推进 |

## 维护规则

1. 新 PRD 先进入目标版本目录，不直接放到根目录。
2. 一个 PRD 只属于一个主版本；后续版本扩展时创建新的增量 PRD。
3. 已经进入 OpenSpec 的 PRD，需要在版本总览中标注对应 change 名称。
4. 已完成版本不频繁改动原始 PRD，只允许补充决策记录、勘误和链接；新的行为变化放到新版本。
5. `prd-template.md` 和 `version-overview-template.md` 保留在根目录，所有版本共用。
6. 根 `README.md` 维护目录规则、命名规范、状态枚举和版本索引规则。
7. `version-overview.md` 是该版本 PRD 的入口；新增、废弃或完成 PRD 时同步更新清单。

## 从 PRD 到 OpenSpec

建议流程：

1. 在目标版本目录创建 PRD。
2. 使用 `prd-template.md` 补全背景、目标、范围、业务规则和验收标准。
3. 评审确认后，将 PRD 状态更新为 `待实现`。
4. 基于 PRD 创建或更新 OpenSpec change。
5. 在 OpenSpec proposal 或 design 中评估接口、数据模型、任务、兼容性和迁移影响。
6. 在版本总览中填写 `关联 OpenSpec Change`。
7. OpenSpec 实现、验证、归档完成后，同步更新 PRD 状态。
