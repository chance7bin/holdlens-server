# 设计

## Context

当前 `BookkeepingCategoryEnumVO` 同时承担类别定义、类型校验、名称解析和排序。数据库只有 `bookkeeping_entry.category_code`，没有类别目录或用户配置。该结构适合固定类别，但无法表达系统类别的用户覆盖、自定义类别、图标选择和拖拽排序。

本项目保持 `Trigger → API → Case → Domain ← Infrastructure` 的依赖方向。类别是 bookkeeping 限界上下文内的长期业务事实，由 Server 持久化和校验；客户端只消费类别和图标契约。

## Goals / Non-Goals

**Goals:**

- 统一表达系统类别与用户自定义类别，同时隔离类别定义和用户启停/排序配置。
- 保持现有稳定类别代码和账目表兼容。
- 支持支出、收入分别启用、停用、创建和排序。
- 保证停用类别与关联活动账目软删除的事务一致性。
- 使明细、统计和编辑链路识别自定义名称与图标。

**Non-Goals:**

- 不提供类别改名、永久删除、跨收支类型移动或旧账目恢复。
- 不提供图标上传、SVG 内容存储或用户自定义图片。
- 不联动资产目录、资产记录、持仓、余额或审计系统。
- 不改变当前显式 `userId` 模式；账号体系接入后再改为鉴权上下文。

## Decisions

### 1. 统一类别定义，独立用户配置

新增 `bookkeeping_category`：

```text
id, code, scope, ownerUserId, entryType, name, iconKey,
defaultEnabled, defaultSortOrder, createRequestId,
createTime, updateTime
```

- `scope` 为 `SYSTEM/USER`，避免与 `entryType=EXPENSE/INCOME` 混淆。
- SYSTEM 类别 `ownerUserId/createRequestId` 为空，保存默认启用状态和顺序。
- USER 类别必须有所有者和创建幂等键，服务端生成 `CUS_` 前缀的全局不透明稳定代码。
- 名称与图标是类别定义，不因用户停用而删除。

新增 `bookkeeping_user_category_config`：

```text
userId, categoryId, status, sortOrder, createTime, updateTime
```

- 主键为 `(user_id, category_id)`，状态为 `ENABLED/DISABLED`。
- SYSTEM 类别没有配置行时使用定义上的默认值；用户修改后以配置覆盖默认值。
- USER 类别创建时同时写入一条 ENABLED 配置。
- 查询通过定义与配置合并，不在 GET 中懒初始化或写入默认数据。

### 2. 保留账目类别代码，不建立跨来源外键

`bookkeeping_entry.category_code VARCHAR(50)` 原样保留。创建和修订由 Category Repository 按 `userId + categoryCode` 解析可用类别并校验类型、所有权和 ENABLED 状态。

账目表不直接外键到类别 ID：系统类别对所有用户可见，自定义类别只对所有者可见，而用户配置允许缺省回退，强制外键会迫使查询初始化或复制系统类别。领域校验与数据库唯一键共同保证一致性。

### 3. 内置类别默认目录与兼容迁移

迁移把现有 23 个代码写入统一定义表。默认启用：

```text
EXPENSE: FOOD, SHOPPING, DAILY, TRANSPORT, ENTERTAINMENT,
         COMMUNICATION, CLOTHING, HOUSING, MEDICAL, OTHER_EXPENSE
INCOME:  SALARY, BONUS, PART_TIME, INVESTMENT_INCOME, OTHER_INCOME
```

默认停用：

```text
EXPENSE: VEGETABLE, FRUIT, SNACK, SPORT, BEAUTY, HOME
INCOME:  BUSINESS, REIMBURSEMENT
```

迁移扫描现有活动账目；如果用户已经使用默认停用类别，则写入 ENABLED 覆盖配置，避免升级过程静默隐藏或删除既有账目。迁移本身绝不改变 `bookkeeping_entry.status`。

### 4. 图标目录由领域白名单定义

服务端维护 68 个合法 `iconKey` 以及十个分组和组内顺序：餐饮美食、交通出行、居家生活、购物装扮、健康运动、娱乐休闲、学习教育、人情社交、收入财务、通用其他。

图标查询只返回 key 和分组元数据；SVG 文件继续随 Client 发布。类别创建只接受白名单 key。图标允许在多个类别间重复使用。

### 5. 名称和创建幂等

自定义名称去除首尾空白后必须包含 1–4 个 Unicode 可见字符。同一用户、同一收支类型下，系统类别和用户类别无论启用或停用均不得重名。Case 先校验系统定义和用户定义，数据库唯一约束收敛并发自定义重名。

`(owner_user_id, create_request_id)` 保证创建幂等；同一请求键重试返回第一次创建的类别，不生成重复定义。

### 6. 停用与账目软删除使用同一事务

停用 Case 在事务内：

1. 解析当前用户可管理的类别并锁定/更新配置；
2. 将配置设为 DISABLED；
3. 按 `user_id + type + category_code + ACTIVE` 批量把账目标记为 DELETED；
4. 规范化剩余启用类别顺序；
5. 返回实际软删除数量。

现有 `(user_id,status,type,category_code,entry_date)` 索引支持批量更新条件。重复停用按幂等成功处理。启用只将配置设为 ENABLED 并排到末尾，不恢复 DELETED 账目。

### 7. 排序提交完整启用集合

重排请求包含一个收支类型下完整的已启用类别代码。Case 校验无重复、集合与当前启用类别一致、类别均对用户可见且类型匹配，再按固定步长持久化顺序。集合过期时返回可理解的业务错误，客户端重新加载，避免部分排序。

### 8. API 与响应扩展

保留 `GET /api/bookkeeping/categories` 作为表单和筛选器的启用类别查询，返回用户顺序及 `iconKey/scope`。新增：

```text
GET  /api/bookkeeping/category-settings
GET  /api/bookkeeping/category-icons
POST /api/bookkeeping/categories
POST /api/bookkeeping/categories/{categoryCode}/enable
POST /api/bookkeeping/categories/{categoryCode}/disable
POST /api/bookkeeping/categories/reorder
```

设置查询返回 enabled/disabled 两组，并返回每个类别的活动账目数。停用响应返回实际删除数。条目和统计分类响应增加 `categoryIconKey`，名称与图标均由用户可见类别解析，不再通过固定枚举反查。

## Security / Privacy

- 所有用户配置、自定义类别、活动账目数、停用、启用和排序均强制使用 `userId` 过滤。
- 他人自定义类别与不存在类别使用不泄露归属关系的统一错误。
- 停用确认只返回当前用户的计数；名称、金额、备注和用户关系不写日志。
- Mapper XML 只使用参数绑定；动态排序通过逐项更新或受控 CASE 参数实现，不拼接用户输入。
- 停用类别不触碰任何资产表。

## Compatibility / Rollback

- 现有系统类别 code、账目 schema 和分类查询路径保持兼容，响应只做增量扩展。
- 数据库先增量部署，Server 再切换类别解析，Client 最后开放管理入口。
- UI 可通过移除入口回退；类别定义、配置表和新解析必须保留，以继续读取已创建的自定义类别。
- 已出现 USER 类别数据后，不得直接回退到只识别枚举的旧 Server；必要时只关闭写入口。

## Open Questions

当前无待确认事项。类别表结构、默认目录、名称规则、图标复用、停用语义、排序和回滚边界均已由用户确认。
