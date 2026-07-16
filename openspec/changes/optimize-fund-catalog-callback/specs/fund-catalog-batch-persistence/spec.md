## ADDED Requirements

### Requirement: Batch persist fund catalog callback
server SHALL persist valid fund catalog callback items through batch upsert operations containing at most 500 funds each.

#### Scenario: Persist a full batch
- **WHEN** a valid fund catalog callback contains 500 writable funds
- **THEN** server SHALL issue one batch catalog upsert containing all 500 funds

#### Scenario: Persist multiple batches and remainder
- **WHEN** a valid fund catalog callback contains 1001 writable funds
- **THEN** server SHALL issue three ordered batch catalog upserts containing 500, 500 and 1 fund respectively

#### Scenario: Exclude invalid rows before batching
- **WHEN** a fund catalog callback contains rows without a usable fund code or fund name
- **THEN** server SHALL exclude those rows from batch upsert
- **AND** server SHALL retain the existing safe diagnostic and partial-failure status semantics

### Requirement: Preserve atomic callback confirmation
server MUST keep all fund catalog batches, callback idempotency state, diagnostics and processing task status in the existing callback transaction and SHALL confirm delivery only after that transaction completes.

#### Scenario: Complete all batches successfully
- **WHEN** every fund catalog batch upsert and callback state update succeeds
- **THEN** server SHALL commit the callback transaction before returning a 2xx response

#### Scenario: Roll back a failed batch
- **WHEN** any fund catalog batch upsert fails
- **THEN** server SHALL roll back all catalog writes and callback state changes from that attempt
- **AND** server SHALL return a non-2xx response so agent can retry the same idempotent callback

#### Scenario: Ignore duplicate completed callback
- **WHEN** server receives the same callback idempotency key after its task is terminal
- **THEN** server SHALL return the existing task result without writing another catalog batch

### Requirement: Emit safe catalog callback diagnostics
server SHALL emit bounded diagnostic logs for fund catalog callback processing without logging individual fund records, callback payloads, credentials or authentication headers.

#### Scenario: Observe callback processing
- **WHEN** server starts processing a valid fund catalog callback
- **THEN** server SHALL log the server task identifier and input item count at INFO level
- **AND** each executed persistence batch SHALL log its batch index, batch size and elapsed time at DEBUG level

#### Scenario: Observe committed callback result
- **WHEN** the fund catalog callback transaction commits successfully
- **THEN** server SHALL log input, valid, skipped and batch counts, elapsed time, deduplication state and final task status at INFO level
- **AND** the completion log SHALL be emitted only after the transaction boundary returns successfully

#### Scenario: Observe callback failure
- **WHEN** a catalog batch or callback transaction fails
- **THEN** server SHALL log bounded task, batch and timing context without individual fund data
- **AND** server SHALL rethrow the failure so the existing global exception handler returns a non-2xx response and records the exception stack
