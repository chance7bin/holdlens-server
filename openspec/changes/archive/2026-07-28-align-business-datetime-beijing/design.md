## Context

当前 server 的时间链路存在三类来源：

- 数据库自动时间：多数表使用 `DATETIME DEFAULT CURRENT_TIMESTAMP` 和 `ON UPDATE CURRENT_TIMESTAMP`。
- Java 写入时间：PO/Entity 大量使用 `java.util.Date` 映射 `DATETIME`，通过 MyBatis/JDBC 写入。
- 跨系统回调时间：agent 回调中 `generated_at`、`quote_time` 等字段使用 ISO-8601 字符串。

当前环境又存在不一致：

- MySQL 和应用容器配置倾向北京时间，例如 Dockerfile `TZ=PRC`、MySQL compose `TZ=Asia/Shanghai`。
- JDBC URL 当前使用 `serverTimezone=UTC`。
- server 接收 `quote_time` 时走 `Instant.parse(value)`，`2026-06-26T16:08:09+08:00` 会变成同一瞬时时间的 `2026-06-26T08:08:09Z`，最终写入 `DATETIME` 时表现为 `2026-06-26 08:08:09`。

本 change 的业务目标是：业务库 `DATETIME` 字段在数据库中直接以北京时间可读值保存，例如行情时间 `2026-06-26T16:08:09+08:00` 入库为 `2026-06-26 16:08:09`。

## Goals / Non-Goals

**Goals:**

- 建立统一规则：MySQL `DATETIME` 等同于 `Asia/Shanghai` 本地日期时间。
- 使 Java 写入、MySQL 自动时间和查询展示在北京时间语义下保持一致。
- 明确 `quote_time` 的推荐解析方式，避免 `Instant`/`Date` 隐式 UTC 化。
- 为历史 UTC 脏数据提供可执行的评估和迁移决策。

**Non-Goals:**

- 不重新设计所有时间字段命名。
- 不引入多时区用户偏好展示。
- 不把所有 API 响应格式一次性改成带 offset 字符串。
- 不在 DAO 或 Mapper XML 中写业务时区判断。

## Decisions

### 业务 `DATETIME` 语义

server SHALL treat every business MySQL `DATETIME` column as `Asia/Shanghai` local date-time. 该值不表示 UTC instant，不携带 offset，也不做用户时区转换。

这意味着：

- 数据库中看到的 `2026-06-26 16:08:09` 就是北京时间 `2026-06-26 16:08:09`。
- Java 业务层需要表达该值时，优先使用 `LocalDateTime`。
- 只有跨系统输入输出需要表达绝对时间或 offset 时，才使用 `OffsetDateTime`、`ZonedDateTime` 或 `Instant`。

### JDBC 和运行时配置

JDBC 连接应与业务库时区语义保持一致，建议将 `serverTimezone=UTC` 改为 `serverTimezone=Asia/Shanghai`，或使用等价配置保证 MySQL Connector/J 按北京时间处理 `DATETIME`。

应用容器、MySQL 容器和 JVM 默认时区应保持 `Asia/Shanghai`，避免 `CURRENT_TIMESTAMP`、`NOW()`、日志和默认 `Date` 行为产生混淆。

### `quote_time` 接收方式

`quote_time` 是外部行情事实时间，agent 回调契约应继续传 offset-aware ISO-8601 字符串，例如：

```json
{
  "quote_time": "2026-06-26T16:08:09+08:00"
}
```

server 接收时应按以下语义处理：

1. 使用 `OffsetDateTime.parse(value)` 解析输入，要求输入包含 offset，例如 `+08:00` 或 `Z`。
2. 使用 `atZoneSameInstant(ZoneId.of("Asia/Shanghai"))` 转为北京时间。
3. 使用 `toLocalDateTime()` 得到数据库 `DATETIME` 要保存的本地时间。

示意代码：

```java
DateTimeUtils.parseOffsetDateTimeOrNull(value);
```

公共工具方法内部应保持以下核心转换：

```java
public static LocalDateTime toBusinessLocalDateTime(String value) {
    return OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.of("Asia/Shanghai"))
            .toLocalDateTime();
}
```

