## Context

server 是长期事实源，agent 只返回结构化市场数据。当前 `fund`、`stock_market` 只保存当前值；现有 processing task/callback 模式已经提供任务创建、派发、回调幂等和诊断基础。根目录以下契约是本变更事实源：

- `contracts/holdlens-server/agent/market-detail-data-refresh-task-create.md`
- `contracts/holdlens-agent/market-data/market-detail-data-refresh-task-dispatch.md`
- `contracts/holdlens-server/agent/market-detail-data-refresh-callback.md`
- 三个 `contracts/holdlens-server/client/*history*` / `stock-company-profile-query.md`

## Goals / Non-Goals

**Goals:**

- 以单资产、可选 slice 的异步任务从 agent 获取详情数据。
- 幂等保存历史点、价格 bar 和最新公司资料。
- 允许部分成功，并让一个失败 slice 不回滚其他可信 slice。
- 为客户端提供按约定区间查询的稳定 API。

**Non-Goals:**

- 不实现自动定时调度、按页面访问自动刷新或客户端直接触发刷新。
- 不实现同类平均、基金排名、风险等级或股票交易状态。
- 不让 server 直接访问 AKShare。
- 不修改自选列表、统一搜索、批量加入和股票当前详情。
- 不保存 agent token、cookie、完整上游响应或个人资产事实。

## Decisions

### 1. 单资产统一刷新任务

新增 `market_detail_data_refresh` processing task，一个任务只包含一个 `assetRef`。基金允许 `nav_history`，股票允许 `price_history`、`company_profile`。Case 在创建任务前解析引用并确认 `fund` 或 `stock_market` 已存在；美股历史任务从 `stock_market.provider_market_code` 读取数据源代码并放入 dispatch，客户端不提交该字段。随后通过独立 Domain Port 派发给 agent。

任务创建只提供受控 `/api/agent/**` 入口，客户端页面查询不触发任务。暂不新增 scheduler，避免在首期同时决定全量刷新频率和成本。

### 2. 数据模型

新增三张表：

- `fund_nav_history`：`fund_code + nav_date` 唯一；保存单位净值、累计净值、日增长率、来源时间和抓取时间。
- `stock_price_bar`：`stock_code + market + granularity + bar_time` 唯一；保存 OHLC、成交量、币种、来源时间和抓取时间。查询 period 是窗口，不持久化为 bar 身份。
- `stock_company_profile`：`stock_code + market` 唯一；保存公司名、行业、业务摘要、公司简介、网站、来源时间和抓取时间。

表使用公开业务键，不把客户端 `assetRef` 当数据库外键。数值字段用 DECIMAL，bar 时间使用 DATETIME 并保存来源时区规范化后的北京时间；API 输出 ISO 8601 `+08:00`。

### 3. callback 幂等和事务边界

callback 先按 `server_task_id`、任务类型和 `idempotency_key={task}:result:1` 验证 processing task，再校验 schema、asset kind/ref 和请求 slice 一致。

每个 slice 独立校验和批量 upsert：

- 有效 slice 在独立事务中保存。
- `partial_failed` 保存所有有效 slice，并记录安全 warning/error summary。
- 全部 slice 无可信结果时不写详情表，将任务标记失败。
- 重复 callback 不产生重复历史记录，也不重复推进终态。

单次历史结果采用批量 Mapper XML upsert，避免逐条 SQL。callback 只保存规范字段，不保留原始 payload。

### 4. 查询窗口

- 基金 `1m/3m/1y/all` 根据最新数据日计算起始日期，按日期升序返回。
- 股票 `intraday/5d/1m/1y` 根据 bar granularity 和时间窗口查询；`5d` 允许 day bar。
- 公司资料返回单条最新快照。

资产存在但对应表为空时返回 HTTP 200 和空数组/空字段；公开资产本身不存在时返回业务错误。查询不触发刷新。

### 5. 分层和并行文件边界

- 新建 `marketdetail` 或等价明确边界的 Domain/Case/Infrastructure 代码，避免把历史模型塞入当前 `stock_market` Entity。
- 新建 `AgentMarketDetailDataRefreshController`、客户端历史/profile Controller 和独立 API 服务接口。
- 新建 Mapper XML 和迁移；不得编辑 `asset_info_mapper.xml`、`fund_mapper.xml`、`stock_market_mapper.xml` 或自选/搜索 Controller。

因此可与 `add-client-market-asset-apis` 并行；允许共同依赖只读的 assetRef 契约，若需要代码引用值对象则在最终集成时复用其已确认 API，不复制第二套解析规则。

## Risks / Trade-offs

- 历史 callback 可能较大：通过单资产任务、批量 SQL 和请求 slice 限制控制。
- DATETIME 丢失原始时区：入库前统一北京时间，API 明确输出 `+08:00`。
- 暂无自动调度意味着数据需受控触发后才出现；客户端已有空态，不伪造结果。
- 两个 server change 并行时 assetRef 代码尚未落地：本 change 使用契约 fixture 开发，最终集成统一引用实现。

## Migration / Rollback

先执行新增表 migration，再部署 server 和 agent，最后受控创建任务验证 callback。回滚时停止创建新任务并回退 API/代码；新增表保留以避免删除已获取公开数据。无既有表破坏性变更。

## Open Questions

当前无待确认事项。刷新采用受控手动入口，自动调度留待后续独立 change。
