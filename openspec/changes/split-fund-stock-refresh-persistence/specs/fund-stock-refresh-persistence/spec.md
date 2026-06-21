## ADDED Requirements

### Requirement: Persist current fund details
server SHALL persist fund refresh results as current fund details keyed by fund code instead of per-refresh snapshots.

#### Scenario: Upsert fund detail by fund code
- **WHEN** server receives a valid `fund-detail-refresh-result/v2` callback containing a successful fund detail
- **THEN** server SHALL upsert one current `fund_detail_item` record for that fund code
- **AND** server SHALL NOT create a `fund_detail_snapshot` record
- **AND** server SHALL NOT require `snapshot_id` to query the current fund detail

#### Scenario: Reject unsupported fund result schema
- **WHEN** server receives a fund refresh callback with an unsupported schema version
- **THEN** server SHALL reject the callback
- **AND** server SHALL NOT mutate fund detail or top holding records
- **AND** server SHALL preserve a safe task diagnostic when the task can be identified

### Requirement: Persist current fund top holdings
server SHALL persist fund top holdings as current fund-to-stock relationships keyed by fund code and rank.

#### Scenario: Upsert current top holdings
- **WHEN** server receives successful top holdings for a fund
- **THEN** server SHALL persist each holding with the fund code and rank number
- **AND** server SHALL enforce uniqueness by `fund_code + rank_no`
- **AND** server SHALL NOT persist `fund_detail_item_id`, `snapshot_id`, `daily_return`, or `missing_reasons_json` for the holding

#### Scenario: Replace stale top holding ranks
- **WHEN** a later fund refresh returns fewer or different top holding ranks for a fund
- **THEN** server SHALL ensure old current ranks not present in the latest result are not returned as current holdings

### Requirement: Persist current stock quotes
server SHALL persist stock quote refresh results as current stock quotes keyed by stock code and market.

#### Scenario: Upsert stock quote by stock code and market
- **WHEN** server receives a valid `stock-quote-refresh-result/v1` callback containing a stock quote
- **THEN** server SHALL upsert one `stock_market_current` record for that stock code and market
- **AND** server SHALL preserve trade date, daily return, quote time, stock name, create time, and update time

#### Scenario: Handle partial stock quote refresh
- **WHEN** stock quote refresh partially succeeds
- **THEN** server SHALL persist successful quote records
- **AND** server SHALL record safe warnings or error summaries for failed quotes in `processing_log`
- **AND** server SHALL mark the task as `partial_failed`

### Requirement: Split fund and stock refresh tasks
server SHALL support separate processing tasks for fund detail refresh and stock quote refresh.

#### Scenario: Create fund detail refresh task
- **WHEN** a system caller submits a non-empty fund code list
- **THEN** server SHALL create a `fund_detail_refresh` processing task
- **AND** server SHALL dispatch a fund detail refresh request to agent without stock quote fields

#### Scenario: Create stock quote refresh task from current quote table
- **WHEN** server needs to refresh stock quotes
- **THEN** server SHALL derive stock code and market pairs from all current `stock_market_current` records
- **AND** server SHALL create a `stock_quote_refresh` processing task for those pairs
- **AND** server SHALL reject or skip creation when `stock_market_current` contains no stock code and market pair

### Requirement: Query portfolio fund details with stock quotes
server SHALL compose portfolio holdings with current fund details, current fund top holdings, and current stock quotes.

#### Scenario: Return fund holding with top holding quote
- **WHEN** a user queries portfolio fund details and a held fund has current top holdings with matching stock quotes
- **THEN** server SHALL return the portfolio holding as the primary fact
- **AND** server SHALL include current fund detail and top holding data
- **AND** server SHALL attach stock quote data from `stock_market_current`

#### Scenario: Do not overwrite portfolio facts
- **WHEN** fund detail, top holding, or stock quote data exists
- **THEN** server SHALL NOT use those records to overwrite account, asset amount, holding source, or current holding facts

#### Scenario: Return holdings when public data is missing
- **WHEN** fund detail or stock quote data is missing
- **THEN** server SHALL still return the portfolio holding
- **AND** server SHALL represent the missing public data without reading agent Markdown files

### Requirement: Preserve refresh diagnostics in processing logs
server SHALL use processing logs for refresh diagnostics instead of storing source or missing-reason JSON on fund business tables.

#### Scenario: Record fund refresh warning
- **WHEN** fund refresh reports missing public holdings, provider failure, or field omission
- **THEN** server SHALL record a safe diagnostic in `processing_log`
- **AND** server SHALL NOT persist `field_sources_json` or `missing_reasons_json` on `fund_detail_item`

#### Scenario: Record stock quote refresh warning
- **WHEN** stock quote refresh reports missing quote, unsupported market, or provider failure
- **THEN** server SHALL record a safe diagnostic in `processing_log`
- **AND** diagnostic content SHALL NOT include account names, holding amounts, tokens, cookies, API keys, or callback credentials
