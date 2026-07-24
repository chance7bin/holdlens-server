## Why

资产管理客户端需要在一次查询中获得当前汇总、可见目录、活跃记录以及服务端权威的目标币种换算结果。现有 `/api/assets/summary` 只返回全局汇总，目录和记录接口只返回原币事实，客户端无法可靠展示目录递归小计、单条记录换算状态和汇率缺失范围。公共市场详情页还需要按不透明 `assetRef` 查询当前用户已登记记录，并能读取单条活跃记录详情。

本需求来源于已经确认的资产管理 UI 与前后端契约讨论，不直接关联 `docs/requirements/**/prd-*.md`。本次能力依赖同一组资产、目录和汇率事实，需要作为一个原子接口增量交付。

## What Changes

- 新增 `GET /api/assets/overview`，一次返回目标币种汇总、可见目录、活跃资产记录及服务端换算结果。
- 为每个目录返回包含全部后代活跃记录的非负目标币种小计、部分结果标识和准确的缺失币种。
- 为每条活跃记录返回目标币种金额或明确的 `MISSING_RATE` 状态，并保留原币金额和创建/更新时间。
- 扩展资产记录列表接口，允许按不透明 `assetRef` 过滤且始终按 `userId` 隔离。
- 新增单条活跃资产记录详情接口，禁止跨用户或读取已归档、已删除记录。
- 更新客户端共享契约，并补充 Domain、Case、Infrastructure、Trigger 测试。

## Capabilities

### New Capabilities

- `asset-management-overview`：资产管理首页聚合查询、目录递归小计、记录换算状态和用户隔离的登记记录查询。

## Impact

- 影响 portfolio 的 Domain 服务与查询结果、Case 编排、Repository 查询、API DTO 和 HTTP Trigger。
- 更新根目录 `contracts/holdlens-server/client/asset-management.md`。
- 不新增数据表，不改写资产记录原币金额，不改变现有 `/api/assets/summary` 语义。
- 不向客户端返回 `fund.id`、`stock_market.id` 等公共数据库技术 ID。

## Success Criteria

- 客户端可只依赖 overview 响应展示全局汇总、目录小计和记录列表，无需自行读取或推导汇率。
- 缺少必要汇率时，记录、目录和全局汇总均明确表达部分结果，且无法换算金额不按零冒充已换算金额。
- `assetRef` 过滤和记录详情只返回请求用户自己的活跃记录。
- 相关 Maven 测试、聚合测试和严格 OpenSpec 验证通过。
