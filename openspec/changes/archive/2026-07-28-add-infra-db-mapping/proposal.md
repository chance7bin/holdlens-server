## Why

`docs/dev-ops/mysql/sql/holdlens.sql` 已定义 HoldLens 资产、持仓、基金详情快照和处理警告等核心表，但 infra 层尚未提供与这些表一致的持久化对象和 MyBatis 查询入口。
先补齐数据库字段到 Spring Boot infra 实体的映射，可以为后续 Repository、领域服务和用例编排提供稳定的数据访问基础。

## What Changes

- 基于 `holdlens.sql` 中的 8 张业务表创建 infrastructure PO，对齐字段名、Java 类型和数据库注释语义。
- 为每张表创建 MyBatis DAO 接口，仅声明简单 select 查询方法，不在 DAO 注解中实现 SQL。
- 在 `holdlens-server-app/src/main/resources/mybatis/mapper` 下新增 Mapper XML，提供 resultMap、基础列清单和简单 select SQL。
- 范围仅限 infra 层数据库映射，不新增 Repository、Domain、Case、Trigger、API 或业务流程。
- 不改变数据库 DDL、不引入新依赖、不修改服务启动端口或运行环境。

## Capabilities

### New Capabilities

- `infra-db-mapping`: 覆盖 HoldLens MySQL 表到 infrastructure PO、DAO 与 MyBatis Mapper XML 的基础映射和简单查询能力。

### Modified Capabilities

- 无。

## Impact

- 影响模块：
  - `holdlens-server-infrastructure`: 新增 `dao/po` 持久化对象和 `dao` 查询接口。
  - `holdlens-server-app`: 新增 MyBatis Mapper XML。
- 用户可见影响：无直接用户界面或 API 行为变化。
- 成功标准：
  - PO 字段覆盖 `holdlens.sql` 中对应表字段。
  - DAO 查询方法均由 Mapper XML 提供 SQL。
  - Maven 编译通过。
  - `openspec validate --strict add-infra-db-mapping` 通过。
