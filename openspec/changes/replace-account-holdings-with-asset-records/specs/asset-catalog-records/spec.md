## ADDED Requirements

### Requirement: 系统与用户资产目录

系统 SHALL 提供最多两级的资产目录。系统目录 SHALL 对所有用户可见且不可由用户修改、停用或删除；用户目录 SHALL 只对所属用户可见和可维护。

#### Scenario: 初始化投资资产系统分组

- **WHEN** 系统初始化资产目录
- **THEN** 系统 SHALL 创建编码为 `INVESTMENT_ASSET`、方向为 `ADD` 的一级系统分组“投资资产”
- **AND** 系统 SHALL 在其下创建编码为 `FUND` 和 `STOCK`、方向为 `ADD` 的二级系统叶子目录
- **AND** “投资资产”分组 MUST NOT 直接创建资产记录

#### Scenario: 用户创建私有目录

- **WHEN** 用户在允许的父目录下创建名称、金额方向合法的用户目录
- **THEN** 系统 SHALL 创建只属于该用户的目录
- **AND** 该目录 SHALL 不设置系统业务编码
- **AND** 其他用户 MUST NOT 查询或使用该目录

#### Scenario: 用户不能修改系统目录

- **WHEN** 用户尝试改名、移动、停用或删除系统目录
- **THEN** 系统 SHALL 拒绝该操作

#### Scenario: 系统叶子目录不能增加用户子目录

- **WHEN** 用户尝试在系统叶子目录下创建用户目录
- **THEN** 系统 SHALL 拒绝该操作

#### Scenario: 用户目录校验后软删除

- **WHEN** 用户删除没有子目录且没有活跃资产记录的自有目录
- **THEN** 系统 SHALL 将目录标记为删除
- **AND** 系统 MUST NOT 物理删除目录

### Requirement: 只有启用叶子目录可记录资产

系统 SHALL 只允许在用户可见、启用且没有子目录的资产目录中创建资产记录。父子目录 SHALL 使用相同金额方向。

#### Scenario: 在启用叶子目录创建记录

- **WHEN** 用户选择可见的启用叶子目录创建合法资产记录
- **THEN** 系统 SHALL 接受该目录归属

#### Scenario: 在分组目录创建记录

- **WHEN** 用户尝试在存在子目录的分组目录创建资产记录
- **THEN** 系统 SHALL 拒绝该请求

#### Scenario: 父子目录金额方向不一致

- **WHEN** 用户创建或移动目录导致父子 `balance_direction` 不一致
- **THEN** 系统 SHALL 拒绝该操作

### Requirement: 统一资产记录状态

系统 SHALL 使用 `asset_kind + asset_id` 区分普通金额、未细分基金/股票金额和具体基金/股票持仓。资产记录 SHALL 只以自身 ID 唯一，并 SHALL 允许同一用户保存多条相同具体标的记录。

#### Scenario: 创建普通金额记录

- **WHEN** 用户在普通系统目录或用户目录创建金额记录
- **THEN** 系统 SHALL 保存空的 `asset_kind` 和 `asset_id`

#### Scenario: 创建未细分基金金额

- **WHEN** 用户在基金系统目录只填写金额而未选择具体基金
- **THEN** 系统 SHALL 保存 `asset_kind = FUND` 和空 `asset_id`
- **AND** 系统 SHALL 生成默认记录名称

#### Scenario: 创建具体股票持仓

- **WHEN** 用户在股票系统目录选择已存在公共股票并填写金额
- **THEN** 系统 SHALL 保存 `asset_kind = STOCK` 和对应 `stock_market.id`
- **AND** 系统 SHALL 从公共股票复制创建时名称快照

#### Scenario: 相同标的保留多条记录

- **WHEN** 用户再次创建同一基金或股票的合法资产记录
- **THEN** 系统 SHALL 创建新的独立记录
- **AND** 系统 MUST NOT 自动合并既有记录

### Requirement: 资产记录不可变归属和计量上下文

系统 SHALL 在资产记录创建后保持 `catalog_id`、`asset_kind`、`asset_id` 和 `currency` 不变。当前系统 MUST NOT 提供资产跨目录移动或标的、币种替换能力。

#### Scenario: 尝试修改资产目录

- **WHEN** 用户尝试修改既有资产记录的 `catalog_id`
- **THEN** 系统 SHALL 拒绝该请求

#### Scenario: 尝试替换具体标的

- **WHEN** 用户尝试把既有具体基金持仓改成另一只基金
- **THEN** 系统 SHALL 拒绝该请求

#### Scenario: 尝试修改币种

- **WHEN** 用户尝试修改既有资产记录币种
- **THEN** 系统 SHALL 拒绝该请求

### Requirement: 用户记录金额是权威事实

系统 SHALL 使用非负原币金额作为资产记录当前金额。公共行情、数量、成本和汇率 MUST NOT 自动覆盖该金额；金额为零 MUST NOT 自动归档记录。

#### Scenario: 活跃记录金额变为零

- **WHEN** 用户把活跃普通金额记录从正数更新为零
- **THEN** 系统 SHALL 保持该记录为 `ACTIVE`

#### Scenario: 公共行情变化

- **WHEN** 具体基金或股票的公共行情发生变化
- **THEN** 系统 MUST NOT 自动修改用户资产记录金额

### Requirement: 未细分金额拆分

系统 SHALL 允许用户从一条活跃未细分基金或股票金额中，一次拆出一条同类型具体持仓。拆分 MUST 保持同目录、同币种金额守恒，并 MUST 在一个事务内完成。

#### Scenario: 部分拆分未细分基金

- **WHEN** 用户从 100000 CNY 的未细分基金中拆出 30000 CNY 到一只有效基金
- **THEN** 系统 SHALL 创建 30000 CNY 的具体基金持仓
- **AND** 系统 SHALL 将源记录更新为 70000 CNY 且保持 `ACTIVE`
- **AND** 新记录 SHALL 继承源目录、类型和币种

#### Scenario: 全部拆分未细分股票

- **WHEN** 用户把未细分股票的全部金额拆到一只有效股票
- **THEN** 系统 SHALL 创建等额具体股票持仓
- **AND** 系统 SHALL 将源金额更新为零并标记为 `ARCHIVED`

#### Scenario: 普通金额不能拆分

- **WHEN** 用户尝试拆分 `asset_kind` 为空的普通金额记录
- **THEN** 系统 SHALL 拒绝该请求

#### Scenario: 并发超额拆分

- **WHEN** 并发拆分导致源记录剩余金额不足
- **THEN** 系统 SHALL 至多提交一个满足金额约束的拆分
- **AND** 其他拆分 SHALL 失败且不得产生部分写入

### Requirement: 资产变更历史只追加

系统 SHALL 为资产创建、金额更新、拆分、归档、恢复和删除追加不可变变更历史。系统 MUST NOT 通过修改或删除既有历史纠错，也 MUST NOT 依赖历史重放计算当前资产。

#### Scenario: 拆分记录同一操作

- **WHEN** 系统成功拆分未细分金额
- **THEN** 系统 SHALL 写入一条 `SPLIT_OUT` 和一条 `SPLIT_IN`
- **AND** 两条历史 SHALL 使用相同 `operation_id`

#### Scenario: 修改名称或备注

- **WHEN** 用户修改允许编辑的记录名称或备注
- **THEN** 系统 SHALL 更新当前记录
- **AND** 系统 MUST NOT 写入资产变更历史
