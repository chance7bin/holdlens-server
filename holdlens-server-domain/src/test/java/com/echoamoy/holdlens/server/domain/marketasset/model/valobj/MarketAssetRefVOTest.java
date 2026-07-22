package com.echoamoy.holdlens.server.domain.marketasset.model.valobj;

import org.junit.Assert;
import org.junit.Test;

public class MarketAssetRefVOTest {

    @Test
    public void shouldGenerateAndParseRefs() {
        Assert.assertEquals("fund:000001", MarketAssetRefVO.fund("000001").value());
        Assert.assertEquals("stock:A_SHARE:000001", MarketAssetRefVO.stock("A_SHARE", "000001").value());
        Assert.assertEquals("US_STOCK", MarketAssetRefVO.parse("stock:US_STOCK:DEMO").getMarket());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectKindConflict() {
        MarketAssetRefVO.parse("fund", "stock:A_SHARE:000001");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnsupportedMarket() {
        MarketAssetRefVO.parse("stock:HK_STOCK:000001");
    }
}
