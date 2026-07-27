# HoldLens Server 产品需求文档

本目录只维护 `holdlens-server` 的 PRD、版本规划和产品进度。文档必须在本项目独立检出、且会话目录只有本项目时仍然可读，不依赖上级仓库的需求文档或 Agent 规范。

PRD 由用户按规划需要自行创建，或由用户明确要求 Agent 创建或修改。未经明确要求，Agent 不得自动生成 PRD。没有 PRD 不妨碍范围和成功标准已经清楚的研发需求进入本项目 OpenSpec。

## 文档边界

| 文档 | 负责内容 | 不负责内容 |
| --- | --- | --- |
| PRD | Server 产品目标、业务范围、用户流程、业务规则、产品验收标准和进度 | Change 名称、接口字段、技术设计和任务 |
| `version-overview.md` | 版本目标、PRD 清单、状态、PRD 与本项目 OpenSpec Change 的映射 | 详细需求和技术设计 |
| OpenSpec | 本项目单次研发变更的行为增量、详细场景、设计、任务和完成状态 | 完整复制 PRD 或 ADR |
| ADR | 领域边界、事实源、持久化、权限、审计和集成方式等长期决策 | 产品计划、详细行为和任务进度 |

PRD 不包含“关联 OpenSpec Change”字段，也不设置“OpenSpec 衔接”章节。PRD 与 OpenSpec Change 的关系只在同版本的 `version-overview.md` 中维护。

## 创建与修改授权

- 创建 PRD 必须由用户主动完成，或由用户明确要求 Agent 创建。
- Agent 可以建议进行产品规划，但不得直接把建议写成 PRD。
- 改变 PRD 的目标、范围、优先级、业务规则或验收标准，必须由用户明确确认。
- 用户授权实现已纳入版本规划的 PRD 时，可以机械同步 PRD 状态和版本总览映射。
- 没有 PRD 且需求仍模糊时先在对话中澄清；清楚后可以直接创建 OpenSpec Change。

## 目录结构

```text
docs/requirements/
  README.md
  prd-template.md
  version-overview-template.md

  v0.0.1/
    version-overview.md
    shared/
      prd-shared-xxx.md
```

一个 PRD 只属于一个主版本。后续版本扩展历史需求时创建增量 PRD，不直接重写已经完成版本的范围。

## 版本总览与 Change 映射

`version-overview.md` 是本项目 PRD 与本项目 OpenSpec Change 映射的唯一事实源：

- PRD 和 OpenSpec 文档不重复维护对方名称或路径。
- 一个 PRD 可以对应多个本地 Change，一个本地 Change 也可以覆盖多个 PRD。
- 没有 PRD 的技术性 Change 可以独立存在，不写入版本总览。
- Change 尚未创建时填写 `-`。
- Change 使用本项目名称，并优先链接到 `openspec/changes/<change-name>/proposal.md`。

## PRD 状态

| 状态 | 含义 |
| --- | --- |
| 待确认 | 产品范围、业务规则或验收标准仍待用户确认 |
| 待实现 | 产品范围已经确认，尚未开始实现 |
| 实现中 | 已开始交付，但尚未覆盖全部范围和必要验证 |
| 已实现 | PRD 范围已经完成，并通过约定的验证或产品验收 |
| 废弃 | 用户决定不再推进该需求 |

OpenSpec Change 是否归档由本项目 OpenSpec 流程管理，不作为 PRD 状态。

## 工作流

1. 用户自行创建 PRD，或明确要求 Agent 创建。
2. 用户确认产品范围后，将 PRD 状态更新为 `待实现`。
3. 非琐碎研发变更按本项目 `AGENTS.md` 创建或更新 OpenSpec Change。
4. 在本版本总览中建立 PRD 与本地 Change 的映射；PRD 本身不记录 Change。
5. 获得实现授权并开始交付后，将 PRD 和版本总览状态同步为 `实现中`。
6. PRD 范围完成并通过验证后同步为 `已实现`；Change 按 OpenSpec 规则独立归档。
