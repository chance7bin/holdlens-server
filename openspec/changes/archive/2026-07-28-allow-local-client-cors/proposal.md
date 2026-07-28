## Why

`holdlens-client` 的 H5 页面运行在本机 `5173` 端口，并直接请求不同 Origin 的 `holdlens-server`。服务端当前没有 CORS 响应头，浏览器会拦截 `/api/**` 响应，导致本地页面无法联调本地、测试或线上环境的后端。

本变更不需要读取产品 PRD：它只修复浏览器到现有业务 API 的 HTTP 接入能力，不改变产品流程、业务事实或接口 payload 契约。

## What Changes

- 在所有运行环境为 `/api/**` 增加统一 CORS 配置。
- 允许 `http://localhost` 和 `http://127.0.0.1` 的任意端口 Origin，避免本机 H5 开发端口变化后重复调整服务端配置。
- 允许现有客户端所需的 `GET`、`POST` 和 CORS 预检 `OPTIONS` 方法。
- 拒绝未列入白名单的 Origin，不使用允许任意主机的 `*` Origin，也不启用跨域凭证。

## Capabilities

### New Capabilities

- `local-client-cors`: 允许本机 H5 客户端跨域访问各环境的现有业务 API。

### Modified Capabilities

- 无。

## Impact

- 影响模块：`holdlens-server-app`。
- 影响接口：仅增加 `/api/**` 的浏览器跨域响应语义，不改变请求和响应 payload。
- 影响安全边界：所有环境接受 `localhost` 和 `127.0.0.1` 的任意 HTTP 端口 Origin；其他主机和 HTTPS Origin 继续被拒绝，`/internal/**` 不开放跨域。
- 影响运行：配置生效需要用户重新运行后端 application。
- 不影响数据库、领域模型、持久化规则、agent 接口和客户端代码。
