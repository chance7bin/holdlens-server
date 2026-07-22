package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IMarketAssetService;
import com.echoamoy.holdlens.server.api.dto.MarketAssetDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetQueryCase;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetQueryResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MarketAssetController implements IMarketAssetService {

    @Resource private IMarketAssetQueryCase marketAssetQueryCase;

    @Override
    @GetMapping("/api/watchlist/assets")
    public Response<MarketAssetDTO.Watchlist> queryWatchlist(@RequestParam("userId") Long userId,
                                                             @RequestParam(value = "assetKind", required = false) String assetKind) {
        MarketAssetQueryResult.Watchlist result = marketAssetQueryCase.queryWatchlist(userId, assetKind);
        return Response.ok(MarketAssetDTO.Watchlist.builder()
                .fundCount(result.getFundCount()).stockCount(result.getStockCount())
                .items(toItems(result.getItems())).build());
    }

    @Override
    @GetMapping("/api/assets/search")
    public Response<MarketAssetDTO.Search> search(@RequestParam("userId") Long userId, @RequestParam("q") String keyword,
                                                   @RequestParam(value = "assetKind", required = false) String assetKind,
                                                   @RequestParam(value = "market", required = false) String market,
                                                   @RequestParam(value = "limit", required = false) Integer limit) {
        return Response.ok(MarketAssetDTO.Search.builder()
                .items(toItems(marketAssetQueryCase.search(userId, keyword, assetKind, market, limit).getItems()))
                .build());
    }

    @Override
    @GetMapping("/api/stocks/detail")
    public Response<MarketAssetDTO.StockDetail> queryStockDetail(@RequestParam("userId") Long userId,
                                                                  @RequestParam("assetRef") String assetRef) {
        MarketAssetQueryResult.StockDetail r = marketAssetQueryCase.queryStockDetail(userId, assetRef);
        return Response.ok(MarketAssetDTO.StockDetail.builder()
                .assetKind(r.getAssetKind()).assetRef(r.getAssetRef()).code(r.getCode()).name(r.getName())
                .market(r.getMarket()).marketLabel(r.getMarketLabel()).currency(r.getCurrency())
                .latestPrice(r.getLatestPrice()).changeAmount(r.getChangeAmount()).changePercent(r.getChangePercent())
                .openPrice(r.getOpenPrice()).highPrice(r.getHighPrice()).lowPrice(r.getLowPrice())
                .previousClose(r.getPreviousClose()).volume(r.getVolume()).volumeUnit(r.getVolumeUnit())
                .peRatio(r.getPeRatio()).totalMarketValue(r.getTotalMarketValue()).quoteAsOf(r.getQuoteAsOf())
                .delayNotice(r.getDelayNotice()).watchlisted(r.getWatchlisted()).build());
    }

    private List<MarketAssetDTO.Item> toItems(List<MarketAssetQueryResult.Item> items) {
        if (items == null) return List.of();
        return items.stream().map(item -> MarketAssetDTO.Item.builder()
                .assetKind(item.getAssetKind()).assetRef(item.getAssetRef()).code(item.getCode()).name(item.getName())
                .assetType(item.getAssetType()).market(item.getMarket()).marketLabel(item.getMarketLabel())
                .currency(item.getCurrency()).latestValue(item.getLatestValue()).changePercent(item.getChangePercent())
                .valueAsOf(item.getValueAsOf()).watchlisted(item.getWatchlisted()).build()).toList();
    }
}
