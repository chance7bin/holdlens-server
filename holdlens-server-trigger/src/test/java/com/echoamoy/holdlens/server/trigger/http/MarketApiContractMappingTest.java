package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.request.MarketDetailRefreshRequest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MarketApiContractMappingTest {

    @Test
    public void marketAssetEndpointsMatchContracts() throws Exception {
        Method watchlist = MarketAssetController.class.getMethod("queryWatchlist", Long.class, String.class);
        assertGet(watchlist, "/api/watchlist/assets");
        assertRequestParam(watchlist, 0, "userId");
        assertRequestParam(watchlist, 1, "assetKind");

        Method search = MarketAssetController.class.getMethod(
                "search", Long.class, String.class, String.class, String.class, Integer.class);
        assertGet(search, "/api/assets/search");
        assertRequestParam(search, 0, "userId");
        assertRequestParam(search, 1, "q");
        assertRequestParam(search, 2, "assetKind");
        assertRequestParam(search, 3, "market");
        assertRequestParam(search, 4, "limit");

        Method detail = MarketAssetController.class.getMethod("queryStockDetail", Long.class, String.class);
        assertGet(detail, "/api/stocks/detail");
        assertRequestParam(detail, 0, "userId");
        assertRequestParam(detail, 1, "assetRef");
    }

    @Test
    public void marketDetailEndpointsAndDecimalStringVolumeMatchContracts() throws Exception {
        Method create = AgentMarketDetailDataRefreshController.class.getMethod(
                "createTask", MarketDetailRefreshRequest.Create.class);
        Assert.assertArrayEquals(new String[]{"/api/agent/market-detail-data-refresh/tasks"},
                create.getAnnotation(PostMapping.class).value());
        Method fundHistory = AgentMarketDetailDataRefreshController.class.getMethod(
                "queryFundNavHistory", String.class, String.class);
        assertGet(fundHistory, "/api/funds/{fundCode}/nav-history");
        assertPathVariable(fundHistory, 0, "fundCode");
        assertRequestParam(fundHistory, 1, "period");

        Method stockHistory = AgentMarketDetailDataRefreshController.class.getMethod(
                "queryStockPriceHistory", String.class, String.class);
        assertGet(stockHistory, "/api/stocks/price-history");
        assertRequestParam(stockHistory, 0, "assetRef");
        assertRequestParam(stockHistory, 1, "period");

        Method profile = AgentMarketDetailDataRefreshController.class.getMethod(
                "queryStockCompanyProfile", String.class);
        assertGet(profile, "/api/stocks/company-profile");
        assertRequestParam(profile, 0, "assetRef");
        Field volume = MarketDetailRefreshRequest.StockBar.class.getDeclaredField("volume");
        Assert.assertEquals(String.class, volume.getType());
    }

    @Test
    public void portfolioFundEndpointsBindNamesWithoutCompilerMetadata() throws Exception {
        Method portfolio = PortfolioFundDetailController.class.getMethod("queryPortfolioFundDetails", Long.class);
        assertRequestParam(portfolio, 0, "userId");

        Method detail = PortfolioFundDetailController.class.getMethod("queryFundDetail", String.class);
        assertPathVariable(detail, 0, "fundCode");
    }

    private void assertGet(Method method, String path) {
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        Assert.assertNotNull(mapping);
        Assert.assertArrayEquals(new String[]{path}, mapping.value());
    }

    private void assertRequestParam(Method method, int index, String name) {
        RequestParam annotation = method.getParameters()[index].getAnnotation(RequestParam.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(name, annotation.value());
    }

    private void assertPathVariable(Method method, int index, String name) {
        PathVariable annotation = method.getParameters()[index].getAnnotation(PathVariable.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(name, annotation.value());
    }
}
