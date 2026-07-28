## MODIFIED Requirements

### Requirement: Split fund and stock refresh tasks
server SHALL support separate processing tasks for fund detail refresh and stock quote refresh.

#### Scenario: Create fund detail refresh task
- **WHEN** a system caller submits a non-empty fund code list
- **THEN** server SHALL create a `fund_detail_refresh` processing task
- **AND** server SHALL dispatch a fund detail refresh request to agent without stock quote fields

#### Scenario: Register fund top holding stocks after fund callback
- **WHEN** server receives a valid `fund-detail-refresh-result/v2` callback with status `succeeded` or `partial_failed`
- **AND** the callback contains top holdings with non-empty stock code
- **THEN** server SHALL register those stock codes in `stock_market_current`
- **AND** server SHALL preserve the provided market when market is present
- **AND** server SHALL allow the registered `stock_market_current` record to have null market when market is missing
- **AND** server SHALL update stock name to the latest provided value, including null when the latest callback lacks stock name
- **AND** server SHALL preserve existing quote fields such as trade date, daily return, and quote time
- **AND** server SHALL NOT register top holdings that lack stock code

#### Scenario: Keep null-market stock registrations idempotent
- **WHEN** server registers the same top holding stock code with missing market more than once
- **THEN** server SHALL keep a single current `stock_market_current` target for that stock code and missing market state
- **AND** server SHALL NOT create duplicate rows only because market is null

#### Scenario: Create stock quote refresh task from current quote table
- **WHEN** server needs to refresh stock quotes
- **THEN** server SHALL derive stock quote targets from current `stock_market_current` records whose stock code is non-empty
- **AND** server SHALL create a `stock_quote_refresh` processing task for those targets
- **AND** server SHALL include `stock_market_current` records with null market in the stock quote refresh request
- **AND** server SHALL send null market as `market: null` for those targets
- **AND** server SHALL reject or skip creation when `stock_market_current` contains no non-empty stock code
