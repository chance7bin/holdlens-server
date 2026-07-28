## 1. 差量同步测试

- [x] 1.1 扩展 `FundDataRepositoryTest` 的 Fake DAO，使其可返回现有重仓并记录 UPDATE、INSERT、DELETE 操作；通过测试夹具验证现有 ID 可追踪。
- [x] 1.2 新增“已有排名更新且不插入、新增排名插入、失效排名删除”的单元测试，并验证最终操作集合与 `fund_code + rank_no` 一致。
- [x] 1.3 新增空集合清空和重复排名最后一条生效测试，验证不保留空槽且每个排名最多写入一次。

## 2. DAO 与 MyBatis SQL

- [x] 2.1 调整 `IFundTopHoldingDao`，增加按已有记录 ID 更新和批量删除失效 ID 的方法；编译验证 DAO 参数绑定完整。
- [x] 2.2 调整 `fund_top_holding_mapper.xml`，将 INSERT 收敛为只插入新增排名，并实现明确 UPDATE 与批量 DELETE SQL；检查仍保留 `fund_code + rank_no` 唯一键且无 DDL 变更。

## 3. Repository 差量同步

- [x] 3.1 在 `FundDataRepository` 中按 `rank_no` 归并本次输入，保持重复排名最后一条生效，并读取该基金现有重仓。
- [x] 3.2 将已有排名路由到 UPDATE、新增排名路由到 INSERT、失效排名路由到 DELETE；空集合继续清空该基金全部当前重仓。
- [x] 3.3 保持 `fundDao.upsert`、重仓差量同步、warning 写入位于现有事务内，并运行现有回调保存失败路径测试，确认异常继续触发事务回滚与 `callback_failed` 状态记录。

## 4. 质量门与一致性检查

- [x] 4.1 使用 JDK 17 运行 `FundDataRepositoryTest`，确认差量更新、插入、删除、空集合和重复排名场景全部通过。
- [x] 4.2 使用 JDK 17 运行受影响模块及 app 聚合测试，并校验 MyBatis Mapper XML 与 DAO 绑定，确认现有基金回调、查询行为无回归。
- [x] 4.3 复核 HTTP 回调契约、数据库 DDL、权限和数据暴露面均未变化，并确认 `tasks.md` 状态与实际实现一致。
- [x] 4.4 运行 `openspec validate --strict sync-fund-top-holdings-incrementally` 并修复全部校验问题。
