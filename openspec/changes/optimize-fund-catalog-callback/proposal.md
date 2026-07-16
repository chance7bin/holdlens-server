## Why

基金目录全量 callback 当前在一个事务内逐条执行单行 upsert，大量数据库往返使处理时间超过 agent 的等待窗口，并在重试后放大为重复事务和幂等锁竞争。当前运行已出现 callback 超时及 server 事务失败，需要在不削弱“落库完成后才确认 callback”的前提下缩短写入耗时。

## What Changes

- 将基金目录 callback 的有效记录按固定每批最多 500 条交给 Repository 批量 upsert。
- 在 Domain Repository 接口、Infrastructure Repository、MyBatis DAO 和 Mapper XML 中补齐批量目录写入链路。
- 保持 callback 校验、单事务原子性、幂等键、任务终态和异常时非 2xx 响应语义不变。
- 增加基金目录 callback 的接收、批次执行、事务提交汇总和失败诊断日志，不记录逐条基金或敏感 callback 内容。
- 为 1001 条目录数据补充分批边界测试，并覆盖批量 SQL 结构和 Repository 映射。
- 不修改数据库表结构、外部 API、callback payload 或跨项目契约。

## Capabilities

### New Capabilities

- `fund-catalog-batch-persistence`: 规定 server 在单次基金目录 callback 事务内按最多 500 条批量写入，并在全部写入完成后确认 callback。

### Modified Capabilities

- 无。

## Impact

- 影响 `holdlens-server-case` 的基金目录 callback 编排。
- 影响基金目录 callback 的运行时可观测性和相关失败传播测试。
- 影响 `holdlens-server-domain` 的基金数据 Repository 接口。
- 影响 `holdlens-server-infrastructure` 的 Repository/DAO 与 `holdlens-server-app` 的 MyBatis Mapper XML。
- 影响相关 Case、Repository 和 SQL 结构测试；不新增数据库迁移。
