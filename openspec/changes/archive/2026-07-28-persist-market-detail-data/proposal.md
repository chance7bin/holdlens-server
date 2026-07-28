## Why

基金详情和股票详情需要历史曲线及公司资料。当前 server 只保存基金/股票当前快照，没有历史净值、价格 bar 或公司资料表，也没有接收 agent 结构化结果的回调和客户端查询接口。本变更依据根目录市场详情刷新和客户端详情契约，让 server 继续承担长期业务事实源、幂等落库和展示 API 职责。

本变更不直接关联现有 PRD；需求来源为首期四页面和已冻结跨项目契约。

## What Changes

- 新增受控市场详情数据刷新任务创建、agent 派发和 callback 处理。
- 新增基金净值历史、股票价格 bar、股票公司资料持久化模型和迁移。
- 新增基金净值历史、股票价格历史和公司资料客户端查询 API。
- callback 支持成功、部分失败、失败和稳定幂等键，合法 slice 独立保存。
- 查询使用 `null` 和空数组表达可信缺失，不伪造数据。

## Capabilities

### New Capabilities

- `market-detail-data-persistence`：市场详情刷新编排、历史/profile 落库和客户端查询。

## Impact

- 新增数据库表、Migration、Domain/Repository/DAO/Mapper、Case、API 和 Controller。
- 新增 processing task 类型和 agent port 调用。
- 不修改自选、统一搜索或当前股票详情实现文件。
- 需要数据库迁移、幂等 callback、批量写入和历史区间查询测试。
