## 1. 契约与规范

- [x] 1.1 更新服务端与客户端 Coding Standards，固定 GET 查询、POST 写入约定。
- [x] 1.2 更新资产管理、自选移除及内部汇率共享契约。

## 2. 服务端迁移

- [x] 2.1 将资产目录、资产记录和内部汇率非 GET/POST 映射迁移为明确的 POST 动作路径。
- [x] 2.2 将自选移除迁移为 POST JSON 请求 DTO，并保持原有业务语义。
- [x] 2.3 更新 Controller 契约测试，覆盖新方法、路径和请求体绑定。

## 3. 客户端迁移

- [x] 3.1 更新资产管理 API 适配，统一使用新 POST 路径和 JSON 请求体。
- [x] 3.2 更新客户端 API 测试与现有资产管理 OpenSpec 增量。

## 4. 验证

- [x] 4.1 运行服务端相关 Maven 测试和客户端 Node 测试、JS 检查。
- [x] 4.2 全仓扫描确认没有 PUT、PATCH、DELETE Controller 映射或客户端请求。
- [x] 4.3 运行服务端和客户端 OpenSpec 严格校验。
