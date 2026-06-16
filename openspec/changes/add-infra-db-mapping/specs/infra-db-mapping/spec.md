## ADDED Requirements

### Requirement: PO field mapping
系统 SHALL 为 `holdlens.sql` 中的每张业务表提供一个 infrastructure PO，并且 PO 字段 SHALL 覆盖对应表的全部列。

#### Scenario: 表字段完整映射
- **WHEN** 开发者查看任一 `holdlens.sql` 业务表对应的 PO
- **THEN** PO 包含该表所有列对应的驼峰命名字段，并使用与 MySQL 类型兼容的 Java 类型

### Requirement: DAO select contracts
系统 SHALL 为每张业务表提供一个 MyBatis DAO 接口，用于声明基础 select 查询方法。

#### Scenario: DAO 接口可被 MyBatis 扫描
- **WHEN** Spring Boot 加载 infrastructure DAO
- **THEN** 每个 DAO 接口都有 `@Mapper` 标记，并且查询方法只声明参数与返回类型

### Requirement: Mapper XML SQL implementation
系统 SHALL 在 Mapper XML 中实现 DAO 的 select SQL，不得在 DAO 接口中使用 SQL 注解实现查询。

#### Scenario: 查询 SQL 位于 XML
- **WHEN** 开发者查看任一新增 DAO 查询方法
- **THEN** 对应 SQL 实现在 `holdlens-server-app/src/main/resources/mybatis/mapper` 下的 Mapper XML 中

### Requirement: DDD infrastructure package boundaries
系统 SHALL 将 PO、DAO 和 Mapper XML 放在项目约定的基础设施边界内，不得新增 `persistent` 包。

#### Scenario: 基础设施包路径合规
- **WHEN** 开发者查看新增 Java 文件路径
- **THEN** PO 位于 `infrastructure/dao/po`，DAO 位于 `infrastructure/dao`，且没有新增 `persistent` 包
