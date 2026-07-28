## ADDED Requirements

### Requirement: Create A-share market refresh task
server SHALL expose a manual HTTP endpoint to create an A-share full market refresh task and dispatch it to agent.

#### Scenario: Create manual A-share market refresh task
- **WHEN** a system caller submits `POST /api/agent/a-share-market-refresh/tasks`
- **THEN** server SHALL create a `processing_task` with task type `a_share_market_refresh`
- **AND** server SHALL dispatch an `a-share-market-refresh-task/v1` request to agent

#### Scenario: Reject concurrent A-share market refresh task
- **WHEN** a non-terminal `a_share_market_refresh` task already exists
- **AND** a system caller submits `POST /api/agent/a-share-market-refresh/tasks`
- **THEN** server SHALL NOT create an additional A-share market refresh task
- **AND** server SHALL return a safe business response that explains the task is already running

### Requirement: Receive A-share market refresh callback
server SHALL receive `a-share-market-refresh-result/v1` callbacks from agent and update processing task state idempotently.

#### Scenario: Accept successful callback
- **WHEN** server receives a valid callback for an existing `a_share_market_refresh` task
- **AND** callback status is `succeeded`
- **THEN** server SHALL save the callback idempotency record
- **AND** server SHALL upsert returned stocks into `stock_market`
- **AND** server SHALL mark the processing task as `succeeded`

#### Scenario: Handle partial callback
- **WHEN** server receives a valid callback with status `partial_failed`
- **THEN** server SHALL upsert successful stock records
- **AND** server SHALL save safe warnings in `processing_log`
- **AND** server SHALL mark the processing task as `partial_failed`

#### Scenario: Reject unsupported callback schema
- **WHEN** server receives a callback whose `schema_version` is not `a-share-market-refresh-result/v1`
- **THEN** server SHALL mark the processing task as `failed`
- **AND** server SHALL reject the callback with a safe business error

#### Scenario: Ignore duplicate callback
- **WHEN** server receives the same `server_task_id` and `idempotency_key` more than once
- **THEN** server SHALL return the current task state
- **AND** server SHALL NOT upsert stock records again

### Requirement: Persist current A-share market records
server SHALL persist A-share current market records in `stock_market` keyed by stock code and business market.

#### Scenario: Upsert A-share stock by stock code and market
- **WHEN** a callback stock item contains `stock_code = 600000` and `market = A_SHARE`
- **THEN** server SHALL upsert one `stock_market` row keyed by `stock_code + market`
- **AND** server SHALL set `status = active`
- **AND** server SHALL persist `refreshed_at` from the callback item or generated result time

#### Scenario: Preserve numeric semantics
- **WHEN** a callback stock item contains numeric fields as string values
- **THEN** server SHALL parse price fields as decimal values
- **AND** server SHALL parse percent and ratio fields as decimal values in percentage-point units
- **AND** server SHALL parse `volume` as an integer value
- **AND** server SHALL persist missing or unparsable values as NULL with safe diagnostics

#### Scenario: Do not mark missing stocks in first version
- **WHEN** an existing `stock_market` row is not present in the latest valid callback
- **THEN** server SHALL NOT delete that row
- **AND** server SHALL NOT change its status in the first version

### Requirement: Replace legacy stock quote refresh server contract
server SHALL remove or disable the legacy server-side `stock_quote_refresh` cross-project contract.

#### Scenario: Legacy stock quote task is not exposed
- **WHEN** implementation for this change is complete
- **THEN** server SHALL NOT expose the legacy stock quote task creation behavior as the active stock refresh path
- **AND** server SHALL NOT dispatch `/tasks/stock-quote-refresh` for scheduled or manual stock market refresh

#### Scenario: Legacy callback is not active
- **WHEN** implementation for this change is complete
- **THEN** server SHALL NOT depend on `/internal/agent/stock-quote-refresh/callback` to update current A-share market data

### Requirement: Query stock market data with new field language
server SHALL expose stock market quote fields using the new A-share market field language.

#### Scenario: Return change percent instead of daily return
- **WHEN** a query composes fund top holdings or stock market data with current stock market records
- **THEN** server SHALL return `changePercent`
- **AND** server SHALL NOT return the legacy `dailyReturn` field for this refreshed stock market data

#### Scenario: Return refreshed time instead of quote time
- **WHEN** a query exposes the refresh timestamp for current stock market records
- **THEN** server SHALL return `refreshedAt`
- **AND** server SHALL NOT return the legacy `quoteTime` field for this refreshed stock market data
