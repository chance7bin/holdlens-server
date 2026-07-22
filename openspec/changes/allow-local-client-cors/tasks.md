## 1. OpenSpec

- [x] 1.1 创建 proposal、design、spec 和 tasks，明确全环境本机 H5 Origin pattern、安全边界与回滚方式。

## 2. Spring MVC 配置

- [x] 2.1 在应用装配层为 `/api/**` 注册 CORS mapping，允许 `localhost` 和 `127.0.0.1` 的任意 HTTP 端口 Origin。
- [x] 2.2 限定允许方法为 GET、POST、OPTIONS，并保持 credentials 关闭。

## 3. 验证

- [x] 3.1 补充自动化测试，覆盖两种本机主机名的多个端口、POST 预检、未知及 HTTPS Origin、内部接口和 credentials 行为。
- [x] 3.2 使用 JDK 17 编译相关 Maven 模块，并独立运行 `CorsConfigTest`；app 模块既有 Surefire 配置会跳过测试。
- [x] 3.3 运行 `openspec validate --strict allow-local-client-cors`。
