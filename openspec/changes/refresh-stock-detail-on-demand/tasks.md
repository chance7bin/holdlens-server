## 1. 契约与持久化结构

- [x] 1.1 更新根 contracts，定义股票详情 ensure/status 及 dispatch exchange_code 的请求、状态和兼容语义
- [x] 1.2 增加 processing_task 活动键/租约与股票详情 slice 状态迁移，并同步基线 SQL 与 SQL 结构测试

## 2. 领域与基础设施能力

- [x] 2.1 扩展处理任务实体、Repository Port、DAO/PO/Mapper，支持唯一活动任务、租约回收和非终态条件更新
- [x] 2.2 增加股票详情 slice 状态实体与 Repository/DAO/XML 实现，验证 active task 条件写入
- [x] 2.3 扩展 Agent dispatch Port/DTO，向 A 股任务传递业务 exchange_code

## 3. 用例与 HTTP 入口

- [x] 3.1 实现股票详情 ensure claim、事实/可信空判断、single-flight 和租约恢复
- [x] 3.2 实现 callback 股票 slice 独立收敛及迟到任务保护，并修复派发响应覆盖终态的竞态
- [x] 3.3 新增 POST ensure 与 GET task status API，保持现有 GET 查询无副作用

## 4. 验证

- [x] 4.1 扩展 Case/Infrastructure/Controller 测试，覆盖并发复用、状态映射、租约、迟到 callback 和 exchange code
- [x] 4.2 使用 JDK 17 串行运行相关 Maven 测试与聚合构建，并执行 `openspec validate --strict refresh-stock-detail-on-demand`
