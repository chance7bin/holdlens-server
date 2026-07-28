## 1. 批量边界测试

- [x] 1.1 补充基金目录 Case 测试，使用 1001 条有效记录验证 Repository 批次严格为 500、500、1，并保持成功终态。
- [x] 1.2 补充 Repository 与 Mapper SQL 结构测试，验证领域对象批量转换和多 VALUES upsert 字段覆盖语义。

## 2. 分层批量写入实现

- [x] 2.1 在 `IFundDataRepository` 增加批量目录 upsert 语义，并在 `FundDataRepository` 转换列表后调用 DAO。
- [x] 2.2 在 `IFundDao` 声明批量方法，并在 `fund_mapper.xml` 使用 MyBatis `foreach` 实现单条多 VALUES upsert。
- [x] 2.3 在 `FundSliceRefreshCaseImpl` 校验目录记录后按固定每批最多 500 条调用 Repository，保留原事务、诊断和状态语义。

## 3. 验证

- [x] 3.1 使用 JDK 17 串行运行相关 Case、Infrastructure 测试和 server Maven 聚合测试。
- [x] 3.2 运行 `openspec validate --strict optimize-fund-catalog-callback` 并检查 tasks 状态与实际一致。
- [x] 3.3 检查安全与兼容性：不记录 callback 凭据或个人资产，不修改外部契约、表结构和同步确认语义。

## 4. Callback 可观测性

- [x] 4.1 在基金目录 callback Case 事务边界增加接收、批次执行、提交汇总和失败诊断日志，确保成功日志只在事务返回后输出。
- [x] 4.2 补充批次失败传播回归测试，确认失败批次停止后续写入并继续向上抛出异常；运行相关测试和 OpenSpec 严格校验。
