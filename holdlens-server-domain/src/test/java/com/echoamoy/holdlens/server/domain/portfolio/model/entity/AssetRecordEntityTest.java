package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class AssetRecordEntityTest {

    @Test
    public void zeroAmountUpdateKeepsActiveRecord() {
        AssetRecordEntity record = AssetRecordEntity.create(1L, 2L, "备用现金", null, null,
                new BigDecimal("100"), "cny", null);

        record.updateAmount(BigDecimal.ZERO);

        Assert.assertEquals(BigDecimal.ZERO, record.getAmount());
        Assert.assertEquals(AssetRecordEntity.STATUS_ACTIVE, record.getStatus());
    }

    @Test
    public void fullSplitArchivesUnspecifiedInvestment() {
        AssetRecordEntity record = AssetRecordEntity.create(1L, 2L, "未细分基金", AssetRecordEntity.KIND_FUND,
                null, new BigDecimal("100000"), "CNY", null);

        record.splitOut(new BigDecimal("100000"));

        Assert.assertEquals(BigDecimal.ZERO, record.getAmount());
        Assert.assertEquals(AssetRecordEntity.STATUS_ARCHIVED, record.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPublicAssetIdWithoutKind() {
        AssetRecordEntity.create(1L, 2L, "非法记录", null, 9L, BigDecimal.ONE, "CNY", null);
    }

    @Test(expected = IllegalStateException.class)
    public void concreteHoldingKeepsServerSnapshotName() {
        AssetRecordEntity record = AssetRecordEntity.create(1L, 2L, "示例基金", AssetRecordEntity.KIND_FUND,
                9L, BigDecimal.ONE, "CNY", null);

        record.updateDetails("用户改名", null);
    }
}
