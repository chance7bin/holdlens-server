# 变更：新增独立记账事实源

## Why

HoldLens 已有资产管理能力，但还不能保存日常收入和支出，也无法为客户端的明细、图表和账单界面提供权威数据。第一版记账已确认独立于资产模块，因此服务端需要新增单独的记账领域、持久化结构和业务 API，同时避免把收支条目误作资产记录或修改资产余额。

本需求来源于已经确认的 `holdlens-client/docs/ui/holdlens-bookkeeping-ui.html`，不直接关联 `docs/requirements/**/prd-*.md`。共享请求、响应、状态、错误和兼容语义以根目录 `contracts/holdlens-server/client/bookkeeping.md` 为准。

## What Changes

- 新增收入、支出两类收支条目的创建、查询、修订和软删除能力。
- 新增服务端维护的稳定收支分类查询，校验分类与收支类型一致。
- 新增按日期范围及可选类型、分类过滤的收支明细和筛选后汇总。
- 新增周、月、年趋势和分类排行统计，以及指定年份月账单和全量年账单。
- 新增独立记账表、迁移脚本、MyBatis XML、Repository、Case、API DTO 和 HTTP Controller。
- 更新服务端领域语言和根目录客户端共享契约。

## Capabilities

### New Capabilities

- `standalone-bookkeeping`：独立收支条目、固定分类、明细、图表统计与账单查询。

## Impact

- 新增 `bookkeeping` 领域包和对应 Case、Infrastructure、API、Trigger 实现。
- 更新数据库初始化脚本并新增可单独执行的数据库迁移。
- 所有接口继续只使用无副作用 GET 与明确动作 POST。
- 不读取或修改 `asset_catalog`、`asset_record`、`asset_record_change`、持仓或行情数据。

## Success Criteria

- 同一用户的创建请求具备幂等性，重复提交不会生成重复条目。
- 明细、统计和账单只包含当前用户自己的活动条目，并在修订或删除后立即一致。
- 周、月、年边界、平均值、零值补点、分类占比和账单结余符合共享契约。
- 非法输入、越权访问和已删除条目使用不泄露归属关系的业务错误。
- 数据库迁移、相关分层测试、Maven 聚合测试和严格 OpenSpec 校验通过。
