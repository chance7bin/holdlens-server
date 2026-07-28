## ADDED Requirements

### Requirement: Schedule fund detail refresh batches

server SHALL support a configurable scheduled trigger that scans current `fund_detail_item` records and creates batched fund detail refresh tasks.

#### Scenario: Scheduled fund refresh scans fund detail items
- **WHEN** the fund detail refresh schedule is enabled and triggered
- **THEN** server SHALL scan `fund_detail_item` records whose `fund_code` is non-empty
- **AND** server SHALL use `id > lastId` keyset pagination ordered by `id ASC`
- **AND** server SHALL create one `fund_detail_refresh` processing task per non-empty page
- **AND** each task SHALL contain only the fund codes from that page

#### Scenario: Scheduled fund refresh is disabled
- **WHEN** the fund detail refresh schedule is disabled
- **THEN** server SHALL NOT scan `fund_detail_item`
- **AND** server SHALL NOT create a scheduled fund detail refresh task

#### Scenario: Scheduled fund refresh skips when same type task is active
- **WHEN** the fund detail refresh schedule is triggered
- **AND** a non-terminal `fund_detail_refresh` processing task already exists
- **THEN** server SHALL skip this scheduled run
- **AND** server SHALL NOT create additional fund detail refresh tasks in that run

### Requirement: Schedule stock quote refresh batches

server SHALL support a configurable scheduled trigger that scans current `stock_market_current` records and creates batched stock quote refresh tasks.

#### Scenario: Scheduled stock refresh scans stock market current
- **WHEN** the stock quote refresh schedule is enabled and triggered
- **THEN** server SHALL scan `stock_market_current` records whose `stock_code` and `market` are both non-empty
- **AND** server SHALL use `id > lastId` keyset pagination ordered by `id ASC`
- **AND** server SHALL create one `stock_quote_refresh` processing task per non-empty page
- **AND** each task SHALL contain only the stock code and market pairs from that page
- **AND** server SHALL NOT send `market: null` stock targets to agent

#### Scenario: Scheduled stock refresh is disabled
- **WHEN** the stock quote refresh schedule is disabled
- **THEN** server SHALL NOT scan `stock_market_current`
- **AND** server SHALL NOT create a scheduled stock quote refresh task

#### Scenario: Scheduled stock refresh skips when same type task is active
- **WHEN** the stock quote refresh schedule is triggered
- **AND** a non-terminal `stock_quote_refresh` processing task already exists
- **THEN** server SHALL skip this scheduled run
- **AND** server SHALL NOT create additional stock quote refresh tasks in that run

### Requirement: Stop scheduled run on batch creation failure

server SHALL stop the current scheduled run when a batch cannot be created or dispatched successfully.

#### Scenario: Stop when case throws
- **WHEN** a scheduled refresh run creates a batch
- **AND** the task creation use case throws an exception
- **THEN** server SHALL stop the current scheduled run
- **AND** server SHALL NOT continue scanning later pages in that run

#### Scenario: Stop when returned task status is abnormal
- **WHEN** a scheduled refresh run creates a batch
- **AND** the returned task status is not `created`, `running`, or `dispatched`
- **THEN** server SHALL stop the current scheduled run
- **AND** server SHALL NOT roll back previously created batch tasks

### Requirement: Configure scheduled refresh

server SHALL expose separate configuration for fund detail refresh schedule and stock quote refresh schedule.

#### Scenario: Configure schedule independently
- **WHEN** the application starts
- **THEN** server SHALL read separate enabled, cron, and batch-size settings for fund detail refresh scheduling
- **AND** server SHALL read separate enabled, cron, and batch-size settings for stock quote refresh scheduling
- **AND** both schedules SHALL be disabled by default

### Requirement: Create manual stock quote refresh task from list

server SHALL support creating a manual stock quote refresh task from an explicit non-empty list of stock code and market pairs.

#### Scenario: Create manual stock refresh task with stocks
- **WHEN** a system caller submits a non-empty stock list whose items contain non-empty `stockCode` and `market`
- **THEN** server SHALL deduplicate stocks by `stockCode + market`
- **AND** server SHALL create one `stock_quote_refresh` processing task for the deduplicated list
- **AND** server SHALL dispatch the stock quote refresh request to agent

#### Scenario: Reject manual stock refresh without valid stocks
- **WHEN** a system caller submits an empty stock list or a list whose items lack `stockCode` or `market`
- **THEN** server SHALL reject the request
- **AND** server SHALL NOT create a `stock_quote_refresh` processing task

#### Scenario: Manual stock refresh does not require existing stock row
- **WHEN** a system caller submits a valid stock code and market pair that is not currently present in `stock_market_current`
- **THEN** server SHALL still allow creating the stock quote refresh task
- **AND** successful callback persistence SHALL use the existing stock quote upsert behavior

### Requirement: Preserve manual fund refresh behavior

server SHALL keep the existing manual fund detail refresh behavior for explicit fund code lists.

#### Scenario: Manual fund refresh still accepts fund codes
- **WHEN** a system caller submits a non-empty fund code list to the manual fund detail refresh endpoint
- **THEN** server SHALL create a `fund_detail_refresh` processing task
- **AND** server SHALL dispatch a fund detail refresh request to agent
