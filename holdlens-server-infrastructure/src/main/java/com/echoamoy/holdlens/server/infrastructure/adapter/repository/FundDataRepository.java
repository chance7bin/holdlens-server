package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundDetailSnapshotAggregate;
import com.echoamoy.holdlens.server.infrastructure.dao.IAgentWarningDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDetailItemDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDetailSnapshotDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundTopHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AgentWarningPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailItemPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailSnapshotPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class FundDataRepository implements IFundDataRepository {

    @Resource
    private IFundDetailSnapshotDao fundDetailSnapshotDao;

    @Resource
    private IFundDetailItemDao fundDetailItemDao;

    @Resource
    private IFundTopHoldingDao fundTopHoldingDao;

    @Resource
    private IAgentWarningDao agentWarningDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSnapshot(FundDetailSnapshotAggregate aggregate) {
        FundDetailSnapshotPO snapshotPO = FundDetailSnapshotPO.builder()
                .schemaVersion(aggregate.getSchemaVersion())
                .generatedAt(aggregate.getGeneratedAt())
                .snapshotStatus(aggregate.getSnapshotStatus())
                .sourceType(aggregate.getSourceType())
                .sourceRefId(aggregate.getSourceRefId())
                .dataSourcesJson(aggregate.getDataSourcesJson())
                .build();
        fundDetailSnapshotDao.insert(snapshotPO);

        Long snapshotId = snapshotPO.getId();
        if (aggregate.getFunds() != null) {
            for (FundDetailSnapshotAggregate.FundDetail fund : aggregate.getFunds()) {
                FundDetailItemPO itemPO = toItemPO(snapshotId, fund);
                fundDetailItemDao.insert(itemPO);
                if (fund.getTopHoldings() != null) {
                    for (FundDetailSnapshotAggregate.TopHolding topHolding : fund.getTopHoldings()) {
                        fundTopHoldingDao.insert(toTopHoldingPO(snapshotId, itemPO.getId(), topHolding));
                    }
                }
            }
        }
        if (aggregate.getWarnings() != null) {
            for (FundDetailSnapshotAggregate.RefreshWarning warning : aggregate.getWarnings()) {
                agentWarningDao.insert(toWarningPO(snapshotId, aggregate.getSourceRefId(), warning));
            }
        }
        return snapshotId;
    }

    @Override
    public Map<String, FundDetailSnapshotAggregate.FundDetail> queryLatestDetails(Set<String> fundCodes) {
        if (fundCodes == null || fundCodes.isEmpty()) {
            return Map.of();
        }
        List<FundDetailItemPO> itemPOList = fundDetailItemDao.selectLatestByFundCodes(fundCodes);
        Map<String, FundDetailSnapshotAggregate.FundDetail> result = new LinkedHashMap<>();
        for (FundDetailItemPO itemPO : itemPOList) {
            FundDetailSnapshotPO snapshotPO = fundDetailSnapshotDao.selectById(itemPO.getSnapshotId());
            List<FundTopHoldingPO> topHoldingPOList = fundTopHoldingDao.selectByFundDetailItemId(itemPO.getId());
            result.put(itemPO.getFundCode(), toFundDetail(itemPO, snapshotPO, topHoldingPOList));
        }
        return result;
    }

    private FundDetailItemPO toItemPO(Long snapshotId, FundDetailSnapshotAggregate.FundDetail fund) {
        return FundDetailItemPO.builder()
                .snapshotId(snapshotId)
                .fundAssetId(null)
                .fundCode(fund.getFundCode())
                .fundName(fund.getFundName())
                .buyStatus(fund.getBuyStatus())
                .dailyPurchaseLimit(fund.getDailyPurchaseLimit())
                .returnsAsOf(fund.getReturnsAsOf())
                .topHoldingsAsOf(fund.getTopHoldingsAsOf())
                .publicHoldingsStatus(fund.getPublicHoldingsStatus())
                .oneMonthReturn(fund.getOneMonthReturn())
                .threeMonthsReturn(fund.getThreeMonthsReturn())
                .sixMonthsReturn(fund.getSixMonthsReturn())
                .oneYearReturn(fund.getOneYearReturn())
                .threeYearsReturn(fund.getThreeYearsReturn())
                .fieldSourcesJson(fund.getFieldSourcesJson())
                .missingReasonsJson(fund.getMissingReasonsJson())
                .build();
    }

    private FundTopHoldingPO toTopHoldingPO(Long snapshotId, Long fundDetailItemId, FundDetailSnapshotAggregate.TopHolding topHolding) {
        return FundTopHoldingPO.builder()
                .fundDetailItemId(fundDetailItemId)
                .snapshotId(snapshotId)
                .rankNo(topHolding.getRankNo())
                .stockName(topHolding.getStockName())
                .stockCode(topHolding.getStockCode())
                .market(topHolding.getMarket())
                .dailyReturn(topHolding.getDailyReturn())
                .holdingRatio(topHolding.getHoldingRatio())
                .quarterChangeType(topHolding.getQuarterChangeType())
                .quarterChangeValue(topHolding.getQuarterChangeValue())
                .missingReasonsJson(topHolding.getMissingReasonsJson())
                .build();
    }

    private AgentWarningPO toWarningPO(Long snapshotId, String sourceRefId, FundDetailSnapshotAggregate.RefreshWarning warning) {
        return AgentWarningPO.builder()
                .warningType("refresh")
                .sourceType("agent")
                .sourceRefId(sourceRefId)
                .snapshotId(snapshotId)
                .fundCode(warning.getFundCode())
                .code(warning.getCode())
                .message(warning.getMessage())
                .sourceSection(warning.getSourceSection())
                .sourceRowNumber(warning.getSourceRowNumber())
                .severity(warning.getSeverity())
                .build();
    }

    private FundDetailSnapshotAggregate.FundDetail toFundDetail(FundDetailItemPO itemPO,
                                                               FundDetailSnapshotPO snapshotPO,
                                                               List<FundTopHoldingPO> topHoldingPOList) {
        return FundDetailSnapshotAggregate.FundDetail.builder()
                .id(itemPO.getId())
                .snapshotId(itemPO.getSnapshotId())
                .fundCode(itemPO.getFundCode())
                .fundName(itemPO.getFundName())
                .buyStatus(itemPO.getBuyStatus())
                .dailyPurchaseLimit(itemPO.getDailyPurchaseLimit())
                .returnsAsOf(itemPO.getReturnsAsOf())
                .topHoldingsAsOf(itemPO.getTopHoldingsAsOf())
                .publicHoldingsStatus(itemPO.getPublicHoldingsStatus())
                .oneMonthReturn(itemPO.getOneMonthReturn())
                .threeMonthsReturn(itemPO.getThreeMonthsReturn())
                .sixMonthsReturn(itemPO.getSixMonthsReturn())
                .oneYearReturn(itemPO.getOneYearReturn())
                .threeYearsReturn(itemPO.getThreeYearsReturn())
                .fieldSourcesJson(itemPO.getFieldSourcesJson())
                .missingReasonsJson(itemPO.getMissingReasonsJson())
                .generatedAt(snapshotPO == null ? null : snapshotPO.getGeneratedAt())
                .topHoldings(topHoldingPOList.stream().map(this::toTopHolding).toList())
                .build();
    }

    private FundDetailSnapshotAggregate.TopHolding toTopHolding(FundTopHoldingPO po) {
        return FundDetailSnapshotAggregate.TopHolding.builder()
                .rankNo(po.getRankNo())
                .stockName(po.getStockName())
                .stockCode(po.getStockCode())
                .market(po.getMarket())
                .dailyReturn(po.getDailyReturn())
                .holdingRatio(po.getHoldingRatio())
                .quarterChangeType(po.getQuarterChangeType())
                .quarterChangeValue(po.getQuarterChangeValue())
                .missingReasonsJson(po.getMissingReasonsJson())
                .build();
    }

}
