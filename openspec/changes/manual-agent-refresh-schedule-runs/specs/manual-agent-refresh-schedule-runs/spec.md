## ADDED Requirements

### Requirement: Manually trigger fund detail refresh schedule

server SHALL expose an HTTP endpoint to manually trigger the existing fund detail refresh schedule run.

#### Scenario: Trigger fund schedule through HTTP
- **WHEN** a system caller submits `POST /api/agent/fund-detail-refresh/schedule-runs`
- **THEN** server SHALL invoke the same fund detail refresh schedule logic used by the cron job
- **AND** server SHALL return a successful empty response after the invocation completes

#### Scenario: Manual fund schedule respects disabled configuration
- **WHEN** the fund detail refresh schedule is disabled
- **AND** a system caller submits `POST /api/agent/fund-detail-refresh/schedule-runs`
- **THEN** server SHALL NOT scan `fund_detail_item`
- **AND** server SHALL NOT create a scheduled fund detail refresh task
- **AND** server SHALL return a successful empty response after the invocation completes

#### Scenario: Manual fund schedule skips active same type task
- **WHEN** a non-terminal `fund_detail_refresh` processing task already exists
- **AND** a system caller submits `POST /api/agent/fund-detail-refresh/schedule-runs`
- **THEN** server SHALL skip the schedule run
- **AND** server SHALL NOT create additional fund detail refresh tasks in that run

### Requirement: Manually trigger stock quote refresh schedule

server SHALL expose an HTTP endpoint to manually trigger the existing stock quote refresh schedule run.

#### Scenario: Trigger stock schedule through HTTP
- **WHEN** a system caller submits `POST /api/agent/stock-quote-refresh/schedule-runs`
- **THEN** server SHALL invoke the same stock quote refresh schedule logic used by the cron job
- **AND** server SHALL return a successful empty response after the invocation completes

#### Scenario: Manual stock schedule respects disabled configuration
- **WHEN** the stock quote refresh schedule is disabled
- **AND** a system caller submits `POST /api/agent/stock-quote-refresh/schedule-runs`
- **THEN** server SHALL NOT scan `stock_market_current`
- **AND** server SHALL NOT create a scheduled stock quote refresh task
- **AND** server SHALL return a successful empty response after the invocation completes

#### Scenario: Manual stock schedule skips active same type task
- **WHEN** a non-terminal `stock_quote_refresh` processing task already exists
- **AND** a system caller submits `POST /api/agent/stock-quote-refresh/schedule-runs`
- **THEN** server SHALL skip the schedule run
- **AND** server SHALL NOT create additional stock quote refresh tasks in that run
