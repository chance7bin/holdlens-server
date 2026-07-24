## Context

当前服务端存在 6 个非 `GET`/`POST` 映射：资产目录更新与删除、资产记录详情与金额更新、自选移除，以及内部汇率 upsert。客户端使用 JSON 请求体，跨域时无论使用 `PATCH` 还是 `POST` 都可能触发 OPTIONS 预检；统一为 `POST` 的目标是收敛项目接口规范并与现有 CORS 方法白名单一致，而不是取消预检。

## Goals / Non-Goals

**Goals:**

- 全部 HTTP 入口只使用 `GET` 或 `POST`，预检继续由框架处理 `OPTIONS`。
- GET 严格保持无副作用，所有状态变更使用 POST。
- 动作路径明确表达局部更新或状态变化，不因改用 POST 放宽业务写入边界。
- 服务端、客户端、共享契约和测试一次性迁移，不保留两套并行接口。

**Non-Goals:**

- 不改变资产目录、资产记录、自选关系和汇率的业务规则。
- 不新增鉴权机制或修改现有用户标识传递方式之外的安全语义。
- 不以 GET 执行任何写操作。
- 不保留旧 HTTP 方法作为兼容别名。

## Decisions

### 1. 接口迁移表

| 旧接口 | 新接口 |
| --- | --- |
| `PUT /api/asset-catalogs/{catalogId}` | `POST /api/asset-catalogs/{catalogId}/update-details` |
| `DELETE /api/asset-catalogs/{catalogId}?userId=...` | `POST /api/asset-catalogs/{catalogId}/delete`，`userId` 放入 JSON 请求体 |
| `PATCH /api/asset-records/{recordId}/details` | `POST /api/asset-records/{recordId}/update-details` |
| `PATCH /api/asset-records/{recordId}/amount` | `POST /api/asset-records/{recordId}/update-amount` |
| `DELETE /api/watchlist/assets?...` | `POST /api/watchlist/assets/remove`，参数放入 JSON 请求体 |
| `PUT /internal/exchange-rates` | `POST /internal/exchange-rates/upsert` |

原有创建、归档、恢复、删除记录、拆分和 agent 任务入口已经使用 POST，查询入口已经使用 GET，不改变路径。

### 2. 写入边界继续由专用 DTO 和用例保护

资产目录更新继续使用 `UpdateCatalog`，记录详情与金额分别使用 `UpdateDetails` 和 `UpdateAmount`，目录删除使用只包含 `userId` 的 `UserOperation`。自选移除新增只包含 `userId + assetKind + assetRef` 的请求 DTO。Controller 只做绑定与转换，现有 Case、Domain、Repository 语义不变。

### 3. 原子迁移，不保留旧映射

服务端映射、客户端适配和共享契约在同一变更中更新。旧方法不再暴露，避免维护两套路径和产生新旧调用方行为差异。本地联调需要服务端与客户端代码同时更新并重启后端。

## Security / Privacy

- POST 不替代鉴权；所有现有用户隔离、权限检查和资产审计必须保持。
- 删除目录与移除自选的参数改入 JSON 请求体，不在 URL 中暴露用户标识和资产引用。
- 请求和测试继续使用脱敏示例，不记录真实资产数据。

## Rollback

同时恢复服务端旧映射、客户端旧请求方法与路径以及根契约即可回滚。该变更不涉及数据库结构或数据迁移。

## Open Questions

当前无待确认事项。用户已确认全项目统一采用 GET/POST，并要求一次性检查和迁移现有非 GET/POST 接口。
