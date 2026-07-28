## 1. OpenSpec

- [x] 1.1 创建基金重仓股票注册的 proposal、design、spec 和 tasks。
- [x] 1.2 验证 OpenSpec change 严格校验通过。

## 2. 股票标的注册能力

- [x] 2.1 在股票行情 Domain Repository 增加注册股票标的接口。
- [x] 2.2 在 Infrastructure Repository/DAO/MyBatis XML 实现注册股票标的，确保不覆盖行情字段。
- [x] 2.3 增加或更新 Repository 测试，覆盖注册不覆盖行情字段的映射行为。

## 3. 基金回调编排

- [x] 3.1 在基金刷新回调成功或部分成功路径中提取重仓股票并注册到股票表。
- [x] 3.2 保持重复回调和终态任务幂等，不重复注册股票。
- [x] 3.3 增加或更新 Case 测试，覆盖有效股票注册、无效股票跳过、去重和重复回调。

## 4. 质量门

- [x] 4.1 运行相关 Maven 测试。
- [x] 4.2 做产品、工程、QA、发布、安全五个视角轻量评审。
