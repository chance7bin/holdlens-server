## MODIFIED Requirements

### Requirement: Split fund and stock refresh tasks
server SHALL support separate processing tasks for fund detail refresh and stock quote refresh.

#### Scenario: Create fund detail refresh task
- **WHEN** a system caller submits a non-empty fund code list
- **THEN** server SHALL create a `fund_detail_refresh` processing task
- **AND** server SHALL dispatch a fund detail refresh request to agent without stock quote fields

#### Scenario: Register fund top holding stocks after fund callback
- **WHEN** server receives a valid `fund-detail-refresh-result/v2` callback with status `succeeded` or `partial_failed`
- **AND** the callback contains top holdings with non-empty stock code and market
- **THEN** server SHALL register those stock code and market pairs in `stock_market_current`
- **AND** server SHALL preserve existing quote fields such as trade date, daily return, and quote time
- **AND** server SHALL NOT register top holdings that lack stock code or market

#### Scenario: Create stock quote refresh task from current quote table
- **WHEN** server needs to refresh stock quotes
- **THEN** server SHALL derive stock code and market pairs from all current `stock_market_current` records
- **AND** server SHALL create a `stock_quote_refresh` processing task for those pairs
- **AND** server SHALL reject or skip creation when `stock_market_current` contains no stock code and market pair