容错解析方法应在空值或解析失败时按调用场景返回 `null` 或当前业务时间，例如：

```java
public static LocalDateTime parseOffsetDateTimeOrNull(String value) {
    if (isBlank(value)) {
        return null;
    }
    try {
        return toBusinessLocalDateTime(value);
    } catch (DateTimeParseException e) {
        return null;
    }
}
```

不建议继续使用：

```java
Date.from(Instant.parse(value))
```

因为它会丢弃输入 offset 的本地展示语义，只保留 UTC instant。

### Java 类型收敛策略

实现时应优先收敛被触及的 `DATETIME` 映射为 `LocalDateTime`：

- `stock_market_current.quote_time`：建议作为本 change 的首要落点，从 API command 到 Domain Entity、PO、Mapper 写入链路均收敛为 `LocalDateTime`。
- `create_time`、`update_time`：可逐步从 PO/Entity 的 `Date` 收敛为 `LocalDateTime`，但需要避免一次性大范围重构影响无关功能。
- `generated_at`：跨系统输入仍按 offset/instant 语义解析；当前不写入基金当前详情业务表，如后续落入业务库 `DATETIME`，必须按同一北京时间规则本地化保存。

本 change 暂不统一 API 响应时间格式，不强制把 DTO 时间字段改成北京时间字符串。实现仍需避免因为存储修正导致现有 API 响应意外回退；后续如需统一响应为带 `+08:00` 的字符串，应单独开 change 处理展示契约。

### 分层边界

按照 DDD/六边形约定：

- Trigger/API 层只接收原始字符串并映射到 Command，不承载时间业务判断。
- Case 层负责跨系统输入归一化：将 agent 的 offset-aware 时间转换成业务本地时间。
- Domain 层持有业务时间语义，例如 `StockQuoteEntity.quoteTime` 表示北京时间本地行情时间。
- Infrastructure 层只负责 `LocalDateTime` 与 MySQL `DATETIME` 的读写映射，不在 DAO/Mapper XML 中进行时区业务判断。
- App 层负责 JDBC、JVM、Jackson 等技术配置的统一。

### 历史数据策略

实现前必须确认是否存在已经按 UTC 写入的业务时间。若存在，至少需要评估：

- `stock_market_current.quote_time` 中是否有 `08:xx` 实际应为 `16:xx` 的历史行情时间。
- `processing_task`、`processing_log`、`processing_callback` 等表的创建/更新时间是否混用 UTC 和北京时间。
- 是否只迁移明确由 Java/JDBC 写入且能判断为 UTC 的字段，避免把本来由 MySQL `CURRENT_TIMESTAMP` 生成的北京时间再错误加 8 小时。

已确认迁移策略：

- 本 change 不迁移历史 `stock_market_current.quote_time` 数据。
- 当前环境通过数据库重刷获得新的北京时间行情时间。
- 实现阶段仍需停止继续写入 UTC 样式的业务 `DATETIME`，避免重刷后再次污染。

## Risks / Trade-offs

- [历史数据留存] 本 change 不迁移历史 `stock_market_current.quote_time`，重刷前旧数据可能仍显示 UTC 样式时间。
- [API 返回格式变化] 如果 DTO 从 `Date` 改为 `LocalDateTime` 或字符串，前端展示可能受影响，需要测试。
- [跨系统时间语义混淆] `generated_at` 这种跨系统生成时间天然适合 offset/instant；实现中必须明确它当前只作为回调元信息，不作为基金当前详情业务表字段。
- [改动面扩大] 全库 `DATETIME` 收敛可能触及多个模块。可以按“配置 + quote_time 首修 + 被触及字段类型收敛”的方式推进，避免无关重构。

## Resolved Decisions

- 不迁移历史 `stock_market_current.quote_time` 数据；直接通过数据库重刷获得新数据。
- API 响应时间暂不统一为北京时间字符串；后续单独处理 API 时间格式。
- `generated_at` 保持回调元信息语义；当前不落入基金当前详情业务表。
