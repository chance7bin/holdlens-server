# 设计

## Context

当前用户私有数据已经在业务层和 SQL 层以 `userId` 隔离，但 `userId` 来自未认证的 HTTP 参数。Server 没有账号表、密码校验、用户会话、可信 Principal 或统一的认证入口。Client 当前固定使用用户 1，开发数据也归属于用户 1。

本项目保持 `Trigger → API → Case → Domain ← Infrastructure` 的依赖方向。账号与会话是 Server 的长期安全事实；密码哈希、随机令牌和 HTTP 安全过滤属于技术适配。业务用例继续接收明确的用户 ID，但该值必须由 Trigger 层从已认证身份取得。

## Goals / Non-Goals

**Goals:**

- 提供不依赖第三方身份服务的用户名密码注册和登录。
- 建立可撤销、可过期的 Bearer 会话，并为每个受保护请求提供可信用户 ID。
- 保证同一账号只有一个有效会话，并允许活跃 App 在绝对期限内显式延长闲置期限。
- 让现有用户私有链路不再信任调用方提交的 `userId`。
- 保留固定用户开发模式，同时确保该模式不能误用于非开发环境。
- 明确新账号与固定用户 1 历史数据完全隔离。
- 将 Client 随机生成的安装标识绑定到登录会话，支持服务端区分普通设备安装实例。

**Non-Goals:**

- 不实现 Client 登录注册页面、客户端令牌存储或路由守卫。
- 不实现邮箱/手机验证、找回密码、修改密码、MFA、第三方登录或管理员角色。
- 不读取硬件序列号、IMEI、系统广告标识或其他平台硬件 deviceId，也不把安装标识当作设备认证凭据。
- 不把 Agent callback、内部汇率接口或服务间认证并入用户认证。
- 不迁移、复制、重新归属或删除固定用户 1 的历史业务数据。
- 不在本次变更中移除所有 API DTO 的过渡 `userId` 字段。

## Decisions

### 1. 账号是独立限界上下文

新增 account/auth 能力，领域层定义账号、会话和 Repository/Port 接口，Case 层编排注册、登录、退出和会话认证。Infrastructure 只实现 MyBatis 数据访问、BCrypt 和安全随机数/摘要算法，Trigger 负责 HTTP 参数、Bearer 提取和安全上下文。

核心调用路径为：

```text
AuthController / AuthenticationFilter
  -> AuthenticationCase
  -> Account Domain Service
  -> IUserAccountRepository / IUserSessionRepository / IPasswordHasher / ISessionTokenPort
  <- Infrastructure adapters
```

现有资产、记账、市场自选等 Case/Domain 不依赖认证框架，也不读取 HTTP 上下文。

### 2. 用户名与密码规则

- 用户名去除首尾空白并转为小写，必须匹配 `[a-z0-9_]{3,32}`，数据库使用唯一约束收敛并发注册。
- 密码必须包含 8–72 个 UTF-8 字节；不在日志、异常或领域对象的 `toString` 中输出。
- Infrastructure 使用 Spring Security `BCryptPasswordEncoder`，强度为 12；数据库只保存 BCrypt 哈希。
- 登录对不存在账号使用固定的 dummy BCrypt 哈希执行一次校验，并对用户名不存在、密码错误和账号锁定返回相同错误，降低枚举和明显计时差异。

### 3. 不透明随机会话令牌

登录成功后生成 32 字节 `SecureRandom`，编码为 Base64 URL 无填充 Bearer token。原始令牌仅在登录成功响应中返回一次；数据库保存 SHA-256 十六进制摘要。

新增 `user_session`：

```text
id, userId, tokenHash, installationId, deviceName, expiresAt, revokedAt, createTime
```

- `token_hash` 全局唯一，查询只按摘要精确匹配。
- 默认闲置有效期 7 天，由 `holdlens.auth.session-ttl` 配置；登录创建时 `expiresAt = now + sessionTtl`。
- 默认绝对有效期 90 天，由 `holdlens.auth.session-absolute-ttl` 配置；续期后的到期时间不得晚于 `createTime + sessionAbsoluteTtl`。
- 同一账号登录成功时，在账号行锁保护的同一事务内先撤销该账号全部有效旧会话，再创建新会话。旧 token 后续统一返回 401；会话可以保存 Client 随机安装标识和可读设备名称，用于审计和设备区分，但不改变单有效会话规则。
- 退出将当前会话标记为撤销；撤销写入本身幂等，过期或撤销会话均不得恢复认证，后续再次携带该 token 请求任何受保护接口都会返回 401。
- `POST /api/auth/session/renew` 使用当前 token 对应的会话行，在会话仍有效且未达到绝对期限时把 `expiresAt` 更新为 `min(now + sessionTtl, createTime + sessionAbsoluteTtl)`，并返回新的 `expiresAt`。续期不轮换原始 token。
- 本次不实现 refresh token。闲置过期、撤销或达到绝对期限后用户重新登录，降低双令牌和轮换状态的复杂度。

### 4. 账号锁定与失败语义

新增 `user_account`：

```text
id, username, passwordHash, status,
failedLoginCount, lockedUntil, createTime, updateTime
```

- 连续 5 次失败后锁定 15 分钟；阈值与时长使用 Server 配置。
- 成功登录原子清零失败计数和锁定时间，然后创建会话。
- 登录失败更新必须限定目标账号，不为不存在用户名创建记录。
- `ACTIVE/DISABLED` 状态为后续禁用入口保留；本次不提供管理 API。

### 5. 新账号不继承固定用户数据

