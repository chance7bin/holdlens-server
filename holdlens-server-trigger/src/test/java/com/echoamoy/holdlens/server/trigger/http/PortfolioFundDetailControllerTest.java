package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.dto.FundDetailDTO;
import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

public class PortfolioFundDetailControllerTest {

    @Test
    public void fundDetailResponseExposesIndependentAssetAllocations() throws Exception {
        PortfolioFundDetailController controller = new PortfolioFundDetailController();
        set(controller, "portfolioFundDetailCase", new FakeCase());

        FundDetailDTO detail = controller.queryFundDetail("000001").getData();

        Assert.assertEquals(Date.valueOf("2026-06-30"), detail.getAssetAllocationAsOf());
        Assert.assertEquals("available", detail.getAssetAllocationStatus());
        Assert.assertEquals(2, detail.getAssetAllocations().size());
        Assert.assertEquals("unknown", detail.getAssetAllocations().get(0).getAssetType());
        Assert.assertEquals("其他资产A", detail.getAssetAllocations().get(0).getAssetTypeName());
        Assert.assertEquals(Integer.valueOf(1), detail.getAssetAllocations().get(0).getDisplayOrder());
        Assert.assertNotNull(detail.getTopHoldings());
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeCase implements IPortfolioFundDetailCase {
        @Override
        public PortfolioFundDetailResult queryPortfolioFundDetails(Long userId) {
            return null;
        }

        @Override
        public PortfolioFundDetailResult.FundDetail queryFundDetail(String fundCode) {
            return PortfolioFundDetailResult.FundDetail.builder()
                    .fundCode(fundCode)
                    .assetAllocationAsOf(Date.valueOf("2026-06-30"))
                    .assetAllocationStatus("available")
                    .assetAllocationFetchedAt(Date.valueOf("2026-07-16"))
                    .assetAllocations(List.of(
                            PortfolioFundDetailResult.AssetAllocation.builder()
                                    .assetType("unknown").assetTypeName("其他资产A")
                                    .allocationRatio(new BigDecimal("1.1000")).displayOrder(1).build(),
                            PortfolioFundDetailResult.AssetAllocation.builder()
                                    .assetType("unknown").assetTypeName("其他资产B")
                                    .allocationRatio(new BigDecimal("2.2000")).displayOrder(2).build()))
                    .topHoldings(List.of())
                    .build();
        }
    }
}
