## ADDED Requirements

### Requirement: Manually trigger fund catalog refresh schedule

server SHALL expose an HTTP endpoint that manually invokes the existing fund catalog refresh schedule run.

#### Scenario: Trigger fund catalog refresh through HTTP

- **WHEN** a caller submits `POST /api/agent/fund-catalog-refresh/schedule-runs`
- **THEN** server SHALL invoke the same fund catalog refresh schedule method used by cron
- **AND** server SHALL return a successful empty response after the invocation completes

#### Scenario: Manual trigger ignores disabled schedule

- **WHEN** `fund-catalog-refresh-schedule.enabled` is false
- **AND** a caller submits `POST /api/agent/fund-catalog-refresh/schedule-runs`
- **THEN** server SHALL invoke the fund catalog refresh Case with trigger source `manual`
- **AND** server SHALL return a successful empty response

#### Scenario: Manual trigger skips an active catalog task

- **WHEN** a non-terminal `fund_catalog_refresh` processing task already exists
- **AND** a caller submits `POST /api/agent/fund-catalog-refresh/schedule-runs`
- **THEN** server SHALL NOT create another fund catalog refresh task
