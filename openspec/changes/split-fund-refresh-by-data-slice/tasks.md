## 1. 数据库与当前快照模型

- [x] 1.1 更新 `docs/dev-ops/mysql/sql/holdlens.sql` 的 `fund` 定义，加入目录字段、净值/收益字段、覆盖状态、四个 slice 抓取时间和 `last_detail_view_time`，并核对 `fund_top_holding` 仍只表示当前前十大；验证新基线可直接建表。
- [x] 1.2 新建 `docs/dev-ops/mysql/sql/migrations/20260712_fund_refresh_slices.sql`，用前向 ALTER/索引升级旧结构且不创建 `processing_task_item`；验证旧数据在迁移后保持不变。
- [x] 1.3 扩展 Fund PO、领域当前快照、API DTO 和 MyBatis result map，使业务日期、抓取时间、收益覆盖状态和最近查看时间具有独立字段；运行相关映射测试。
- [x] 1.4 增加数据库基线与前向迁移结构一致性检查，分别验证“新基线重建”和“旧基线 + migration”得到等价目标结构。

## 2. 四类任务与 Agent Port

- [x] 2.1 在任务领域模型中增加 `fund_catalog_refresh`、`fund_purchase_status_refresh`、`fund_period_return_refresh`、`fund_top_holding_refresh` 类型及兼容的批次级状态流转；增加状态单元测试。
- [x] 2.2 为四类任务分别定义 Case 命令/结果和 Domain Port 契约，固定 dispatch path、callback path、task/result schema v1；验证错误 task type/schema 无法进入写入用例。
- [x] 2.3 在 Infrastructure `adapter/port` 与 gateway DTO 中实现四组 agent dispatch 适配，保持 source 实现细节不进入 server 契约；为路径、schema、callback URL 和异常映射增加 Port 测试。
- [x] 2.4 复用通用 `processing_task` Repository 实现任务创建、派发状态、同类型非终态查询和安全参数摘要，确认重仓批次只创建 task 而不创建 task item。

## 3. Slice 专用 Repository 与 Mapper

- [x] 3.1 实现目录专用 upsert Repository/DAO/Mapper XML，只插入或更新代码、名称、类型、拼音和 `catalog_fetched_at`；用测试证明缺失行不删除且其他 slice 字段不变。
- [x] 3.2 实现申购状态专用 update Repository/DAO/Mapper XML，只更新已知基金的状态、限额和 `purchase_status_fetched_at`；用测试证明未知代码 warning+skip 且不插入。
- [x] 3.3 实现阶段收益专用 update Repository/DAO/Mapper XML，处理 covered/source_not_covered、当前净值和阶段收益；用测试证明同日同内容业务 no-op、未知/无效记录保留旧值。
- [x] 3.4 实现重仓元数据与 `fund_top_holding` 的原子同步，覆盖新报告期、同期 no-op、同期源修正、较旧报告期、普通空列表、明确无公开股票持仓和未知代码场景；用测试证明只有带有效报告期的显式 `no_public_stock_holdings` 才能清仓，异常空列表绝不清仓。
- [x] 3.5 移除四类新回调对现有统一 `fund.upsert` 的依赖，并增加交叉字段保护测试，分别证明任一 slice 写入不改变另外三个 slice。

## 4. 调度与重仓目标选择

- [x] 4.1 在 Domain Repository 中定义“当前持有 + 有效关注/资产主数据 + 近 90 天查看”的基金目标查询，并在 Infrastructure Mapper XML 中实现去重查询；覆盖三类纳入和仅目录基金排除测试。
- [x] 4.2 实现目录、申购状态、阶段收益三个独立调度 Case，每次只创建一个全市场任务，并在本轮启动前相同 task type 有非终态任务时跳过；覆盖独立性和跳过测试。
- [x] 4.3 实现重仓调度 Case，按可配置 batch size 切分去重代码且默认每批 20，并允许在本轮先创建全部批次；覆盖空目标、刚好 20、超过 20、本轮批次互不阻塞和批次失败不回滚前置批次测试。
- [x] 4.4 将 Trigger Job 改为四个轻量入口，默认全部关闭，按 `Asia/Shanghai` 自然日配置每日 cron 和每月 1/15 cron；验证不读取 Repository、不调用 Domain Port、不依赖交易日历。
- [x] 4.5 增加配置绑定与启动测试，验证四个 enabled/cron 独立，重仓 batch size 默认 20，非法配置会安全跳过并记录日志。

