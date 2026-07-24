package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAssetService;
import com.echoamoy.holdlens.server.api.dto.AssetDTO;
import com.echoamoy.holdlens.server.api.request.AssetRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.portfolio.IAssetManagementCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.AssetManagementCommand;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetOverviewEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetSummaryEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
public class AssetController implements IAssetService {

    @Resource private IAssetManagementCase assetManagementCase;

    public AssetController() { }

    AssetController(IAssetManagementCase assetManagementCase) {
        this.assetManagementCase = assetManagementCase;
    }

    @Override
    @GetMapping("/api/asset-catalogs")
    public Response<List<AssetDTO.Catalog>> queryCatalogs(@RequestParam("userId") Long userId) {
        return Response.ok(assetManagementCase.queryCatalogs(userId).stream().map(this::toCatalog).toList());
    }

    @Override
    @PostMapping("/api/asset-catalogs")
    public Response<AssetDTO.Catalog> createCatalog(@Valid @RequestBody AssetRequestDTO.CreateCatalog request) {
        return Response.ok(toCatalog(assetManagementCase.createCatalog(AssetManagementCommand.CreateCatalog.builder()
                .userId(request.getUserId()).parentId(request.getParentId()).catalogName(request.getCatalogName())
                .balanceDirection(request.getBalanceDirection()).sortOrder(request.getSortOrder()).build())));
    }

    @Override
    @PostMapping("/api/asset-catalogs/{catalogId}/update-details")
    public Response<AssetDTO.Catalog> updateCatalog(@PathVariable("catalogId") Long catalogId,
                                                    @Valid @RequestBody AssetRequestDTO.UpdateCatalog request) {
        return Response.ok(toCatalog(assetManagementCase.updateCatalog(AssetManagementCommand.UpdateCatalog.builder()
                .userId(request.getUserId()).catalogId(catalogId).parentId(request.getParentId())
                .catalogName(request.getCatalogName()).balanceDirection(request.getBalanceDirection())
                .sortOrder(request.getSortOrder()).build())));
    }

    @Override
    @PostMapping("/api/asset-catalogs/{catalogId}/delete")
    public Response<Void> deleteCatalog(@PathVariable("catalogId") Long catalogId,
                                        @Valid @RequestBody AssetRequestDTO.UserOperation request) {
        assetManagementCase.deleteCatalog(request.getUserId(), catalogId);
        return Response.ok(null);
    }

    @Override
    @GetMapping("/api/asset-records")
    public Response<List<AssetDTO.Record>> queryRecords(@RequestParam("userId") Long userId,
                                                        @RequestParam(value = "assetRef", required = false) String assetRef) {
        return Response.ok(assetManagementCase.queryRecords(userId, assetRef).stream().map(this::toRecord).toList());
    }

    @Override
    @GetMapping("/api/asset-records/{recordId}")
    public Response<AssetDTO.Record> queryRecord(@PathVariable("recordId") Long recordId,
                                                  @RequestParam("userId") Long userId) {
        return Response.ok(toRecord(assetManagementCase.queryRecord(userId, recordId)));
    }

    @Override
    @PostMapping("/api/asset-records")
    public Response<AssetDTO.Record> createRecord(@Valid @RequestBody AssetRequestDTO.CreateRecord request) {
        return Response.ok(toRecord(assetManagementCase.createRecord(AssetManagementCommand.CreateRecord.builder()
                .userId(request.getUserId()).catalogId(request.getCatalogId()).assetRef(request.getAssetRef())
                .recordName(request.getRecordName()).amount(request.getAmount()).currency(request.getCurrency())
                .remark(request.getRemark()).build())));
    }

    @Override
    @PostMapping("/api/asset-records/{recordId}/update-details")
    public Response<AssetDTO.Record> updateRecordDetails(@PathVariable("recordId") Long recordId,
                                                         @Valid @RequestBody AssetRequestDTO.UpdateDetails request) {
        return Response.ok(toRecord(assetManagementCase.updateRecordDetails(AssetManagementCommand.UpdateDetails.builder()
                .userId(request.getUserId()).recordId(recordId).recordName(request.getRecordName())
                .remark(request.getRemark()).build())));
    }

