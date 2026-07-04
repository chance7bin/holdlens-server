## ADDED Requirements

### Requirement: Mark callback processing failures terminal
server SHALL mark a known agent refresh task as terminal when server-side callback processing fails after the callback task id and idempotency key have been validated.

#### Scenario: US stock callback persistence failure becomes callback failed
- **WHEN** server receives a valid `us_stock_market_refresh` callback with a supported schema version and idempotency key
- **AND** server fails while persisting returned market records, warnings, or task completion state
- **THEN** server SHALL mark the processing task as `callback_failed`
- **AND** server SHALL preserve a safe error summary when available
- **AND** the failed task SHALL NOT block creation of a later `us_stock_market_refresh` task

#### Scenario: A-share callback persistence failure becomes callback failed
- **WHEN** server receives a valid `a_share_market_refresh` callback with a supported schema version and idempotency key
- **AND** server fails while persisting returned market records, warnings, or task completion state
- **THEN** server SHALL mark the processing task as `callback_failed`
- **AND** server SHALL preserve a safe error summary when available

#### Scenario: Fund detail callback persistence failure becomes callback failed
- **WHEN** server receives a valid `fund_detail_refresh` callback with a supported schema version and idempotency key
- **AND** server fails while persisting returned fund data, quote targets, or task completion state
- **THEN** server SHALL mark the processing task as `callback_failed`
- **AND** server SHALL preserve a safe error summary when available

#### Scenario: Callback failure status survives business transaction rollback
- **WHEN** callback result persistence is rolled back
- **THEN** server SHALL persist `callback_failed` task state independently from the rolled-back business result transaction
- **AND** server SHOULD record the callback processing record as failed when the callback record table is available

#### Scenario: Unsupported schema still marks identified task failed
- **WHEN** server receives a callback for an existing refresh task
- **AND** the callback schema version is unsupported
- **THEN** server SHALL mark the task as `failed`
- **AND** server SHALL reject the callback with a safe business error
