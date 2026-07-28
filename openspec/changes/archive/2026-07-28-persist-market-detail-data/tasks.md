## 1. 契约与数据库

- [x] 1.1 对照根目录 task/dispatch/callback 和三个客户端查询契约建立 DTO fixture 与字段映射测试。
- [x] 1.2 新增 `fund_nav_history`、`stock_price_bar`、`stock_company_profile` 基线 DDL 和独立 migration，包含唯一键、查询索引和回滚说明。

## 2. Domain 与 Infrastructure

- [x] 2.1 新增市场详情历史/profile 领域实体、Repository 和 agent dispatch Port，保持 Domain 无框架依赖。
- [x] 2.2 新增 PO、DAO、Mapper XML 和 Repository 适配器，完成批量幂等 upsert 与时间窗口查询。
- [x] 2.3 实现 agent HTTP 适配器和安全日志，复用现有 token、timeout、代理和错误脱敏约定。

## 3. 任务创建与 callback

- [x] 3.1 实现受控任务创建用例和 `/api/agent/market-detail-data-refresh/tasks`，覆盖公开资产存在性和 slice/period 校验。
- [x] 3.2 实现派发请求模型，确保一个任务一个 assetRef、schema 和 callback URL 正确。
- [x] 3.3 实现 callback 用例和 `/internal/agent/market-detail-data-refresh/callback`，覆盖任务身份、幂等、部分成功、失败和重复回调。
- [x] 3.4 为每个 slice 建立独立校验/事务边界并批量保存合法结果，更新 processing task 终态和脱敏诊断。

## 4. 客户端查询 API

- [x] 4.1 实现基金净值历史查询 Case/API/Controller，覆盖四个 period、升序和空结果。
- [x] 4.2 实现股票价格历史查询 Case/API/Controller，覆盖四个 period、minute/day 粒度和空结果。
- [x] 4.3 实现公司资料查询 Case/API/Controller，覆盖部分字段和未采集空对象。
- [x] 4.4 增加 Trigger/API 测试，校验响应 null/0、时间格式和不存在资产错误。

## 5. 验证与审查

- [x] 5.1 运行 migration/Mapper、Case、callback、Trigger 相关测试和 Maven 编译。
- [x] 5.2 检查批量 SQL、幂等键、事务边界、用户数据隔离和凭据/原始响应不落库。
- [x] 5.3 运行 `openspec validate --strict persist-market-detail-data`。
- [x] 5.4 根据真实实现和验证结果同步任务状态，不提前勾选。

## 6. 联调缺陷修复

- [x] 6.1 为市场详情查询 Controller 显式声明参数名，并增加标准编译产物回归测试。
- [x] 6.2 兼容未请求集合 slice 的空数组，保持请求后可信空结果语义，并覆盖 callback 测试。
- [x] 6.3 为每个 slice 的持久化异常增加脱敏诊断日志。
- [x] 6.4 修正 Spring Boot 可执行 JAR 主类配置并验证 `java -jar` 可启动。
- [x] 6.5 重新执行真实 server → agent → callback → 查询联调，核验 processing 状态与详情表落库。
- [x] 6.6 将基金完整净值历史上限调整为 10000 点，并覆盖超过 5000 点的长期基金回归场景。
