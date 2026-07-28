## ADDED Requirements

### Requirement: 允许指定本机 H5 Origin 访问业务 API

server SHALL 在所有运行环境允许 `http://localhost` 和 `http://127.0.0.1` 的任意端口 Origin 跨域访问 `/api/**`，并且 SHALL NOT 将该许可扩展到其他主机或 HTTPS Origin。

#### Scenario: localhost 页面读取业务 API

- **WHEN** Origin 为任意端口的 `http://localhost` 页面向 `/api/**` 发起 GET 请求
- **THEN** server SHALL 返回与请求 Origin 匹配的 `Access-Control-Allow-Origin` 响应头

#### Scenario: 127.0.0.1 页面读取业务 API

- **WHEN** Origin 为任意端口的 `http://127.0.0.1` 页面向 `/api/**` 发起 GET 请求
- **THEN** server SHALL 返回与请求 Origin 匹配的 `Access-Control-Allow-Origin` 响应头

#### Scenario: JSON POST 预检

- **WHEN** 任一允许 Origin 为 `/api/**` 的 POST 请求发送 OPTIONS 预检
- **THEN** server SHALL 允许 POST 方法和请求声明的 JSON Content-Type 请求头

#### Scenario: 未知 Origin 被拒绝

- **WHEN** 未列入白名单的 Origin 向 `/api/**` 发起跨域请求
- **THEN** server SHALL NOT 返回允许该 Origin 的 CORS 响应头
- **AND** server SHALL 拒绝该跨域请求

#### Scenario: HTTPS localhost Origin 被拒绝

- **WHEN** HTTPS 协议的 localhost Origin 向 `/api/**` 发起跨域请求
- **THEN** server SHALL NOT 返回允许该 Origin 的 CORS 响应头
- **AND** server SHALL 拒绝该跨域请求

#### Scenario: 内部接口不开放跨域

- **WHEN** 任一允许 Origin 请求 `/internal/**`
- **THEN** server SHALL NOT 返回 `Access-Control-Allow-Origin` 响应头

### Requirement: CORS 不启用跨域凭证

server SHALL NOT 为本机 H5 Origin 启用跨域凭证。

#### Scenario: 允许 Origin 的响应不声明凭证

- **WHEN** 任一允许 Origin 请求 `/api/**`
- **THEN** server SHALL NOT 返回值为 `true` 的 `Access-Control-Allow-Credentials` 响应头
