## Context

Server 的股票行情事实以业务市场标识股票身份，基金重仓却把供应商编码保存进同名字段。兼容映射如果放在详情查询路径，会让错误数据长期存在并把供应商知识扩散到 Server 业务逻辑。

## Goals / Non-Goals

**Goals:**

- 只保存规范业务市场到 `fund_top_holding.market`。
- 独立保存供应商市场码用于诊断，不向 Client 暴露。
- 对错误 Agent 输入安全降级，保留持仓事实和 callback 可用性。
- 通过一次性迁移修正存量数据，不保留运行时兼容分支。

**Non-Goals:**

- 不在 Server 维护东方财富市场码映射表。
- 不新增港股业务市场或行情抓取链路。
- 不在本变更实现行情快照过期判断。

## Decisions

### 1. Agent 映射，Server 软校验

Case 仅接受 `A_SHARE`、`US_STOCK` 和 null。其他 `market` 归一为 null，并写入 `unsupported_holding_market` processing warning；持仓行继续参与快照保存，callback 不因此失败，原本成功的任务也不降级为 `partial_failed`。

### 2. 原始码作为持仓来源元数据持久化

在基金聚合和 `fund_top_holding` 增加 `provider_market_code`。它不参与行情关联和 Client DTO，仅用于来源追踪与未来市场扩展。同期快照内容比较包含该字段，允许来源修正落库。

### 3. 历史数据只通过 migration 修复

migration 先把现有非业务枚举 `market` 复制到 `provider_market_code`，再映射已支持编码；无法映射的业务市场置空。运行时代码不把 `0/1/105/106/107` 转换为业务市场。

## Risks / Trade-offs

- [Agent 回归输出原始码] → Server 保留持仓并记录 warning，但当日行情暂时缺失，避免错误关联。
- [迁移未执行就发布 Server] → 新 Mapper 会读取新列，因此数据库 migration 必须先于 Server 部署。
- [未知市场无法展示行情] → 保留原始码，待业务明确支持该市场后单独扩展。

## Migration Plan

1. 发布支持双字段的 Agent。
2. 在数据库执行前向 migration，新增字段并修正历史市场值。
3. 发布 Server 代码。
4. 回归基金详情；若需回滚 Server，新增列可保留，不影响旧代码。

迁移脚本只作为交付物创建，本变更不自动操作运行中的数据库。
