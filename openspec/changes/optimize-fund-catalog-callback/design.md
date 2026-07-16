## Context

`FundSliceRefreshCaseImpl` 在一个事务内处理 callback 幂等记录、基金切片写入、日志和任务终态。基金目录路径当前对每条有效基金调用一次 `IFundDataRepository.upsertCatalog()`，Infrastructure 再执行一次单行 MyBatis upsert。全量目录因此产生大量数据库往返，处理超过 agent 的 10 秒等待窗口后会触发重复 callback；重复请求还可能等待首个事务持有的幂等键或基金行锁。

现有 A 股全量行情已采用 Case 层固定每 500 条调用一次 Repository 批量 upsert 的模式。本次复用该模式，同时保留基金目录 callback 的单事务一致性和“全部处理完成后才返回 2xx”语义。

## Goals / Non-Goals

**Goals:**

- 将基金目录有效记录按每批最多 500 条执行一条批量 upsert SQL。
- 保持单条校验、warning、有效数量和任务状态判断不变。
- 保持完整 callback 在一个事务内原子提交，任一批失败时整体回滚并返回非 2xx。
- 遵循 `Case -> Domain Repository -> Infrastructure Repository -> DAO/XML` 分层。

**Non-Goals:**

- 不把 callback 改成先确认后异步处理。
- 不修改 callback schema、幂等键、鉴权或重试协议。
- 不调整申购状态、阶段收益或重仓写入路径。
- 不新增表、索引、迁移脚本或外部依赖。

## Decisions

### 1. Case 层固定每批 500 条

`FundSliceRefreshCaseImpl` 负责把已通过基金代码、名称校验的目录领域对象累计到批次，满 500 条即调用 `IFundDataRepository.upsertCatalogs()`，尾批按实际数量调用。固定值与现有股票全量 upsert 一致，避免为内部 SQL 大小增加暂时不需要的运行配置。

目录记录的业务有效性仍在 Case 层判断；Infrastructure 只接收已确认可写的领域对象，不承载业务校验。

### 2. Domain 暴露批量目录语义，Infrastructure 转换为 PO

`IFundDataRepository` 新增批量目录 upsert 方法；`FundDataRepository` 将领域对象列表转换为 `FundPO` 列表，并调用 `IFundDao.upsertCatalogBatch()`。Case 不直接依赖 DAO 或 MyBatis 类型，保持六边形依赖方向。

保留现有单条方法以兼容测试替身和已有调用；默认批量方法可以委托单条方法，但生产 Repository 必须覆盖为真正的批量 DAO 调用。

### 3. MyBatis 使用多 VALUES 的原子 upsert

Mapper XML 使用 `INSERT INTO fund (...) VALUES (...), (...) ON DUPLICATE KEY UPDATE ...`，字段覆盖语义与当前单行 SQL 完全一致。单批最多 500 条，控制 SQL 参数数量与 packet 大小。

备选方案是 MyBatis executor batch 重复执行单条 statement。该方案仍产生多次语句执行且事务行为更依赖 executor 配置，不如单条多 VALUES SQL 直接，因此不采用。

### 4. 跨批仍保持一个 callback 事务

分批只降低 SQL 往返次数，不拆分事务。幂等记录、所有目录批次、日志和任务终态仍由现有 `TransactionExecutor.required()` 包裹；任一批抛错时全部回滚，Controller 返回 500，agent 可以安全重试相同幂等结果。

### 5. 在 Case 事务边界记录有界诊断

基金目录 callback 在进入事务前记录 `serverTaskId` 和输入条数；每批 Repository 调用在 Case 层以 `DEBUG` 记录批次序号、批次大小和 SQL 调用耗时。`TransactionExecutor.required()` 成功返回后，再以 `INFO` 记录输入、有效、跳过、批次数、去重状态、最终状态和总耗时，避免把尚未提交的数据误报为成功。

批次或事务失败时只记录任务标识、批次序号、大小、耗时和异常类型，并继续抛出原异常，由现有全局异常处理器统一记录堆栈和返回非 2xx。日志不得包含基金逐条数据、callback payload、鉴权 header、凭据或个人资产信息。

## Risks / Trade-offs

- [单条批量 SQL 仍可能触及 MySQL packet 限制] -> 固定上限 500，目录字段均为短文本；测试验证 Mapper 使用受控批次入口。
- [1001 条数据产生多个 DAO 调用] -> 预期为 500、500、1 三批，仍处于同一事务并由边界测试固定。
- [失败仍会回滚全量目录] -> 这是现有可靠确认语义；本次以降低往返为目标，不引入部分提交。
- [工作区已有其他未提交改动] -> 仅修改本 change 所需文件，保留手动调度和异常日志等既有改动。
- [批次 DEBUG 日志在默认 INFO 级别不可见] -> 正常运行只保留接收与提交汇总；排查性能问题时临时开启对应 Case logger 的 DEBUG。

## Migration Plan

1. 发布 server 批量写入实现并重新运行应用。
2. 通过基金目录手动调度验证 callback 在 agent timeout 内返回 2xx、任务进入成功或部分成功终态。
3. 随后发布或重启 agent，使默认 callback timeout 从 10 秒变为 60 秒。
4. 回滚时恢复旧 server 版本即可；SQL schema 和持久化数据无需回滚。

## Open Questions

当前无待确认事项。批次固定 500 条、同步确认和单事务原子性均已由用户选择的方案确定。