## 5. 四类 Callback 事务与幂等

- [x] 5.1 实现目录 callback 用例，在单事务内完成幂等记录、非空结果校验、目录 upsert、warning 和 succeeded/partial_failed/failed 终态；覆盖整体空结果零写入测试。
- [x] 5.2 实现申购状态 callback 用例，在单事务内保存有效已知基金并对未知/无效行 warning+skip；覆盖部分成功与全失败测试。
- [x] 5.3 实现阶段收益 callback 用例，保存当前快照和覆盖状态但不创建历史序列；覆盖 source_not_covered 不计失败、同内容 no-op 和整体空结果保旧测试。
- [x] 5.4 实现重仓 callback 用例，对批内每只基金应用报告期/内容比较并只提交有效结果；覆盖部分失败保旧、明确无公开股票持仓清空、完全 inflight 重叠成功 no-op、前十限制和增减字段测试。
- [x] 5.5 调整四个新 callback 的事务失败路径：任一步异常时回滚幂等、slice、warning 和终态并返回非 2xx，任务保持原非终态；证明没有沿用旧复合回调立即独立写 `callback_failed` 的路径。
- [x] 5.6 增加 callback 幂等测试，验证相同 idempotency key 的已成功 callback 只返回既有结果且不重复写数据或日志。
- [x] 5.7 限定 agent callback body 的 `status` 只能为 succeeded/partial_failed/failed，并增加拒绝 callback_failed 和错误 schema/task type 的 Controller/Case 测试。

## 6. Callback 超时与详情异步回源

- [x] 6.1 实现四类新任务的 callback 超时扫描，默认超时 30 分钟，并校验配置覆盖预期最长任务执行时间与 agent 立即/10/60 秒完整重试窗口；验证超时后仅由 server 本地写 `callback_failed` 并解除调度阻塞。
- [x] 6.2 增加超时边界测试，证明重试窗口内不提前终结、已终态任务不被改写、晚到重复 callback 不产生重复 slice 写入。
- [x] 6.3 在基金详情 Case 中先读取并返回数据库快照，再更新全局 `last_detail_view_time`；验证详情响应不等待 agent 且不新增用户访问明细。
- [x] 6.4 当重仓缺失或 `top_holding_fetched_at` 超过默认 15 天阈值时 best-effort 派发单基金重仓任务，并在 DTO 中表达 missing/refreshing/stale；验证派发失败不影响详情响应。
- [x] 6.5 调整详情新鲜度判断只使用 slice 抓取时间，并验证页面始终携带 `top_holdings_as_of`，不会把公开报告期描述为实时持仓。

## 7. 回归测试与质量门

- [x] 7.1 为四类 Case、四类 Port、四类 Repository、四类 Callback Controller 和四个 Job 补齐单元/集成测试，覆盖 specs 中全部 WHEN/THEN 场景并记录对应关系。
- [x] 7.2 串行运行受影响 Maven 模块测试和 app 聚合测试（JDK 17），确认旧 A 股/美股刷新、资产持仓和关注基金行为无回归。
- [x] 7.3 运行 SQL 基线/迁移验证，确认不存在 `processing_task_item`，已有基金及重仓数据保留，四个 slice 字段不会交叉覆盖。
- [x] 7.4 运行 `openspec validate --strict split-fund-refresh-by-data-slice`，并在实现完成后同步勾选实际已验证的任务。

## 8. 灰度启用与旧链路退役

- [ ] 8.1 在 agent 四个 endpoint 已发布后部署 server 新链路并保持新 Job disabled，执行前向 migration，验证未产生外部调用。
- [ ] 8.2 关闭旧 `fund_detail_refresh` 复合 Job，再依次启用目录、申购状态、阶段收益和重仓调度；每一步核对任务终态、warning、返回数量和 slice 数据。
- [x] 8.3 首次完整目录导入后移除旧 Job 的全表扫描入口，确保配置和回滚手册均不能重新启用“扫描全部 fund 并逐基金复合刷新”。
- [ ] 8.4 稳定观察后退役旧 `fund_detail_refresh` endpoint、DTO、Case/Port 和统一写入路径；运行完整回归并确认单 slice 可通过独立开关停用。
