## ADDED Requirements

### Requirement: 业务 DATETIME 使用北京时间语义
server SHALL store and read MySQL `DATETIME` business columns as `Asia/Shanghai` local date-time values.

#### Scenario: 数据库中直接展示北京时间
- **WHEN** server writes a business `DATETIME` value that represents `2026-06-26T16:08:09+08:00`
- **THEN** the database `DATETIME` column SHALL store `2026-06-26 16:08:09`
- **AND** the value SHALL NOT be normalized to `2026-06-26 08:08:09` only because the same instant is `08:08:09Z`

#### Scenario: Java 写入时间与 MySQL 自动时间一致
- **WHEN** one row contains a Java-written business `DATETIME` and a MySQL-generated `create_time` or `update_time`
- **THEN** both values SHALL follow the same `Asia/Shanghai` local time convention
- **AND** server SHALL NOT mix UTC-style and Beijing-style `DATETIME` values in the same business database

### Requirement: 股票行情时间按输入 offset 转北京时间入库
server SHALL parse agent stock quote `quote_time` as an offset-aware ISO-8601 value and persist it as Beijing local `DATETIME`.

#### Scenario: 保存 agent 传入的北京时间行情时间
- **WHEN** server receives a valid `stock-quote-refresh-result/v1` callback containing `quote_time = "2026-06-26T16:08:09+08:00"`
- **THEN** server SHALL persist `stock_market_current.quote_time` as `2026-06-26 16:08:09`
- **AND** server SHALL preserve the quote's stock code, market, stock name, trade date, and daily return as before

#### Scenario: 保存非北京时间 offset 的行情时间
- **WHEN** server receives a valid stock quote callback containing `quote_time` with a non-Beijing offset
- **THEN** server SHALL convert that instant to `Asia/Shanghai` local date-time before persisting
- **AND** the database value SHALL reflect the Beijing local clock time for that instant

#### Scenario: 缺失或非法行情时间
- **WHEN** server receives a stock quote callback whose `quote_time` is blank or cannot be parsed as an offset-aware ISO-8601 value
- **THEN** server SHALL preserve the existing null-or-skip behavior defined by the stock quote refresh flow
- **AND** server SHALL NOT write an incorrect fallback UTC time into `stock_market_current.quote_time`

### Requirement: Agent generated_at 保留跨系统时间语义
server SHALL keep agent callback `generated_at` as offset-aware cross-system metadata and SHALL NOT require persisting it to current fund detail business tables.

#### Scenario: 处理 agent 生成时间
- **WHEN** server receives an agent callback containing `generated_at` as an offset-aware ISO-8601 value
- **THEN** server SHALL parse the input as an instant with offset when needed for callback processing
- **AND** server SHALL NOT require `fund_detail_item` to contain a `generated_at` column
- **AND** server SHALL preserve the existing callback schema and validation behavior

### Requirement: API 时间响应格式暂不变更
server SHALL NOT require a global API response time format change as part of this timezone storage change.

#### Scenario: 查询接口返回时间字段
- **WHEN** server returns API DTOs containing time fields after this change
- **THEN** server SHALL preserve existing API response compatibility unless a touched endpoint explicitly needs a minimal adjustment
- **AND** server SHALL NOT require all time fields to be returned as `+08:00` strings in this change

### Requirement: 持久化边界避免隐式 UTC 换算
server SHALL avoid implicit timezone conversion when mapping business `DATETIME` columns between Java and MySQL.

#### Scenario: 使用本地日期时间写入 DATETIME
- **WHEN** server maps a business `DATETIME` column in the persistence path touched by this change
- **THEN** server SHOULD use `LocalDateTime` or an equivalent timezone-free local date-time representation
- **AND** server SHALL NOT rely on `Instant`/`Date` conversion that changes the local clock value before writing MySQL `DATETIME`

#### Scenario: JDBC 时区配置与业务语义一致
- **WHEN** server connects to MySQL for HoldLens business data
- **THEN** the JDBC timezone behavior SHALL be aligned with `Asia/Shanghai`
- **AND** server SHALL NOT configure the business datasource in a way that stores Beijing business `DATETIME` values as UTC local clock values

### Requirement: 历史 quote_time 不做迁移
server SHALL not require historical `stock_market_current.quote_time` data migration in this change.

#### Scenario: 保留历史行情时间
- **WHEN** existing `stock_market_current.quote_time` records were written before this change
- **THEN** this change SHALL NOT require a migration that updates those historical values
- **AND** operators MAY refresh the stock quote data to obtain new Beijing-local quote times

#### Scenario: 防止继续写入 UTC 样式时间
- **WHEN** stock quote data is refreshed after this change
- **THEN** newly persisted `quote_time` values SHALL follow Beijing local `DATETIME` semantics
- **AND** server SHALL NOT continue writing UTC-normalized local clock values that require later migration
