## Why

服务端 CORS 与团队开发约定只开放 `GET`、`POST` 和预检 `OPTIONS`，但现有资产目录、资产记录、自选移除和内部汇率写入仍混用 `PUT`、`PATCH`、`DELETE`。这会让同一项目形成两套 HTTP 风格，并使浏览器客户端在调用未进入 CORS 白名单的方法时失败。

## What Changes

- 所有 HTTP 查询接口继续使用无副作用的 `GET`。
- 所有创建、更新、删除、移除、归档、恢复和写入操作统一使用 `POST`。
- 非创建类写操作使用明确的动作路径，迁移现有资产目录、资产记录、自选移除和内部汇率写入接口。
- 同步根目录接口契约、客户端 API 适配和服务端映射测试，不保留旧 `PUT`、`PATCH`、`DELETE` 兼容入口。
- HTTP 方法调整不改变现有字段级写入边界、用户隔离、权限、审计、事务或业务错误语义。

## Capabilities

### New Capabilities

- `get-post-http-api-convention`：统一 HoldLens Server 的 HTTP 方法与动作路径约定。

## Impact

- 影响 `holdlens-server-api` 和 `holdlens-server-trigger` 的 HTTP 入参及映射。
- 影响根目录 `contracts/holdlens-server/client` 下的资产管理与自选契约。
- 影响 `holdlens-client` 的资产管理请求适配与测试。
- 不修改领域模型、持久化结构、金额语义或长期业务事实。