    @Override
    @PostMapping("/api/asset-records/{recordId}/update-amount")
    public Response<AssetDTO.Record> updateRecordAmount(@PathVariable("recordId") Long recordId,
                                                        @Valid @RequestBody AssetRequestDTO.UpdateAmount request) {
        return Response.ok(toRecord(assetManagementCase.updateRecordAmount(AssetManagementCommand.UpdateAmount.builder()
                .userId(request.getUserId()).recordId(recordId).amount(request.getAmount()).build())));
    }

    @Override
    @PostMapping("/api/asset-records/{recordId}/{action:archive|restore|delete}")
    public Response<AssetDTO.Record> changeRecordStatus(@PathVariable("recordId") Long recordId,
                                                        @PathVariable("action") String action,
                                                        @Valid @RequestBody AssetRequestDTO.UserOperation request) {
        AssetRecordEntity record = switch (action) {
            case "archive" -> assetManagementCase.archiveRecord(request.getUserId(), recordId);
            case "restore" -> assetManagementCase.restoreRecord(request.getUserId(), recordId);
            case "delete" -> assetManagementCase.deleteRecord(request.getUserId(), recordId);
            default -> throw new IllegalArgumentException("资产状态操作不支持");
        };
        return Response.ok(toRecord(record));
    }

    @Override
    @PostMapping("/api/asset-records/{recordId}/split")
    public Response<AssetDTO.Record> splitRecord(@PathVariable("recordId") Long recordId,
                                                 @Valid @RequestBody AssetRequestDTO.SplitRecord request) {
        return Response.ok(toRecord(assetManagementCase.splitRecord(AssetManagementCommand.SplitRecord.builder()
                .userId(request.getUserId()).sourceRecordId(recordId).assetRef(request.getAssetRef())
                .amount(request.getAmount()).remark(request.getRemark()).build())));
    }

    @Override
    @GetMapping("/api/assets/summary")
    public Response<AssetDTO.Summary> summarize(@RequestParam("userId") Long userId,
                                                @RequestParam(value = "targetCurrency", required = false)
                                                String targetCurrency) {
        return Response.ok(toSummary(assetManagementCase.summarize(userId, targetCurrency)));
    }

    @Override
    @GetMapping("/api/assets/overview")
    public Response<AssetDTO.Overview> overview(@RequestParam("userId") Long userId,
                                                @RequestParam(value = "targetCurrency", required = false)
                                                String targetCurrency) {
        return Response.ok(toOverview(assetManagementCase.overview(userId, targetCurrency)));
    }

    @Override
    @GetMapping("/internal/exchange-rates")
    public Response<List<AssetDTO.ExchangeRate>> queryExchangeRates(
            @RequestParam("baseCurrencies") List<String> baseCurrencies) {
        return Response.ok(assetManagementCase.queryExchangeRates(baseCurrencies).stream().map(this::toRate).toList());
    }

    @Override
    @PostMapping("/internal/exchange-rates/upsert")
    public Response<AssetDTO.ExchangeRate> upsertExchangeRate(
            @Valid @RequestBody AssetRequestDTO.UpsertExchangeRate request) {
        return Response.ok(toRate(assetManagementCase.upsertExchangeRate(AssetManagementCommand.UpsertExchangeRate.builder()
                .baseCurrency(request.getBaseCurrency()).quoteCurrency(request.getQuoteCurrency()).rate(request.getRate())
                .source(request.getSource()).sourceAsOf(request.getSourceAsOf()).fetchedAt(request.getFetchedAt()).build())));
    }

    private AssetDTO.Catalog toCatalog(AssetCatalogEntity entity) {
        return AssetDTO.Catalog.builder().id(entity.getId()).parentId(entity.getParentId())
                .catalogCode(entity.getCatalogCode()).catalogName(entity.getCatalogName())
                .catalogScope(entity.getCatalogScope()).balanceDirection(entity.getBalanceDirection())
                .sortOrder(entity.getSortOrder()).status(entity.getStatus()).build();
    }

    private AssetDTO.Catalog toCatalog(AssetOverviewEntity.CatalogAmount value) {
        AssetCatalogEntity entity = value.getCatalog();
        return AssetDTO.Catalog.builder().id(entity.getId()).parentId(entity.getParentId())
                .catalogCode(entity.getCatalogCode()).catalogName(entity.getCatalogName())
                .catalogScope(entity.getCatalogScope()).balanceDirection(entity.getBalanceDirection())
                .sortOrder(entity.getSortOrder()).status(entity.getStatus())
                .convertedAmount(value.getConvertedAmount()).partial(value.isPartial())
                .missingCurrencies(value.getMissingCurrencies()).build();
    }

