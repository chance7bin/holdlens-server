## ADDED Requirements

### Requirement: Create fund refresh task
server SHALL support creating a system-level asynchronous fund detail refresh task from a non-empty list of fund codes. The task MUST record task type, fund code count, task status, source type, source reference, created time, updated time, and diagnostic summary when available. The fund refresh task SHALL NOT be bound to a specific user.

#### Scenario: Create task with fund codes
- **WHEN** a background scheduler or system caller submits a valid fund code list
- **THEN** server SHALL create a `fund_detail_refresh` task in `created` status
- **AND** server SHALL deduplicate fund codes within the task

#### Scenario: Reject empty fund codes
- **WHEN** a caller submits an empty fund code list
- **THEN** server SHALL reject the request without creating a task

### Requirement: Dispatch fund refresh task to agent
server SHALL dispatch a created fund refresh task to agent through a domain port implemented by infrastructure. The dispatch request MUST include a schema version, server task id, fund code list, callback URL, and network authorization flag or equivalent authorization context.

#### Scenario: Dispatch accepted by agent
- **WHEN** agent accepts the task dispatch request
- **THEN** server SHALL move the task to `dispatched` or `running`
- **AND** server SHALL record the agent task reference when one is returned

#### Scenario: Dispatch fails
- **WHEN** server cannot dispatch the task to agent
- **THEN** server SHALL mark the task as `dispatch_failed`
- **AND** server SHALL preserve a diagnostic summary safe for logs and UI

### Requirement: Receive agent fund refresh callback
server SHALL expose an internal callback endpoint for agent fund refresh results. The callback MUST validate authentication, schema version, server task id, system task identity, idempotency key, and allowed task state before saving results.

#### Scenario: Accept valid callback
- **WHEN** agent sends a valid callback for a known running task
- **THEN** server SHALL accept the callback
- **AND** server SHALL persist the fund detail result according to `funddata` rules
- **AND** server SHALL update the task status according to callback result status

#### Scenario: Reject unauthorized callback
- **WHEN** callback authentication is missing or invalid
- **THEN** server SHALL reject the callback
- **AND** server SHALL NOT persist fund detail data from that callback

#### Scenario: Reject unsupported schema version
- **WHEN** callback schema version is unknown or unsupported
- **THEN** server SHALL reject the callback
- **AND** server SHALL record the task failure reason when the task can be identified

#### Scenario: Preserve callback failed status
- **WHEN** agent reports that all callback attempts failed for a task
- **THEN** server SHALL preserve a `callback_failed` diagnostic state when such status is received or observed
- **AND** server SHALL NOT poll agent for recovery in the first phase

#### Scenario: Handle duplicate callback idempotently
- **WHEN** agent sends a duplicate callback with the same task id and idempotency key
- **THEN** server SHALL NOT write duplicate snapshots or duplicate detail rows
- **AND** server SHALL NOT move a terminal task status backwards

### Requirement: Persist structured fund refresh result
server SHALL persist agent callback results as structured fund data instead of Markdown content. Persisted data MUST include snapshot metadata, fund detail items, top holdings, data sources, field sources or missing reasons when provided, and refresh warnings.

#### Scenario: Persist successful result
- **WHEN** callback status is `succeeded`
- **THEN** server SHALL save a fund detail snapshot and all successful fund details
- **AND** server SHALL mark the task as `succeeded`

#### Scenario: Persist partial result
- **WHEN** callback status is `partial_failed`
- **THEN** server SHALL save successful fund details
- **AND** server SHALL save warnings or failure summaries for failed funds
- **AND** server SHALL mark the task as `partial_failed`

#### Scenario: Preserve missing data reason
- **WHEN** a fund detail field is missing with a reason
- **THEN** server SHALL preserve the missing reason in structured storage

### Requirement: Query portfolio assets with fund details
server SHALL provide a query capability that returns account assets from `portfolio` and matching global latest fund details from `funddata` by fund code. Portfolio assets SHALL remain the primary facts. server SHALL first load the current user's portfolio holdings, extract held fund codes, and then query `funddata` for matching global latest fund details.

#### Scenario: Return asset with matching fund detail
- **WHEN** a user queries account assets and a held fund has a global latest fund detail
- **THEN** server SHALL return the asset holding and corresponding fund detail together

#### Scenario: Do not return unheld fund detail
- **WHEN** `funddata` contains a latest detail for a fund code that is not present in the current user's holdings
- **THEN** server SHALL NOT include that fund detail in the user's account asset result

#### Scenario: Return asset without fund detail
- **WHEN** a held fund has no latest fund detail
- **THEN** server SHALL still return the asset holding
- **AND** server SHALL represent fund detail as unavailable, missing, failed, or stale according to available task state

#### Scenario: Keep fund detail from overwriting holding
- **WHEN** fund detail exists for a fund code
- **THEN** server SHALL NOT use fund detail to overwrite account, asset amount, or holding facts

### Requirement: Exclude Markdown from server query chain
server SHALL NOT read agent-generated Markdown reports as a data source for fund detail refresh callback handling or portfolio fund detail queries.

#### Scenario: Query without Markdown dependency
- **WHEN** server returns portfolio assets with fund details
- **THEN** the result SHALL be based on server persisted portfolio and funddata records
- **AND** server SHALL NOT read `finance/基金明细跟踪.md` or equivalent Markdown reports

### Requirement: Protect privacy in agent dispatch
server SHALL minimize user financial data sent to agent. By default, server SHALL send only fund codes and technical system task context required for refresh.

#### Scenario: Dispatch minimal data
- **WHEN** server dispatches a fund refresh task to agent
- **THEN** the request SHALL NOT include account names, holding amounts, full portfolio composition, or personal asset details
- **AND** logs SHALL NOT print those details
