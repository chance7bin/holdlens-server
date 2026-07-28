## Tasks

- [x] 1. 更新 OpenSpec proposal、design、spec，明确 `fund` 不再引用资产主数据。
- [x] 2. 将 `fund_detail_item` DDL 重命名为 `fund`，并删除 `fund_asset_id`。
- [x] 3. 将 `FundDetailItemPO`、`IFundDetailItemDao`、MyBatis Mapper 和 Repository 转换收敛为 `FundPO`、`IFundDao` 和 `fund` 表。
- [x] 4. 更新领域边界 ADR 中关于 `fund_asset_id` 的过期描述。
- [x] 5. 检查 `fund` 其他跨业务使用点并记录结论。
- [x] 6. 运行 `openspec validate --strict decouple-fund-detail-item-from-asset`。
- [x] 7. 运行相关 Maven 测试。
