package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.funddata.model.entity.FundRefreshTargetEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundTopHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingLogDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingLogPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class FundDataRepository implements IFundDataRepository {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private IFundDao fundDao;

    @Resource
    private IFundTopHoldingDao fundTopHoldingDao;

    @Resource
    private IProcessingLogDao processingLogDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCurrentData(FundCurrentDataAggregate aggregate) {
        if (aggregate.getFunds() != null) {
            for (FundCurrentDataAggregate.FundDetail fund : aggregate.getFunds()) {
                fundDao.upsert(toFundPO(fund));
                // 当前重仓以基金代码为边界整体替换，避免最新结果减少 rank 时旧 rank 残留。
                fundTopHoldingDao.deleteByFundCode(fund.getFundCode());
                if (fund.getTopHoldings() != null) {
                    for (FundCurrentDataAggregate.TopHolding topHolding : fund.getTopHoldings()) {
                        fundTopHoldingDao.insert(toTopHoldingPO(fund.getFundCode(), topHolding));
                    }
                }
            }
        }
        if (aggregate.getWarnings() != null) {
            for (FundCurrentDataAggregate.RefreshWarning warning : aggregate.getWarnings()) {
                processingLogDao.insert(toProcessingLogPO(aggregate.getSourceRefId(), warning));
            }
        }
    }

    @Override
    public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
        if (fundCodes == null || fundCodes.isEmpty()) {
            return Map.of();
        }
        List<FundPO> fundPOList = fundDao.selectByFundCodes(fundCodes);
        Map<String, List<FundTopHoldingPO>> topHoldingsByFundCode = groupTopHoldings(fundCodes);
        Map<String, FundCurrentDataAggregate.FundDetail> result = new LinkedHashMap<>();
        for (FundPO fundPO : fundPOList) {
            result.put(fundPO.getFundCode(), toFundDetail(fundPO, topHoldingsByFundCode.getOrDefault(fundPO.getFundCode(), List.of())));
        }
        return result;
    }

    @Override
    public Set<String> queryExistingFundCodes(Collection<String> fundCodes) {
        if (fundCodes == null || fundCodes.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (FundPO fundPO : fundDao.selectByFundCodes(fundCodes)) {
            result.add(fundPO.getFundCode());
        }
        return result;
    }

    @Override
    public void registerRefreshTargets(List<FundRefreshTargetEntity> refreshTargets) {
        if (refreshTargets == null || refreshTargets.isEmpty()) {
            return;
        }
        for (FundRefreshTargetEntity refreshTarget : refreshTargets) {
            if (refreshTarget == null) {
                continue;
            }
            fundDao.upsertTarget(FundPO.builder()
                    .fundCode(refreshTarget.getFundCode())
                    .fundName(refreshTarget.getFundName())
                    .build());
        }
    }

    @Override
    public List<FundRefreshTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return fundDao.selectRefreshTargetsAfterId(lastId == null ? 0L : lastId, limit).stream()
                .map(po -> FundRefreshTargetEntity.builder()
                        .id(po.getId())
                        .fundCode(po.getFundCode())
                        .fundName(po.getFundName())
                        .build())
                .toList();
    }

    private Map<String, List<FundTopHoldingPO>> groupTopHoldings(Collection<String> fundCodes) {
        List<FundTopHoldingPO> topHoldingPOList = fundTopHoldingDao.selectByFundCodes(fundCodes);
        Map<String, List<FundTopHoldingPO>> result = new LinkedHashMap<>();
        for (FundTopHoldingPO po : topHoldingPOList) {
            result.computeIfAbsent(po.getFundCode(), key -> new java.util.ArrayList<>()).add(po);
        }
        return result;
    }

    private FundPO toFundPO(FundCurrentDataAggregate.FundDetail fund) {
        return FundPO.builder()
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
                .build();
    }

    private FundTopHoldingPO toTopHoldingPO(String fundCode, FundCurrentDataAggregate.TopHolding topHolding) {
        return FundTopHoldingPO.builder()
                .fundCode(fundCode)
                .rankNo(topHolding.getRankNo())
                .stockName(topHolding.getStockName())
                .stockCode(topHolding.getStockCode())
                .market(topHolding.getMarket())
                .holdingRatio(topHolding.getHoldingRatio())
                .quarterChangeType(topHolding.getQuarterChangeType())
                .quarterChangeValue(topHolding.getQuarterChangeValue())
                .build();
    }

    private ProcessingLogPO toProcessingLogPO(String sourceRefId, FundCurrentDataAggregate.RefreshWarning warning) {
        return ProcessingLogPO.builder()
                .sourceRefId(sourceRefId)
                .module(warning.getModule())
                .event(warning.getEvent())
                .message(warning.getMessage())
                .severity(warning.getSeverity())
                .build();
    }

    private FundCurrentDataAggregate.FundDetail toFundDetail(FundPO fundPO,
                                                            List<FundTopHoldingPO> topHoldingPOList) {
        return FundCurrentDataAggregate.FundDetail.builder()
                .id(fundPO.getId())
                .fundCode(fundPO.getFundCode())
                .fundName(fundPO.getFundName())
                .buyStatus(fundPO.getBuyStatus())
                .dailyPurchaseLimit(fundPO.getDailyPurchaseLimit())
                .returnsAsOf(fundPO.getReturnsAsOf())
                .topHoldingsAsOf(fundPO.getTopHoldingsAsOf())
                .publicHoldingsStatus(fundPO.getPublicHoldingsStatus())
                .oneMonthReturn(fundPO.getOneMonthReturn())
                .threeMonthsReturn(fundPO.getThreeMonthsReturn())
                .sixMonthsReturn(fundPO.getSixMonthsReturn())
                .oneYearReturn(fundPO.getOneYearReturn())
                .threeYearsReturn(fundPO.getThreeYearsReturn())
                .updateTime(toBeijingLocalDateTime(fundPO.getUpdateTime()))
                .topHoldings(topHoldingPOList.stream().map(this::toTopHolding).toList())
                .build();
    }

    private LocalDateTime toBeijingLocalDateTime(Date value) {
        if (value == null) {
            return null;
        }
        return value.toInstant().atZone(BEIJING_ZONE).toLocalDateTime();
    }

    private FundCurrentDataAggregate.TopHolding toTopHolding(FundTopHoldingPO po) {
        return FundCurrentDataAggregate.TopHolding.builder()
                .fundCode(po.getFundCode())
                .rankNo(po.getRankNo())
                .stockName(po.getStockName())
                .stockCode(po.getStockCode())
                .market(po.getMarket())
                .holdingRatio(po.getHoldingRatio())
                .quarterChangeType(po.getQuarterChangeType())
                .quarterChangeValue(po.getQuarterChangeValue())
                .build();
    }

}
