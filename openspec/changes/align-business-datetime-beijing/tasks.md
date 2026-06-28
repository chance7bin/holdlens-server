## 1. 时间语义确认

- [x] 1.1 确认本 change 不需要读取 `docs/requirements/**/prd-*.md`，需求来源为本次对 `stock_market_current.quote_time` 入库偏移问题的排查。
- [x] 1.2 确认业务库所有 MySQL `DATETIME` 字段统一采用 `Asia/Shanghai` 本地时间语义。
- [x] 1.3 确认 `DATE` 字段不受本 change 影响。
- [x] 1.4 已确认：`generated_at` 跨系统输入保留 offset/instant 语义；当前不保存到基金当前详情业务表。

## 2. 配置与运行时

- [x] 2.1 调整 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 的 MySQL JDBC 时区配置，使其与北京时间业务语义一致。
- [x] 2.2 检查应用容器、MySQL compose 和 JVM 时区配置，确保 `CURRENT_TIMESTAMP`、`NOW()` 和 Java 默认时区不冲突。
- [x] 2.3 更新运维或数据库初始化文档，说明业务库 `DATETIME` 存储北京时间本地值。

## 3. quote_time 接收与入库

- [x] 3.1 将股票行情回调中的 `quote_time` 解析从 `Instant`/`Date` 路径调整为 offset-aware 解析，并转换为 `Asia/Shanghai LocalDateTime`。
- [x] 3.2 优先将 `stock_market_current.quote_time` 从 API command、Domain Entity、PO 到 MyBatis 写入链路收敛为 `LocalDateTime` 或等价本地日期时间类型。
- [x] 3.3 保持缺失或非法 `quote_time` 的既有容错语义，不写入错误 fallback 时间。
- [x] 3.4 补充测试：`2026-06-26T16:08:09+08:00` 入库后保持 `2026-06-26 16:08:09`。
- [x] 3.5 补充测试：非 `+08:00` offset 输入会先换算到北京时间再入库。

## 4. 全库 DATETIME 影响评估

- [x] 4.1 盘点 `holdlens.sql` 中所有 `DATETIME` 字段及其 Java PO/Entity/DTO 映射类型。
- [x] 4.2 识别所有通过 `new Date()`、`Instant.parse`、`Date.from` 或 JDBC 默认转换写入业务 `DATETIME` 的路径。
- [x] 4.3 对被本 change 触及的路径优先收敛为 `LocalDateTime`；不做无关大范围重构时，记录剩余风险和后续任务。
- [x] 4.4 检查 API 响应序列化是否会把北京时间业务值展示成 UTC；本 change 暂不统一 API 响应为北京时间字符串，只防止被触及接口意外回退。

## 5. 历史数据策略

- [x] 5.1 已确认：不迁移历史 `stock_market_current.quote_time` 数据，当前环境直接通过数据库重刷获得新行情时间。
- [x] 5.2 实现完成后提示运维/使用方通过股票行情重刷修正历史展示数据。
- [x] 5.3 不对由 MySQL `CURRENT_TIMESTAMP`/`NOW()` 已按北京时间生成的字段做盲目 `+8 小时`。

## 6. 质量门

- [x] 6.1 运行相关 Maven 测试，至少覆盖 `AgentFundRefreshCaseImplTest` 和股票行情仓储/Mapper 相关测试。
- [x] 6.2 如修改 app 配置或 MyBatis 映射，运行受影响模块测试或 app 聚合测试。
- [x] 6.3 运行 `openspec validate --strict align-business-datetime-beijing`。
- [x] 6.4 从产品、工程、QA、发布、安全五个视角做轻量评审，重点检查时间展示、历史数据、回滚和隐私影响。
