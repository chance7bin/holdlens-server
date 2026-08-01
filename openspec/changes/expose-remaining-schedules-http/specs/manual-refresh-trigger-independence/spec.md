## MODIFIED Requirements

### Requirement: Manual refresh is independent from schedule switches

server SHALL ensure that schedule `enabled` properties control only cron-triggered execution and SHALL NOT prevent any HTTP manual schedule endpoint from invoking its corresponding Case.

#### Scenario: Manually trigger fund catalog while schedule is disabled
- **WHEN** `holdlens.agent.fund-catalog-refresh-schedule.enabled` is `false`
- **AND** a caller submits `POST /api/agent/fund-catalog-refresh/schedule-runs`
- **THEN** server SHALL invoke the fund catalog refresh Case with trigger source `manual`

#### Scenario: Manually trigger fund top holdings while schedule is disabled
- **WHEN** `holdlens.agent.fund-top-holding-refresh-schedule.enabled` is `false`
- **AND** a caller submits `POST /api/agent/fund-top-holding-refresh/schedule-runs`
- **THEN** server SHALL invoke the fund top-holding refresh Case with trigger source `manual` and the configured batch size

#### Scenario: Manually trigger fund asset allocation while schedule is disabled
- **WHEN** `holdlens.agent.fund-asset-allocation-refresh-schedule.enabled` is `false`
- **AND** a caller submits `POST /api/agent/fund-asset-allocation-refresh/schedule-runs`
- **THEN** server SHALL invoke the fund asset-allocation refresh Case with trigger source `manual` and the configured batch size

#### Scenario: Manually trigger fund purchase status while schedule is disabled
- **WHEN** `holdlens.agent.fund-purchase-status-refresh-schedule.enabled` is `false`
- **AND** a caller submits `POST /api/agent/fund-purchase-status-refresh/schedule-runs`
- **THEN** server SHALL invoke the fund purchase-status refresh Case with trigger source `manual`
- **AND** server SHALL return a successful empty response

#### Scenario: Manually process fund callback timeout while schedule is disabled
- **WHEN** `holdlens.agent.fund-slice-callback-timeout.enabled` is `false`
- **AND** a caller submits `POST /api/agent/fund-slice-callback-timeout/schedule-runs`
- **THEN** server SHALL close timed-out callbacks using the configured timeout minutes
- **AND** server SHALL warn for slow catalog callbacks using the configured warning minutes
- **AND** server SHALL return a successful empty response

#### Scenario: Manually trigger an all-market refresh while schedule is disabled
- **WHEN** the corresponding A-share or US-stock market refresh schedule is disabled
- **AND** a caller submits its `POST /api/agent/{market}-market-refresh/schedule-runs` endpoint
- **THEN** server SHALL invoke the corresponding all-market refresh Case
- **AND** server SHALL return a successful empty response

#### Scenario: Manually trigger active fund detail refresh while schedule is disabled
- **WHEN** `holdlens.agent.active-fund-detail-refresh-schedule.enabled` is `false`
- **AND** a caller submits `POST /api/agent/active-fund-detail-refresh/schedule-runs`
- **THEN** server SHALL invoke the active fund-detail refresh Case
- **AND** server SHALL return a successful empty response

#### Scenario: Manually trigger active stock detail refresh while schedule is disabled
- **WHEN** `holdlens.agent.active-stock-detail-refresh-schedule.enabled` is `false`
- **AND** a caller submits the active A-share or active US-stock detail `POST .../schedule-runs` endpoint
- **THEN** server SHALL invoke the stock-detail refresh Case with the market represented by the endpoint
- **AND** server SHALL return a successful empty response

#### Scenario: Disabled cron schedule does not invoke refresh Case
- **WHEN** any refresh or callback-timeout cron fires while its `enabled` property is `false`
- **THEN** server SHALL NOT invoke the corresponding Case