    private AssetDTO.Record toRecord(AssetRecordEntity entity) {
        return AssetDTO.Record.builder().id(entity.getId()).catalogId(entity.getCatalogId())
                .catalogCode(entity.getCatalogCode()).recordName(entity.getRecordName())
                .assetKind(entity.getAssetKind() == null ? null : entity.getAssetKind().toLowerCase(Locale.ROOT))
                .assetRef(entity.getAssetRef()).fund(toFundAsset(entity)).stock(toStockAsset(entity))
                .amount(entity.getAmount()).currency(entity.getCurrency())
                .remark(entity.getRemark()).status(entity.getStatus()).createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime()).build();
    }

    private AssetDTO.Record toRecord(AssetOverviewEntity.RecordAmount value) {
        AssetRecordEntity entity = value.getRecord();
        return AssetDTO.Record.builder().id(entity.getId()).catalogId(entity.getCatalogId())
                .catalogCode(entity.getCatalogCode()).recordName(entity.getRecordName())
                .assetKind(entity.getAssetKind() == null ? null : entity.getAssetKind().toLowerCase(Locale.ROOT))
                .assetRef(entity.getAssetRef()).fund(toFundAsset(entity)).stock(toStockAsset(entity))
                .amount(entity.getAmount()).currency(entity.getCurrency()).remark(entity.getRemark())
                .status(entity.getStatus()).convertedAmount(value.getConvertedAmount())
                .conversionStatus(value.getConversionStatus()).createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime()).build();
    }

    private AssetDTO.FundAsset toFundAsset(AssetRecordEntity entity) {
        if (!AssetRecordEntity.KIND_FUND.equals(entity.getAssetKind()) || entity.getAssetCode() == null) {
            return null;
        }
        return AssetDTO.FundAsset.builder().assetCode(entity.getAssetCode()).build();
    }

    private AssetDTO.StockAsset toStockAsset(AssetRecordEntity entity) {
        if (!AssetRecordEntity.KIND_STOCK.equals(entity.getAssetKind()) || entity.getAssetCode() == null) {
            return null;
        }
        return AssetDTO.StockAsset.builder().assetCode(entity.getAssetCode())
                .assetMarket(entity.getAssetMarket()).assetMarketLabel(marketLabel(entity.getAssetMarket())).build();
    }

    private String marketLabel(String market) {
        if ("A_SHARE".equals(market)) return "A股";
        if ("US_STOCK".equals(market)) return "美股";
        return null;
    }

    private AssetDTO.Summary toSummary(AssetSummaryEntity entity) {
        return AssetDTO.Summary.builder().targetCurrency(entity.getTargetCurrency()).assetTotal(entity.getAssetTotal())
                .liabilityTotal(entity.getLiabilityTotal()).netAsset(entity.getNetAsset()).partial(entity.isPartial())
                .missingCurrencies(entity.getMissingCurrencies())
                .unconvertedAmounts(entity.getUnconvertedAmounts().stream().map(item ->
                        AssetDTO.UnconvertedAmount.builder().currency(item.getCurrency()).amount(item.getAmount()).build()).toList())
                .build();
    }

    private AssetDTO.Overview toOverview(AssetOverviewEntity entity) {
        AssetSummaryEntity summary = entity.getSummary();
        return AssetDTO.Overview.builder().targetCurrency(summary.getTargetCurrency())
                .assetTotal(summary.getAssetTotal()).liabilityTotal(summary.getLiabilityTotal())
                .netAsset(summary.getNetAsset()).partial(summary.isPartial())
                .missingCurrencies(summary.getMissingCurrencies())
                .unconvertedAmounts(summary.getUnconvertedAmounts().stream().map(item ->
                        AssetDTO.UnconvertedAmount.builder().currency(item.getCurrency())
                                .amount(item.getAmount()).build()).toList())
                .catalogs(entity.getCatalogs().stream().map(this::toCatalog).toList())
                .records(entity.getRecords().stream().map(this::toRecord).toList())
                .build();
    }

    private AssetDTO.ExchangeRate toRate(ExchangeRateEntity entity) {
        return AssetDTO.ExchangeRate.builder().baseCurrency(entity.getBaseCurrency()).quoteCurrency(entity.getQuoteCurrency())
                .rate(entity.getRate()).source(entity.getSource()).sourceAsOf(entity.getSourceAsOf())
                .fetchedAt(entity.getFetchedAt()).build();
    }
}
