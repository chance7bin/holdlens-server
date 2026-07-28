## ADDED Requirements

### Requirement: Persist stock market unit semantics
server SHALL persist explicit currency and volume unit semantics for current stock market records.

#### Scenario: Persist A-share units
- **WHEN** server receives a valid A-share market refresh callback stock item
- **THEN** server SHALL upsert the `stock_market` row with `currency = CNY`
- **AND** server SHALL upsert the row with `volume_unit = LOT`

#### Scenario: Persist US stock units
- **WHEN** server receives a valid US stock market refresh callback stock item
- **THEN** server SHALL upsert the `stock_market` row with `currency = USD`
- **AND** server SHALL upsert the row with `volume_unit = SHARE`

#### Scenario: Default units for older callbacks
- **WHEN** a valid callback omits `currency` or `volume_unit`
- **THEN** server SHALL derive the missing unit fields from the business market
- **AND** server SHALL NOT reject the callback only because the unit fields are absent
