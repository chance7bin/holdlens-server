## ADDED Requirements

### Requirement: 差量同步基金当前重仓

server SHALL 以 `fund_code + rank_no` 作为基金当前重仓身份，并将每次有效基金刷新结果差量同步为该基金唯一的当前重仓集合。

#### Scenario: 更新已有排名
- **WHEN** 有效基金刷新结果包含数据库中已经存在的基金代码和排名
- **THEN** server SHALL 更新该排名的最新重仓字段
- **AND** server SHALL 保留该排名记录原有 ID
- **AND** server MUST NOT 通过新增记录完成该排名的更新

#### Scenario: 插入新增排名
- **WHEN** 有效基金刷新结果包含数据库中尚不存在的基金代码和排名
- **THEN** server SHALL 为该排名插入一条新的当前重仓记录

#### Scenario: 删除失效排名
- **WHEN** 数据库中的某个基金排名未出现在该基金本次有效刷新结果中
- **THEN** server SHALL 删除该排名记录
- **AND** server SHALL NOT 保留空槽或将其返回为当前重仓

#### Scenario: 清空基金当前重仓
- **WHEN** 有效基金刷新结果包含某基金且该基金的重仓集合为空
- **THEN** server SHALL 删除该基金已有的全部当前重仓

#### Scenario: 重复排名最后一条生效
- **WHEN** 同一基金的一次有效刷新结果中多次出现相同排名
- **THEN** server SHALL 只保存一条该基金与排名的当前重仓记录
- **AND** server SHALL 使用输入顺序中最后一条该排名数据作为最终内容

### Requirement: 基金重仓同步保持事务原子性

server SHALL 在现有基金回调事务内完成基金当前重仓的更新、插入和删除。

#### Scenario: 差量同步成功
- **WHEN** 某基金当前重仓的全部更新、插入和删除均成功
- **THEN** server SHALL 原子提交该基金的最新当前重仓集合

#### Scenario: 差量同步失败
- **WHEN** 某基金当前重仓的任一更新、插入或删除失败
- **THEN** server SHALL 回滚本次回调事务中的基金及重仓变更
- **AND** server SHALL NOT 暴露部分同步结果
