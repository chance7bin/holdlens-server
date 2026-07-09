## ADDED Requirements

### Requirement: Fund table shall not reference user assets
server SHALL keep `fund` focused on current public fund facts and SHALL NOT persist a direct user asset or portfolio asset reference on that table.

#### Scenario: Persist current fund detail without asset reference
- **WHEN** server receives a valid fund detail refresh result for a fund code
- **THEN** server SHALL upsert the current `fund` record by `fund_code`
- **AND** server SHALL NOT persist `fund_asset_id` or equivalent portfolio asset reference on `fund`

#### Scenario: Compose portfolio with public fund detail by fund code
- **WHEN** server queries portfolio fund details
- **THEN** server MAY read matching `fund` records by held fund code
- **AND** server SHALL NOT use `fund` to overwrite account, asset, amount, or holding facts
