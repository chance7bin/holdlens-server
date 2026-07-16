## 1. 契约与数据库模型

- [x] 1.1 新增脱敏的 dispatch/callback v1 跨项目契约文档，固定 schema、路径、状态、字段、幂等键、错误与原子覆盖语义；核对 server DTO 与示例一致。
- [x] 1.2 更新 `holdlens.sql` 并新增前向 migration，为 `fund` 增加资产配置报告期/状态/抓取时间，创建带 `DECIMAL(12,4)`、排序、时间和 `(fund_code, asset_type, asset_type_name)` 唯一键的 `fund_asset_allocation` 当前快照表；验证不改动现有重仓结构。
- [x] 1.3 扩展 Domain 当前快照、Fund PO/Mapper result map，并新增 Domain Repository 接口、DAO/PO 和 Mapper XML；验证详情查询按基金批量聚合资产配置且不存在 N+1。

## 2. 独立任务、Port 与调度

- [x] 2.1 增加 `fund_asset_allocation_refresh` task type、task/result schema 和 callback path，扩展通用基金切片状态/超时查询；用状态与 schema 测试验证独立任务识别。
- [x] 2.2 实现 agent Port 的 `/tasks/fund-asset-allocation-refresh` 路由和 payload，确保只发送去重 `fund_codes` 而不发送报告期；补充路径、schema、callback URL、派发结果和数据安全测试。
- [x] 2.3 实现基于持有/关注/近 90 天查看并集的资产配置候选查询，覆盖 missing、新已结束季度和 unavailable 七天退避；用 Repository/Mapper 测试验证仅目录基金被排除。
- [x] 2.4 实现独立调度 Case 与轻量 Job，使用每周 cron、默认 batch size 20、独立开关和同类型非终态跳过；补充空目标、批次切分、开关和基本 scheduler 测试。
- [x] 2.5 扩展 `IAgentRefreshScheduleService` 和 `AgentRefreshScheduleController`，新增资产配置调度 HTTP 手动入口并复用 Job；补充响应、路由和调用次数测试。

## 3. Callback 与原子快照写入

- [x] 3.1 扩展 callback request/command 和 Controller 独立入口，接收报告期、`available/unavailable/missing`、资产配置明细和 warning，并严格校验 callback header、schema、task type、状态与 `<server_task_id>:result:1`。
- [x] 3.2 在 Case 层实现规范化、报告期比较和覆盖决策，覆盖新报告期、同期 no-op、同期修正、旧报告期、空/非法明细、未知基金、missing 和 unavailable 保留历史规则。
- [x] 3.3 在 Infrastructure Repository 中实现“更新 fund 元数据 + 删除旧明细 + 批量插入新明细”的事务原子替换及首次 unavailable 标记；验证任一步失败回滚并且其他 slice 字段不变。
- [x] 3.4 补充 callback 幂等、任务 succeeded/partial_failed/failed、重复投递、外部 warning 脱敏和任务参数不包含凭据/报告期/个人金额的测试。

## 4. 基金详情 API

- [x] 4.1 扩展 Case Result、API DTO 和 Controller 映射，独立返回 `assetAllocationAsOf`、`assetAllocationStatus`、`assetAllocationFetchedAt` 与排序后的 `assetAllocations`；验证 missing/unavailable 空态和已有 top holdings 兼容。

## 5. 集成验证

- [x] 5.1 串行运行 JDK 17 下受影响模块测试和 app 聚合构建，确认基金目录/收益/重仓、详情和其他刷新任务无回归。
- [x] 5.2 检查数据库基线与 migration 字段/索引一致性，确认现有基金与 `fund_top_holding` 不被删除或改义。
- [x] 5.3 运行 `openspec validate --strict add-fund-asset-allocation-refresh`，复核敏感数据影响并将 tasks.md 复选框同步为真实完成状态。
