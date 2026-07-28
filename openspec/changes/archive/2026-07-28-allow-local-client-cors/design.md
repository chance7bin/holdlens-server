## Context

客户端通过 `uni.request` 直接访问 `http://localhost:8091/api/**`。当 H5 页面由 `http://localhost:5173` 或 `http://127.0.0.1:5173` 提供时，请求跨越 Origin；Spring MVC 当前未注册 CORS mapping，因此浏览器在业务响应到达页面代码前将其拦截。

Spring Boot 版本为 3.4.3，应用启动与 Spring 通用配置集中在 `holdlens-server-app` 的 `com.echoamoy.holdlens.server.config` 包。现有业务 Controller 和 API 契约无需调整。

## Goals / Non-Goals

**Goals:**

- 让 `localhost` 和 `127.0.0.1` 的任意 HTTP 端口 Origin 可以访问所有环境的 `/api/**`。
- 支持当前客户端使用的 GET、POST 请求及 OPTIONS 预检。
- 保持未知 Origin 和内部 agent 回调接口不对浏览器开放。
- 用自动化测试固定允许与拒绝行为。

**Non-Goals:**

- 不允许远程主机或 HTTPS Origin。
- 不开放 `/internal/**`。
- 不启用跨域 Cookie 或其他凭证。
- 不新增或替代身份认证、用户隔离与权限控制。
- 不改变现有 API 请求、响应及错误语义。

## Decisions

- 在 `holdlens-server-app` 新增 Spring MVC `WebMvcConfigurer`，由应用装配层统一注册 CORS mapping，避免在每个 Controller 上重复添加 `@CrossOrigin`。
- mapping 限定为 `/api/**`，使用 Spring MVC Origin pattern 允许 `http://localhost:[*]` 和 `http://127.0.0.1:[*]`，其中 `[*]` 表示任意端口。
- 允许方法固定为 `GET`、`POST`、`OPTIONS`；允许请求头由 Spring MVC CORS 处理器回显预检声明，以兼容 JSON `Content-Type`，但不启用 credentials。
- 配置不使用 Spring Profile 限定，因同一本机页面需要联调本地、测试和线上后端；所有环境保持相同白名单。
- 使用轻量 Spring MVC 测试上下文验证两种本机主机名的多个端口、未知 Origin、HTTPS localhost 拒绝和 `/internal/**` 不返回 CORS 头，不启动完整 application，也不连接数据库。

## Security / Privacy

- CORS 只限制浏览器读取响应，不能替代服务端鉴权；本变更不把 Origin 当作用户身份或授权依据。
- 本机任意 HTTP 端口白名单会让运行在这些本机 Origin 上的页面具备发起浏览器请求的能力，因此线上 API 仍必须独立执行用户身份、权限和数据隔离校验。
- `allowCredentials` 保持关闭，避免浏览器携带跨站 Cookie。
- `/internal/**` 不纳入 mapping，避免扩大 agent 回调接入面。

## Rollback

删除该 Spring MVC CORS 配置及对应测试即可恢复原行为；不涉及数据回滚或契约迁移。