初始化 SQL 和增量迁移创建 `user_account` 时设置 `AUTO_INCREMENT=2`，保留用户 ID 1 给当前开发期固定身份，但不为其创建可登录账号。首个及后续注册账号从 2 开始，所有业务表仍按现有 BIGINT 用户 ID 关联，因此新账号自然得到空资产、空记账、空自选和空持仓视图。

迁移不读取、搬运或删除用户 1 数据，也不创建默认账号或默认密码。

### 6. 两种认证模式

配置 `holdlens.auth.mode`：

- `fixed`：每个受保护请求自动得到 `fixed-user-id`，开发环境默认值为 1，不要求 Authorization header。
- `session`：要求 `Authorization: Bearer <token>`，通过会话表解析账号。

应用启动时校验：非 `dev/test` profile 使用 `fixed` 模式立即失败，防止生产环境误配置绕过认证。开发者可在 dev profile 显式设置 `session`，完整调试注册、登录、认证和退出。

### 7. HTTP 保护范围与错误

- `POST /api/auth/register` 与 `POST /api/auth/login` 匿名可访问。
- `POST /api/auth/logout` 与 `GET /api/auth/me` 必须认证。
- `POST /api/auth/session/renew` 必须认证，并且只能续期当前 Bearer 会话。
- 除 CORS `OPTIONS` 外，全部 `/api/**` 默认必须认证，包括用户数据、市场数据和人工 Agent 调度入口，避免新接口因漏配路径而匿名开放。
- `/internal/**` 不进入用户认证过滤器，其服务认证由独立变更处理。
- 认证失败返回 HTTP 401，可信身份与过渡 `userId` 不一致返回 HTTP 403，参数或业务校验失败继续使用既有响应语义。

过滤器只把已认证用户 ID 和会话标识写入请求级 SecurityContext，并在请求结束后清理；日志不得记录 Authorization header 或 token。

### 8. 过渡 userId 兼容

现有 Client 仍提交 `userId`，因此本次保留查询参数和请求字段。所有用户私有 Controller 在调用 Case 前执行统一规则：

1. 从当前认证上下文取得可信用户 ID；
2. 如果请求显式携带 `userId`，只检查其是否与可信身份一致；
3. 向 Case/Domain 传递可信用户 ID，绝不传递未经认证的请求值。

后续 Client 完成令牌接入后，可通过独立兼容性变更移除冗余 `userId`。

### 9. 随机会话安装标识

Client 首次安装时生成符合 UUID 格式的随机 `installationId`，并在登录请求中同时提交本地可读的 `deviceName`。Server 将两者保存到新会话：

- `installationId` 只表示一次客户端安装，卸载重装后可以变化；它不是硬件唯一标识。
- Server 不使用 `installationId` 或 `deviceName` 建立身份、恢复会话、绕过密码或决定权限。
- 兼容期允许旧 Client 省略这两个字段，历史会话保持为空；新 Client 必须提交随机安装标识。
- 设备名称和安装标识不得进入普通日志或认证错误响应。
- 数据库按 `user_id + installation_id` 建立普通索引，为后续受控的设备会话查询提供基础，但本次不新增历史设备管理 API。

## Security / Privacy

- BCrypt 哈希、会话摘要、原始令牌和密码均不得写入普通日志；认证异常使用稳定、无枚举信息的消息。
- 注册与登录 DTO 必须禁用或避免自动生成包含密码的 `toString`。
- 会话查询使用参数化 SQL；比较令牌摘要时不查询或返回其他会话信息。
- 受保护请求先认证再进入 Controller，身份不一致在任何业务查询或写入之前失败。
- Bearer token 不使用 Cookie，当前链路不依赖浏览器自动携带凭据，避免引入基于 Cookie 的 CSRF 状态。
- 账号锁定减缓单账号暴力尝试；公网部署仍应在网关增加按 IP/路径限流，本次不实现分布式限流。
- 安装标识是可由 Client 提交的审计元数据，不能作为可信设备证明；Server 仅做格式、长度和控制字符校验，并使用参数化 SQL 保存。

## Consistency / Transactions

- 注册在单事务内创建唯一账号；并发同用户名只允许一个成功。
- 登录成功撤销旧会话、重置失败计数和创建新会话在同一事务内；并发登录通过账号行锁串行化，最终只保留最后创建的有效会话。
- 失败计数更新使用数据库条件更新，避免并发失败丢失计数。
- 会话撤销写入是幂等操作；客户端在 token 已撤销后重试 HTTP 退出请求会因无法再认证而返回 401，且不会恢复会话。
- 会话续期锁定当前会话行并重新检查撤销、闲置和绝对期限；与新登录撤销并发时，已经撤销的会话不得被续期恢复。

## Compatibility / Rollback

- dev 默认 `fixed`，现有本地 Client 和脚本继续以用户 1 工作。
- session 模式下，旧 Client 未携带 token 时将得到 401，这是启用真实认证的预期行为。
- 未接入续期的 Client 仍可使用固定 7 天会话，到期后重新登录；续期是向后兼容的新增动作。
- 旧 Client 可以暂时省略安装标识；新迁移列允许为空，现有会话不失效。
- 数据库变更先部署，再启用 session 模式；回滚应用时可保留账号和会话表，不影响旧业务表。
- 已创建新账号业务数据后不得把该账号映射回用户 1；回滚仅关闭 session 入口，不改写业务所有权。

## Open Questions

当前无待确认事项。用户已确认自建用户名密码体系、新账号从空数据开始、开发环境保留固定用户调试，并授权创建本 OpenSpec 后并行实现。
