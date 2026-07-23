## Why

资产管理 UI 已确认系统目录是所有用户共享的默认目录，用户自定义目录通过 `userId` 隔离；当前服务端却只允许在“投资资产”系统分组下创建用户子目录，无法表达“银行卡 / 招商银行 / 工资卡”这类按用户细化的目录。同时，资产记录缺少关联基金代码或股票代码与市场，公共基金详情仍要求客户端额外传递 `fundCode`，与统一传递不透明 `assetRef` 的客户端边界不一致。

## What Changes

- 允许用户在任意可见、启用的一级系统目录下创建带 `userId` 的私有二级目录，同时禁止同一用户在一个目录下混合直接资产记录和子目录。
- 将系统目录 `BANK_CARD` 的展示名称统一为“银行卡”。
- 资产记录响应按 `assetKind` 返回基金或股票专属的关联信息；股票不虚构 `assetType`。
- 新增统一公共市场详情接口，使用 `userId + assetKind + assetRef` 查询基金或股票详情，不读取用户资产记录。
- 保留现有基金、股票详情接口作为兼容入口。
- 首期 UI 不展示基金风险等级；服务端不根据基金类型推导风险等级。

## Capabilities

### New Capabilities

- `asset-management-ui-alignment`：对齐系统/用户目录层级、资产记录关联标识和统一市场详情查询。

## Impact

- 更新根目录客户端契约与资产管理 UI 原型。
- 影响 portfolio、marketasset 的 API、Case、Domain Entity、Repository 映射和 Trigger。
- 不新增资产事实表，不改变 `asset_kind + asset_id` 的内部引用方式。
- 需要迁移既有 `BANK_CARD` 系统目录名称，但不修改用户资产记录。
