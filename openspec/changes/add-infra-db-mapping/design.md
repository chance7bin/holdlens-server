## Context

当前项目已有 Maven 多模块与 MyBatis 资源目录，但 `holdlens-server-infrastructure` 只有包占位文件，`holdlens-server-app/src/main/resources/mybatis/mapper/frame_case_mapper.xml` 仍是脚手架示例，且示例 namespace 指向禁止使用的 `persistent` 包。

`docs/dev-ops/mysql/sql/holdlens.sql` 是本次变更的数据模型输入，包含 8 张表：
- `asset_account`
- `asset_info`
- `asset_holding`
- `asset_holding_change`
- `fund_detail_snapshot`
- `fund_detail_item`
- `fund_top_holding`
- `agent_warning`

本次实现不改变 DDL，也不读取或依赖 mock 数据脚本。当前行为是没有可用的表字段映射和查询 DAO；本次变更只建立 infra 层基础读模型。

## Goals / Non-Goals

**Goals:**
- 为 `holdlens.sql` 中每张表创建一个 PO，字段覆盖 DDL 列并使用合适 Java 类型。
- 为每张表创建一个 DAO 接口，使用 `@Mapper` 和必要的 `@Param`，仅声明简单 select 查询。
- 为每个 DAO 创建 Mapper XML，包含 resultMap、基础列清单、主键查询和基于索引的简单查询。
- 保持 DDD/六边形边界：PO/DAO 属于 Infrastructure，Mapper XML 属于 App 资源。

**Non-Goals:**
- 不创建 Domain Entity、Repository 接口或 Repository 实现。
- 不创建 Controller、Case、API DTO、定时任务或业务服务。
- 不实现 insert、update、delete、事务逻辑、缓存逻辑或业务校验。
- 不修改 `holdlens.sql`、数据库部署脚本或运行配置。

## Decisions

1. PO 使用 `*PO` 命名并放在 `com.echoamoy.holdlens.server.infrastructure.dao.po`
   - 理由：符合项目 DDD/六边形 infra 约定，PO 是数据库表字段映射对象，不承载业务行为。
   - 备选：直接创建领域 Entity。放弃原因是当前需求限定 infra 层，领域模型需要后续结合业务语义设计。

2. DAO 使用 `I*Dao` 命名并放在 `com.echoamoy.holdlens.server.infrastructure.dao`
   - 理由：保持 MyBatis Mapper 接口职责单一，只声明数据库查询方法。
   - 备选：使用注解 SQL。放弃原因是项目规则要求 MyBatis SQL 默认使用 Mapper XML。

3. Mapper XML 以表名命名，例如 `asset_account_mapper.xml`
   - 理由：与表一一对应，后续排查 SQL 更直接。
   - 备选：按业务域聚合成一个 XML。放弃原因是当前只是底层表映射，按表拆分更简单。

4. 简单 select 查询范围采用“主键 + 现有索引字段”
   - 理由：主键查询提供通用单条读取，索引查询覆盖后续 Repository 最可能需要的基础列表查询，同时避免无索引全表扫描。
   - 备选：为所有字段生成条件查询。放弃原因是超出“简单查询”范围，也容易提前固化未确认的业务查询模型。

## Risks / Trade-offs

- 字段语义未来可能被领域模型重新命名 → PO 保持数据库字段驼峰映射，业务命名留给后续 Domain/Repository 转换。
- `TEXT` JSON 字段当前按 `String` 映射 → 简单、无额外依赖；后续如需结构化解析，应在 Repository 或 Domain 转换层处理。
- decimal 字段使用 `BigDecimal` → 保持金额和比例精度，但调用方需要自行处理空值。
- 只提供 select 查询 → 满足当前“先实现映射和简单查询”的目标，写入和事务一致性留给后续明确业务流程的 change。
