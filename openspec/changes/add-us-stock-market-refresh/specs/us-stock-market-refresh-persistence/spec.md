## ADDED Requirements

### Requirement: Create US stock market refresh task
server SHALL expose a manual HTTP endpoint to create a US stock full market refresh task and dispatch it to agent.

#### Scenario: Create manual US stock market refresh task
- **WHEN** a system caller submits `POST /api/agent/us-stock-market-refresh/tasks`
- **THEN** server SHALL create a `processing_task` with task type `us_stock_market_refresh`
- **AND** server SHALL dispatch a `us-stock-market-refresh-task/v1` request to agent

#### Scenario: Reject concurrent US stock market refresh task
- **WHEN** a non-terminal `us_stock_market_refresh` task already exists
- **AND** a system caller submits `POST /api/agent/us-stock-market-refresh/tasks`
- **THEN** server SHALL NOT create an additional US stock market refresh task
- **AND** server SHALL return a safe business response that explains the task is already running

### Requirement: Receive US stock market refresh callback
server SHALL receive `us-stock-market-refresh-result/v1` callbacks from agent and update processing task state idempotently.

#### Scenario: Accept successful callback
- **WHEN** server receives a valid callback for an existing `us_stock_market_refresh` task
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
- **WHEN** server receives a callback whose `schema_version` is not `us-stock-market-refresh-result/v1`
- **THEN** server SHALL mark the processing task as `failed`
- **AND** server SHALL reject the callback with a safe business error

#### Scenario: Ignore duplicate callback
- **WHEN** server receives the same `server_task_id` and `idempotency_key` more than once
- **THEN** server SHALL return the current task state
- **AND** server SHALL NOT upsert stock records again

### Requirement: Persist current US stock market records
server SHALL persist US stock current market records in `stock_market` keyed by stock code and business market.

#### Scenario: Upsert US stock by stock code and market
- **WHEN** a callback stock item contains `stock_code = NVDA` and `market = US_STOCK`
- **THEN** server SHALL upsert one `stock_market` row keyed by `stock_code + market`
- **AND** server SHALL set `status = active`
- **AND** server SHALL persist `provider_market_code` when provided
- **AND** server SHALL persist `refreshed_at` from the callback item or generated result time

#### Scenario: Preserve US stock field semantics
- **WHEN** a callback stock item contains `pe_ratio`
- **THEN** server SHALL persist it as US stock PE ratio
- **AND** server SHALL NOT store it in the A-share `pe_dynamic` field

#### Scenario: Preserve numeric semantics
- **WHEN** a callback stock item contains numeric fields as string values
- **THEN** server SHALL parse price fields as decimal values
- **AND** server SHALL parse percent and ratio fields as decimal values in percentage-point units
- **AND** server SHALL parse `volume` as an integer value
- **AND** server SHALL persist missing or unparsable values as NULL with safe diagnostics

#### Scenario: Do not mark missing US stocks in first version
- **WHEN** an existing `stock_market` row is not present in the latest valid callback
- **THEN** server SHALL NOT delete that row
- **AND** server SHALL NOT change its status in the first version

### Requirement: Keep A-share refresh contract unchanged
server SHALL keep the active A-share full market refresh contract independent from the US stock refresh contract.

#### Scenario: A-share refresh remains independently callable
- **WHEN** implementation for this change is complete
- **THEN** `POST /api/agent/a-share-market-refresh/tasks` SHALL continue to create `a_share_market_refresh`
- **AND** it SHALL NOT require a market parameter

#### Scenario: US stock refresh does not use legacy stock quote refresh
- **WHEN** implementation for this change is complete
- **THEN** server SHALL NOT dispatch `/tasks/stock-quote-refresh` for US stock full market refresh
