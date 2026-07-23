## ADDED Requirements

### Requirement: 当前汇率使用通用币种对结构

系统 SHALL 使用 `base_currency + quote_currency` 唯一标识一个当前汇率。当前业务 SHALL 只保存外币作为基准币、人民币作为报价币的最新汇率。

#### Scenario: 保存美元兑人民币汇率

- **WHEN** 系统保存 `base_currency = USD`、`quote_currency = CNY`、正数汇率
- **THEN** 系统 SHALL 创建或更新该币种对的最新汇率

#### Scenario: 尝试保存外币交叉汇率

- **WHEN** 调用方尝试保存 `USD → HKD` 汇率
- **THEN** 系统 SHALL 拒绝该输入

#### Scenario: 尝试保存人民币作为基准币

- **WHEN** 调用方尝试保存 `CNY → USD` 汇率
- **THEN** 系统 SHALL 拒绝该输入

### Requirement: 资产汇总按查询目标币种换算

系统 SHALL 按查询指定的目标币种汇总活跃资产记录，未指定时 SHALL 默认使用 CNY。目标币种 MUST NOT 保存为用户属性，也 MUST NOT 改写资产记录原币金额。

#### Scenario: 外币汇总到人民币

- **WHEN** 活跃记录为 100 USD，最新 `USD → CNY` 汇率为 7.2，目标币种为 CNY
- **THEN** 系统 SHALL 使用 720 CNY 参与汇总

#### Scenario: 人民币汇总到美元

- **WHEN** 活跃记录为 720 CNY，最新 `USD → CNY` 汇率为 7.2，目标币种为 USD
- **THEN** 系统 SHALL 使用 100 USD 参与汇总

#### Scenario: 两种外币经人民币换算

- **WHEN** 原币和目标币均不是 CNY 且两者兑人民币汇率都存在
- **THEN** 系统 SHALL 先将原币换算为 CNY，再换算为目标币

### Requirement: 汇率缺失返回部分汇总

系统 SHALL 在部分活跃资产缺少必要汇率时返回部分汇总，并 SHALL 明确列出无法换算的币种和原币金额。系统 MUST NOT 把无法换算的资产按零处理。

#### Scenario: 部分外币缺少汇率

- **WHEN** 用户同时持有可换算 CNY 资产和缺少汇率的外币资产
- **THEN** 系统 SHALL 汇总可换算资产
- **AND** 系统 SHALL 标记结果为部分汇总
- **AND** 系统 SHALL 返回缺失币种及未换算原币金额

### Requirement: 金额方向决定净资产

系统 SHALL 使用资产目录金额方向计算当前资产、负债和净资产。只有 `ACTIVE` 资产记录 SHALL 参与当前汇总。

#### Scenario: 计算加项和减项

- **WHEN** 换算后加项目录总额为 100000，减项目录总额为 30000
- **THEN** 系统 SHALL 返回资产总额 100000、负债总额 30000 和净资产 70000

#### Scenario: 归档和删除记录不参与汇总

- **WHEN** 用户存在 `ARCHIVED` 或 `DELETED` 资产记录
- **THEN** 系统 MUST NOT 将这些记录计入当前资产、负债或净资产
