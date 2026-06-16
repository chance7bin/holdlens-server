## 1. Infrastructure PO

- [x] 1.1 为 `asset_account`、`asset_info`、`asset_holding`、`asset_holding_change` 创建 PO，并核对字段覆盖 DDL
- [x] 1.2 为 `fund_detail_snapshot`、`fund_detail_item`、`fund_top_holding`、`agent_warning` 创建 PO，并核对字段覆盖 DDL

## 2. MyBatis DAO

- [x] 2.1 为 8 张表创建 DAO 接口，声明主键查询和基于现有索引字段的简单查询
- [x] 2.2 确认 DAO 只使用 `@Mapper` / `@Param` 等绑定注解，不使用 SQL 注解

## 3. Mapper XML

- [x] 3.1 为 8 张表创建 Mapper XML，配置 namespace、resultMap 和基础列清单
- [x] 3.2 在 Mapper XML 中实现所有 DAO select 查询 SQL

## 4. Verification

- [x] 4.1 运行 Maven 编译，确认新增 Java 与 XML 资源不破坏构建
- [x] 4.2 运行 `openspec validate --strict add-infra-db-mapping`
