# 实施任务

## 1. 领域模型与用例

- [x] 1.1 新增账号、登录失败/锁定、会话状态领域模型和账号/会话 Repository 接口；验证：用户名规范化、密码长度边界、锁定阈值、过期与撤销 Domain 测试。
- [x] 1.2 新增密码哈希与会话令牌 Port，并实现注册、登录、认证、退出和当前账号 Case；验证：重复注册、dummy hash、失败锁定、成功重置、会话创建/撤销和事务 Case 测试。
- [x] 1.3 保证注册账号 ID 不使用 1，且不初始化或复制任何固定用户业务数据；验证：首个账号分配边界和空数据所有权测试。

## 2. 数据库与基础设施

- [x] 2.1 新增账号/会话 PO、DAO、Repository 适配器和 MyBatis XML；验证：参数化 SQL、用户名唯一、token 摘要唯一、过期/撤销过滤和并发失败计数测试。
- [x] 2.2 实现 BCrypt 密码 Port、SecureRandom 原始令牌与 SHA-256 摘要 Port；验证：哈希不可逆、同密码不同哈希、摘要稳定、原始 token 不持久化测试。
- [x] 2.3 更新初始化 SQL并新增增量迁移，创建 `user_account` 与 `user_session`，账号自增从 2 开始；验证：初始化/迁移结构一致且不修改用户 1 业务数据。

## 3. API 与认证入口

- [x] 3.1 新增注册、登录、退出、当前账号请求响应 DTO 和 Controller；验证：只使用 GET/POST、参数校验、401/403、响应不暴露密码或 token 摘要。
- [x] 3.2 新增 Spring Security 配置、Bearer 过滤器和请求级当前用户上下文；验证：`/api/**` 默认保护、匿名白名单、OPTIONS 放行、`/internal/**` 不使用用户身份。
- [x] 3.3 新增 fixed/session 配置与启动保护；验证：dev 默认固定用户 1、dev 可切 session、非 dev/test 使用 fixed 启动失败。

## 4. 现有用户私有链路接入

- [x] 4.1 资产目录、资产记录、资产汇总和持仓详情 Controller 改用可信用户 ID；验证：请求 userId 不一致先返回 403，Case 只收到可信 ID。
- [x] 4.2 记账类别、账目、统计和账单 Controller 改用可信用户 ID；验证：全部读写路径覆盖且无未经认证 userId 下传。
- [x] 4.3 自选、市场详情和其他带用户语义的 Controller 改用可信用户 ID；验证：查询、ensure、添加、移除均使用身份上下文。
- [x] 4.4 更新根目录 Server/Client 认证契约，明确 Authorization、端点、状态、兼容 userId 和 Client 后续接入边界；验证：契约与 API/过滤器逐项一致。

## 5. 质量门与安全

- [x] 5.1 运行 Domain、Case、Infrastructure、Trigger、App 相关测试并确认目标测试实际执行。
- [x] 5.2 按 runbook 使用 JDK 17 串行运行 Maven 聚合测试和打包/编译质量门。
- [x] 5.3 检查密码、Authorization、原始 token、token 摘要不进入日志或响应，并检查用户名枚举、会话撤销、用户隔离、事务和默认拒绝策略。
- [x] 5.4 同步任务状态并运行 `openspec validate --strict add-user-authentication`。

## 6. 单设备会话与显式续期增量

- [x] 6.1 扩展会话领域模型、Repository 和 MyBatis，使登录在账号行锁事务内撤销旧会话，并使续期按会话行锁重新检查有效性；验证：并发登录、撤销与续期竞态测试最终只保留新会话。
- [x] 6.2 新增 7 天闲置与 90 天绝对期限配置和 Case 续期编排，计算 `min(now + idleTtl, createTime + absoluteTtl)` 且不轮换 token；验证：正常续期、闲置过期、已撤销和绝对期限边界测试。
- [x] 6.3 新增 `POST /api/auth/session/renew` 与 `{expiresAt}` 响应，并同步根目录认证契约；验证：仅当前已认证会话可续期，fixed 模式客户端不调用该接口。
- [x] 6.4 运行认证 Domain、Case、Infrastructure、Trigger、App 测试与 JDK 17 Maven 质量门，检查 token 不进入日志/响应、旧会话不可恢复和用户隔离。
- [x] 6.5 同步任务状态并运行 `openspec validate --strict add-user-authentication`。

## 7. 客户端安装标识

- [x] 7.1 扩展登录请求和会话领域模型，接收随机安装标识与设备名称，且不将其用于认证或授权；验证：格式规范化、非法输入和兼容空值测试。
- [x] 7.2 扩展 PO、Repository、MyBatis、初始化 SQL 和增量迁移，将设备元数据绑定到会话并保持历史会话兼容；验证：参数绑定、索引、字段映射和迁移结构测试。
- [x] 7.3 检查安装标识、设备名称不进入日志和错误响应，运行认证相关测试、JDK 17 Maven 质量门及严格 OpenSpec 校验。
