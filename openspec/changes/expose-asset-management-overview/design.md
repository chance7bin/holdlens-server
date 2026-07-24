## Context

现有资产事实由 `asset_catalog`、`asset_record` 和 `exchange_rate` 保存。`AssetSummaryService` 已实现同币种、外币兑人民币、人民币兑外币和经人民币交叉换算，并在汇率缺失时返回部分汇总；`AssetManagementCaseImpl` 已负责查询活跃记录和汇率。现有目录查询按系统目录全局可见、用户目录按 `userId` 隔离，资产记录查询按 `user_id` 过滤。

客户端当前需要并列展示全局汇总、一级/二级目录目标币种小计和记录换算金额。若由客户端分别调用目录、记录、汇率接口并计算，会复制服务端换算规则并产生不一致。新增查询必须继续遵循 `Trigger → API → Case → Domain ← Infrastructure`：Trigger 只绑定参数和映射 DTO，Case 编排目录、记录和汇率，Domain 负责换算及目录递归汇总，Infrastructure 只执行参数化查询。

## Goals / Non-Goals

**Goals:**

- 复用现有资产和汇率事实，一次返回资产管理页面需要的扁平数据。
- 保证目录小计递归包含所有后代活跃记录，且金额方向不改变目录展示的非负规模。
- 在记录、目录和全局三个层级一致表达汇率缺失。
- 支持按不透明 `assetRef` 查询用户登记记录，并查询单条活跃记录详情。
- 保持用户隔离、隐私和公共数据库技术 ID 不外泄。

**Non-Goals:**

- 不保存换算后金额或用户目标币种偏好。
- 不新增汇率抓取、历史汇率、归档记录列表或变更历史接口。
- 不改变目录创建、资产录入、拆分、归档、恢复或删除规则。
- 不在 overview 中嵌套目录树；客户端依据 `id + parentId` 组织展示。

## Decisions

### 1. 一个原子 change 交付聚合查询和登记关系查询

overview、`assetRef` 过滤和单条详情共同支撑同一资产管理 UI，复用同一用户隔离与记录 DTO，且没有独立发布价值。拆分会使客户端必须在不完整契约之间兼容，因此本次使用一个 `expose-asset-management-overview` change。

### 2. Domain 统一计算换算结果和目录递归小计

新增资产总览领域结果，包含原有全局汇总、每条记录的换算结果和每个可见目录的递归小计。`AssetSummaryService` 继续作为汇率换算规则的唯一实现：

- 同币种直接返回原金额；
- 外币与目标币种按现有人民币中间价换算；
- 缺少任一必要汇率时记录 `convertedAmount = null`、`conversionStatus = MISSING_RATE`；
- 目录只累加成功换算的金额，并通过 `partial=true` 和 `missingCurrencies` 明确其余记录未参与，不能把缺失记录解释为零金额；
- 一级目录递归包含二级目录，叶子目录包含直接记录；实现对层级异常保持防循环保护；
- 目录小计始终是非负规模，`SUBTRACT` 只在全局 `liabilityTotal/netAsset` 中决定方向。

### 3. Case 一次装载事实并编排

`AssetManagementCaseImpl` 查询当前用户可见启用目录、当前用户全部活跃记录和换算所需最新汇率，然后调用 Domain 服务生成总览。服务端是汇率选择和金额汇总事实源，客户端只格式化返回值。

### 4. 记录查询由 Repository 保证用户与状态边界

资产记录列表增加可选 `assetRef`。未传时保持原有行为；传入时使用 Mapper XML 对服务端签发的引用表达式做精确匹配，并同时限制 `user_id` 与 `status = ACTIVE`。`assetRef` 对客户端保持不透明，API 不要求客户端解析代码或市场。

单条详情使用独立的 `user_id + id + ACTIVE` 查询，避免改变既有更新和恢复流程对非活跃记录的内部读取语义。不存在、跨用户、已归档或已删除统一按现有“资产记录不存在或不可见”业务错误处理。

### 5. API 返回扁平数据且不泄露技术 ID

`GET /api/assets/overview?userId=&targetCurrency=CNY` 的 `data` 直接包含：

```text
targetCurrency, assetTotal, liabilityTotal, netAsset,
partial, missingCurrencies, unconvertedAmounts,
catalogs, records
```

overview 目录保留现有目录字段并新增 `convertedAmount`、`partial`、`missingCurrencies`。overview 记录保留现有记录字段并新增 `convertedAmount`、`conversionStatus`、`createTime`、`updateTime`。关联基金和股票仍只返回 `assetRef` 与类型专属公共标识，不返回内部 `assetId`。

## Security / Privacy

- 所有目录、记录列表和详情均以 `userId` 做可见性或所有权过滤。
- `assetRef` 仅用于参数化精确匹配，不拼接 SQL，不作为越过 `userId` 的查询键。
- Controller、Case、Repository 不记录资产金额、名称、备注、用户关系或完整响应。
- 错误信息保持通用，不回显资产名称、金额、备注或关系。

## Compatibility / Rollback

- 现有 `GET /api/assets/summary` 与未带 `assetRef` 的记录列表保持兼容。
- 新增 overview 和详情接口可独立回滚；列表过滤参数回滚后客户端需停止发送该参数。
- 无数据库迁移和持久化回滚风险；换算结果始终查询时计算。

## Open Questions

当前无待确认事项。目录完全缺失汇率时 `convertedAmount` 返回已成功换算部分的合计（可能为零），并必须结合 `partial=true` 使用；单条缺失记录则返回 `convertedAmount=null`，避免被解释为已换算的零金额。
