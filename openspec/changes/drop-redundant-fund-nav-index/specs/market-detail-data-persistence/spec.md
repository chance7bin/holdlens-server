## MODIFIED Requirements

### Requirement: 独立保存合法 slice

系统 SHALL 对基金净值、股票价格和公司资料分别校验并批量 upsert。部分失败时，合法 slice SHALL 被保存，失败 slice MUST NOT 产生伪造或部分脏数据。

系统 SHALL 接受最多 10000 点的完整基金净值历史，使超过 5000 个交易日记录的长期基金不因存续时间而被拒绝。基金净值历史 SHALL 仅使用唯一索引 `(fund_code, nav_date)` 保证幂等写入并支持当前按基金和日期查询，不得为该相同列组合维护额外普通索引。

#### Scenario: 基金净值幂等 upsert

- **WHEN** callback 包含同一基金和日期的有效净值点
- **THEN** 系统 SHALL 按 `fund_code + nav_date` 创建或更新单条记录
- **AND** 数据库 SHALL 保留对应的唯一索引

#### Scenario: 基金净值索引不重复

- **WHEN** 初始化新数据库或执行索引优化迁移
- **THEN** `fund_nav_history` MUST NOT 同时维护唯一索引和普通索引，且两者列顺序均为 `(fund_code, nav_date)`

#### Scenario: 股票 bar 幂等 upsert

- **WHEN** callback 包含同一股票、市场、粒度和时间的有效 bar
- **THEN** 系统 SHALL 创建或更新单条 bar
- **AND** 系统 SHALL 保留合法数值 0

#### Scenario: 部分成功

- **WHEN** 一个股票任务的价格历史成功而公司资料失败
- **THEN** 系统 SHALL 保存有效价格 bar
- **AND** 系统 SHALL 把 processing task 终态记录为部分失败语义
- **AND** 系统 MUST NOT 写入伪造公司资料

#### Scenario: slice 持久化失败诊断

- **WHEN** 任一合法 slice 在独立事务中保存失败
- **THEN** 系统 SHALL 记录 task id、slice、脱敏异常类型和安全错误摘要
- **AND** 日志 MUST NOT 包含 callback 鉴权值、凭据或完整原始 payload
