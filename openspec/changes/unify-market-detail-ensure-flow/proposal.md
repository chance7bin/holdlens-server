# Change: 统一公共基金与股票详情确保流程

## Why

基金与股票详情已经具备缺数回源能力，但入口、任务轮询、空结果判断和访问激活方式不同。客户端仍需先查询数据再依据空数组决定是否刷新，基金任务不返回操作标识，股票缺少最近详情访问时间，已有数据也没有统一的新鲜度语义。

本变更直接落实根仓库 ADR-005，不依赖额外 PRD。

## What Changes

- 新增统一详情 ensure-and-read POST，返回当前详情快照、标准数据切片状态和可选操作标识。
- 新增统一操作状态 GET；基金与股票并发请求均复用已有活动任务。
- 为股票增加全局最近详情访问时间，并让基金、股票详情访问按可配置窗口节流写入。
- 将 `MISSING/PROCESSING/AVAILABLE/EMPTY/FAILED` 与 `FRESH/STALE` 分开表达；已有陈旧数据立即返回并后台刷新。
- 使用持有、自选或近 90 天详情访问组成活动标的集合，并增加基金详情、股票详情及股票全市场行情调度。
- 区分股票行情批次抓取时间与交易所行情时间；当前无来源行情时间时只返回抓取时间。
- 保留现有详情 GET、基金刷新 POST 和股票 ensure/status API 作为兼容入口。

## Capabilities

### New Capabilities

- `market-detail-ensure-flow`: 统一详情进入、状态、任务复用、新鲜度与活动刷新调度。

### Modified Capabilities

- `market-detail-data-persistence`: 增加显式空结果、陈旧数据后台刷新和统一操作查询。
- `agent-refresh-scheduling`: 增加股票行情与活动详情调度。

## Impact

- 影响 MarketAsset/MarketDetail/Processing/FundData/StockData 的 Case、Domain、Infrastructure、Trigger、SQL、配置和测试。
- 只共享公共市场数据；用户自选状态仍按 `userId` 查询，持仓及账户事实不进入 Agent 任务。
