## Context

当前基金详情刷新回调会保存 `fund_top_holding`，其中 `market` 允许为空；但注册股票行情目标时，Case 层只提取 `stock_code` 和 `market` 都非空的重仓股票。`stock_market_current.market` 当前也是 `NOT NULL`，并以 `stock_code + market` 唯一。

这导致 agent 能识别股票代码但暂时无法识别市场时，基金重仓可以保存，股票库却不会留下待补全记录。后续排查时只能从基金重仓表反推，股票刷新 universe 也无法表达“存在股票，但 market 待补全”这一状态。

关联 PRD `docs/requirements/v0.0.1/shared/prd-shared-fund-detail-refresh-task.md` 仍保持已实现状态；本变更是已有基金详情刷新链路的持久化规则修正，不新增独立 PRD。

## Goals / Non-Goals

**Goals:**

- 允许基金详情回调中的非空 `stock_code` 进入 `stock_market_current`，即使 `market` 为 `null` 或空白。
- 保持 `stock_market_current` 注册操作幂等：同一只 `stock_code` 的空 market 记录不应重复插入多行。
- 股票行情刷新任务允许下发 `market = null` 的股票目标，由 agent 根据 `stock_code` 尝试识别或刷新行情。
- 保持注册股票目标不覆盖已有行情字段。

**Non-Goals:**

- 不在 server 中推断 `market`。
- 不在 server 中强制补齐或推断 `market`。
- 不清理或合并历史已存在的异常重复数据；如生产库已有真实数据，需另开迁移任务。

## Decisions

### 允许空 market 注册股票目标

Case 层 `toQuoteTargets` 的过滤规则调整为：

1. `stock_code` 必须非空，并做 trim。
2. `market` 可为空；空白字符串归一化为 `null`。
3. `stock_name` 为空时继续允许注册，并按最新输入更新为 `null`。
4. 同一回调内按 `stock_code + normalized_market` 去重，其中空 market 使用内部空值标识参与去重。

这条规则仍属于用例编排，因为基金回调保存基金数据后，需要协调股票数据仓储注册一个刷新 universe 候选。

### 空 market 记录进入股票行情刷新任务

`queryAllQuoteTargets` 和对应 Mapper 查询应返回 `stock_code` 非空的记录，`market` 可以为 `null`。Infrastructure port 下发股票行情刷新请求时继续包含 `market` 字段；当数据库值为空时，请求中的 `market` 为 `null`。

这会扩大 agent 股票行情刷新契约：agent 需要接受 `market: null` 的股票目标，并可根据 `stock_code` 自行判断市场或返回安全 warning。股票行情回调保存成功 quote 时，仍以回调中的 `stock_code + market` 作为行情身份；如果 agent 仍无法识别市场并返回 `market = null`，server 允许保存到空 market 记录。

组合查询按 `stock_code + market` 关联股票行情。基金重仓缺 market 且行情也缺 market 时，可通过同样的空 market 键关联到行情；若后续 agent 补出非空 market，则会形成一条非空 market 行情记录，空 market 记录仍作为原始候选保留，除非后续另开迁移/合并规则。

### 数据库唯一性使用归一化 market 键

仅把 `stock_market_current.market` 改成可空不足以保证幂等，因为 MySQL 唯一索引允许多条 `NULL` 参与的组合键记录。实现时应通过数据库层的归一化唯一键保证：

- `market` 字段本身允许 `NULL`。
- 增加内部归一化键，例如由 `COALESCE(market, '')` 得到的 generated column，或等价的 MyBatis/DDL 方案。
- 唯一约束以 `stock_code + normalized_market` 为准。

该内部键只服务持久化幂等，不进入 Domain Entity、API DTO、agent 契约或前端响应。

### 注册操作更新名称但不覆盖行情字段

`registerQuoteTargets` 继续表示“注册股票目标”，不是“刷新行情”。无论 `market` 是否为空：

- 新记录写入 `stock_code`、`market`、`stock_name`。
- 已有记录按本次入参更新 `stock_name`；当入参为空时，当前股票简称也更新为 `null`，表达最新目标注册结果中名称未知。
- 不更新 `trade_date`、`daily_return`、`quote_time`。

股票行情字段仍只由 `stock_quote_refresh` 回调写入。

Mapper XML 中的 `ON DUPLICATE KEY UPDATE` 使用 MyBatis 参数形式引用本次入参，例如 `stock_name = #{stockName}`，不使用 `VALUES(column)` 写法。唯一键冲突仍由数据库约束判断，业务层不做“先查再插/更新”的分支。

### 分层边界

- Case 层负责从基金回调聚合中提取股票目标并调用股票数据仓储。
- Domain 层的 `IStockMarketRepository` 语义不需要新增接口；已有 `registerQuoteTargets` 可表达本变更。
- Infrastructure 层在 `adapter/repository` 中转换 PO，调用 `dao`；SQL 继续放在 `holdlens-server-app/src/main/resources/mybatis/mapper`。
- 不新增 `persistent` 包，不在 DAO 注解里写 SQL。

## Risks / Trade-offs

- [agent 无法处理空 market] -> server 仍按用户要求下发 `market: null`；agent 失败时应通过股票刷新 warning 或部分失败表达，server 不在本变更中推断 market。
- [MySQL NULL 唯一性导致重复行] -> 使用归一化唯一键或等价方案保障 `stock_code + null market` 幂等。
- [同一 stock_code 可能同时存在空 market 和非空 market 记录] -> 当前接受这种过渡状态；空 market 表示原始候选或 agent 未能识别市场的行情，非空 market 表示已知市场行情。
- [初始化 SQL 与真实库迁移不一致] -> 当前项目约定先维护初始化 SQL；如已有线上数据，需要单独设计在线迁移和重复数据清理。

## Migration Plan

1. 调整初始化 SQL 中 `stock_market_current.market` 为空约束和唯一性定义。
2. 调整 Mapper XML 中注册目标的 upsert 逻辑，确保空 market 可插入且幂等。
3. 调整 Case 层过滤与去重规则。
4. 调整股票刷新目标查询，让 `market = null` 的记录进入 agent 股票刷新请求。
5. 增加 Case/Repository 测试后运行相关 Maven 测试与 `openspec validate --strict allow-null-market-stock-targets`。

## Open Questions

当前无待确认事项。实现阶段默认不做 market 推断、不自动合并空 market 与已知 market 记录。
