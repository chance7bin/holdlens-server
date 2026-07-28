## ADDED Requirements

### Requirement: Manual refresh is independent from schedule switches

server SHALL ensure that schedule `enabled` properties control only cron-triggered execution and SHALL NOT prevent the existing HTTP manual refresh endpoints from invoking their corresponding refresh cases.

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

#### Scenario: Disabled cron schedule does not invoke refresh Case
- **WHEN** any fund refresh schedule cron fires while its `enabled` property is `false`
- **THEN** server SHALL NOT invoke the corresponding refresh Case

### Requirement: Application schedule switches are disabled

server SHALL keep all currently configured fund refresh and callback-timeout schedule `enabled` properties set to `false` in both common and dev application configuration.

#### Scenario: Load common application configuration
- **WHEN** server loads `application.yml`
- **THEN** fund catalog, purchase status, period return, top holding, asset allocation, and callback-timeout schedule switches SHALL all be `false`

#### Scenario: Load dev application configuration
- **WHEN** the dev profile loads `application-dev.yml`
- **THEN** fund catalog, purchase status, period return, top holding, asset allocation, and callback-timeout schedule switches SHALL all be `false`

### Requirement: Existing refresh safeguards remain in force

server SHALL apply the existing batch-size validation, target selection, same-type non-terminal task skipping, task creation, and dispatch rules regardless of whether a refresh Case is invoked through cron or HTTP.

#### Scenario: Manual batch refresh uses invalid batch size
- **WHEN** a manual top-holding or asset-allocation refresh is invoked with a non-positive configured batch size
- **THEN** the corresponding Case SHALL skip task creation according to its existing validation rule

#### Scenario: Manual refresh has an active task
- **WHEN** a manual refresh is invoked while the same task type has a non-terminal task
- **THEN** the corresponding Case SHALL skip duplicate task creation according to its existing rule
