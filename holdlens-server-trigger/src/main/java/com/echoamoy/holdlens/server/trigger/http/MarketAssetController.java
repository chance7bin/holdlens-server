package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IMarketAssetService;
import com.echoamoy.holdlens.server.api.dto.MarketAssetDTO;
import com.echoamoy.holdlens.server.api.dto.MarketDetailDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.api.request.MarketAssetDetailEnsureRequest;
import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetQueryCase;
import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetDetailCase;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetDetailResult;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetQueryResult;
import com.echoamoy.holdlens.server.trigger.http.auth.CurrentUserContext;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
public class MarketAssetController implements IMarketAssetService {

    @Resource private IMarketAssetQueryCase marketAssetQueryCase;
    @Resource private IMarketAssetDetailCase marketAssetDetailCase;

    @Override
    @GetMapping("/api/watchlist/assets")
    public Response<MarketAssetDTO.Watchlist> queryWatchlist(@RequestParam("userId") Long userId,
                                                             @RequestParam(value = "assetKind", required = false) String assetKind) {
        MarketAssetQueryResult.Watchlist result = marketAssetQueryCase.queryWatchlist(CurrentUserContext.requireMatchingUserId(userId), assetKind);
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
                .items(toItems(marketAssetQueryCase.search(CurrentUserContext.requireMatchingUserId(userId), keyword, assetKind, market, limit).getItems()))
                .build());
    }

    @Override
    @GetMapping("/api/market-assets/detail")
    public Response<MarketAssetDTO.Detail> queryDetail(@RequestParam("userId") Long userId,
                                                        @RequestParam("assetKind") String assetKind,
                                                        @RequestParam("assetRef") String assetRef) {
        MarketAssetDetailResult result = marketAssetDetailCase.queryDetail(CurrentUserContext.requireMatchingUserId(userId), assetKind, assetRef);
        return Response.ok(toDetail(result));
    }

    @Override
    @PostMapping("/api/market-assets/detail/ensure")
    public Response<MarketAssetDTO.Detail> ensureDetail(@RequestBody MarketAssetDetailEnsureRequest request) {
        MarketAssetDetailResult result = marketAssetDetailCase.ensureDetail(
                CurrentUserContext.requireMatchingUserId(request == null ? null : request.getUserId()), request == null ? null : request.getAssetKind(),
                request == null ? null : request.getAssetRef());
        return Response.ok(toDetail(result));
    }

    @Override
    @GetMapping("/api/stocks/detail")
    public Response<MarketAssetDTO.StockDetail> queryStockDetail(@RequestParam("userId") Long userId,
                                                                  @RequestParam("assetRef") String assetRef) {
        MarketAssetQueryResult.StockDetail r = marketAssetQueryCase.queryStockDetail(CurrentUserContext.requireMatchingUserId(userId), assetRef);
        return Response.ok(toStockDetail(r));
    }

    private MarketAssetDTO.StockDetail toStockDetail(MarketAssetQueryResult.StockDetail r) {
        if (r == null) return null;
        return MarketAssetDTO.StockDetail.builder()
                .assetKind(r.getAssetKind()).assetRef(r.getAssetRef()).code(r.getCode()).name(r.getName())
                .market(r.getMarket()).marketLabel(r.getMarketLabel()).currency(r.getCurrency())
                .latestPrice(r.getLatestPrice()).changeAmount(r.getChangeAmount()).changePercent(r.getChangePercent())
                .openPrice(r.getOpenPrice()).highPrice(r.getHighPrice()).lowPrice(r.getLowPrice())
                .previousClose(r.getPreviousClose()).volume(r.getVolume()).volumeUnit(r.getVolumeUnit())
                .peRatio(r.getPeRatio()).totalMarketValue(r.getTotalMarketValue()).quoteAsOf(r.getQuoteAsOf())
                .quoteFetchedAt(r.getQuoteFetchedAt()).delayNotice(r.getDelayNotice())
                .watchlisted(r.getWatchlisted()).build();
    }

    private MarketAssetDTO.Detail toDetail(MarketAssetDetailResult result) {
        return MarketAssetDTO.Detail.builder().assetKind(result.getAssetKind())
                .assetRef(result.getAssetRef()).watchlisted(result.getWatchlisted())
                .fund(FundDetailDtoMapper.toDTO(result.getFund())).stock(toStockDetail(result.getStock()))
                .refresh(toRefresh(result.getRefresh())).build();
    }

    private MarketDetailDTO.DetailRefresh toRefresh(
            com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailResult.DetailRefresh result) {
        if (result == null) return null;
        return MarketDetailDTO.DetailRefresh.builder().assetKind(result.getAssetKind()).assetRef(result.getAssetRef())
                .operationId(result.getOperationId()).status(result.getStatus()).retryAfterMs(result.getRetryAfterMs())
                .slices(result.getSlices().stream().map(slice -> MarketDetailDTO.DetailSlice.builder()
                        .slice(slice.getSlice()).status(slice.getStatus()).freshness(slice.getFreshness())
                        .hasData(slice.getHasData()).build()).toList()).build();
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
