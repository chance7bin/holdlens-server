# HoldLens Server 文档导航

本文件只说明文档和运行资料的位置，不要求新功能必须先创建 PRD，也不替代 `AGENTS.md` 中的 OpenSpec、授权或实现规则。

## 目录与文件

- `requirements/`：用户按需维护的产品需求、版本总览和模板。
- `decisions/`：领域边界、事实源、持久化、权限、审计和集成方式等长期决策。
- `notes/`：调研、讨论记录和临时分析，不作为当前实现状态的事实源。
- `agent-runbook.md`：本项目环境、构建、测试和运行问题的已验证解法。
- `to-do-list.md`：尚未进入正式产品规划的轻量想法收集箱。
- `dev-ops/`：本地与部署环境、Docker、应用启停和数据库资料。
- `script/`：开发或数据验证辅助脚本。

## 数据库资料

`dev-ops/mysql/sql/holdlens.sql` 是新环境建库基线，`dev-ops/mysql/sql/migrations/` 保存已有环境的前向迁移。它们是可执行且受项目测试约束的数据库事实源，不是仅供阅读的示例 SQL。

阅读时从与当前任务最相关的入口开始，不需要预先加载整个 `docs/`。
