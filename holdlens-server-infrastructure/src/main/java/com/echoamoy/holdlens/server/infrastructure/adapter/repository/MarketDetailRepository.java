package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.marketdetail.adapter.repository.IMarketDetailRepository;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.FundNavHistoryEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.FundPeriodPerformanceEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.MarketDetailSliceStateEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockCompanyProfileEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockDetailSliceStateEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockPriceBarEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IMarketDetailDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundNavHistoryPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPeriodPerformancePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.MarketDetailSliceStatePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockCompanyProfilePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockDetailSliceStatePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockPriceBarPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Repository
public class MarketDetailRepository implements IMarketDetailRepository {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    @Resource private IMarketDetailDao marketDetailDao;

    @Override
    public void upsertFundNavHistory(List<FundNavHistoryEntity> points) {
        if (points == null || points.isEmpty()) return;
        marketDetailDao.upsertFundNavHistory(points.stream().map(this::toPO).toList());
    }

    @Override
    public void upsertFundPeriodPerformance(List<FundPeriodPerformanceEntity> rows) {
        if (rows == null || rows.isEmpty()) return;
        marketDetailDao.upsertFundPeriodPerformance(rows.stream().map(this::toPO).toList());
    }

    @Override
    public void upsertStockPriceBars(List<StockPriceBarEntity> bars) {
        if (bars == null || bars.isEmpty()) return;
        marketDetailDao.upsertStockPriceBars(bars.stream().map(this::toPO).toList());
    }

    @Override
    public void upsertStockCompanyProfile(StockCompanyProfileEntity profile) {
        if (profile != null) marketDetailDao.upsertStockCompanyProfile(toPO(profile));
    }

    @Override
    public List<FundNavHistoryEntity> queryFundNavHistory(String fundCode, LocalDate startDate) {
        return marketDetailDao.selectFundNavHistory(fundCode, toDate(startDate)).stream().map(this::toEntity).toList();
    }

    @Override
    public LocalDate queryLatestFundNavDate(String fundCode) {
        Date value = marketDetailDao.selectLatestFundNavDate(fundCode);
        return value == null ? null : new java.sql.Date(value.getTime()).toLocalDate();
    }

    @Override
    public List<FundPeriodPerformanceEntity> queryFundPeriodPerformance(String fundCode) {
        return marketDetailDao.selectFundPeriodPerformance(fundCode).stream().map(this::toEntity).toList();
    }

    @Override
    public void ensureFundSliceStates(String fundCode, List<String> sliceTypes) {
        if (sliceTypes != null && !sliceTypes.isEmpty()) {
            marketDetailDao.insertFundSliceStatesIfAbsent(fundCode, sliceTypes);
        }
    }

    @Override
    public MarketDetailSliceStateEntity lockFundSliceState(String fundCode, String sliceType) {
        MarketDetailSliceStatePO po = marketDetailDao.selectFundSliceStateForUpdate(fundCode, sliceType);
        return po == null ? null : toEntity(po);
    }

    @Override
    public void updateFundSliceState(MarketDetailSliceStateEntity state) {
        marketDetailDao.updateFundSliceState(toPO(state));
    }

    @Override
    public boolean updateFundSliceStateIfActiveTask(MarketDetailSliceStateEntity state) {
        return marketDetailDao.updateFundSliceStateIfActiveTask(toPO(state)) == 1;
    }

    @Override
    public List<StockPriceBarEntity> queryStockPriceBars(String stockCode, String market, String granularity,
                                                         LocalDateTime startTime) {
        return marketDetailDao.selectStockPriceBars(stockCode, market, granularity, toDate(startTime))
                .stream().map(this::toEntity).toList();
    }

    @Override
    public LocalDateTime queryLatestStockBarTime(String stockCode, String market, String granularity) {
        return toLocalDateTime(marketDetailDao.selectLatestStockBarTime(stockCode, market, granularity));
    }

    @Override
    public StockCompanyProfileEntity queryStockCompanyProfile(String stockCode, String market) {
        StockCompanyProfilePO po = marketDetailDao.selectStockCompanyProfile(stockCode, market);
        return po == null ? null : toEntity(po);
    }

    @Override
    public void ensureStockSliceStates(String assetRef, List<String> sliceTypes) {
        if (sliceTypes != null && !sliceTypes.isEmpty()) {
            marketDetailDao.insertStockSliceStatesIfAbsent(assetRef, sliceTypes);
        }
    }

    @Override
    public StockDetailSliceStateEntity lockStockSliceState(String assetRef, String sliceType) {
        StockDetailSliceStatePO po = marketDetailDao.selectStockSliceStateForUpdate(assetRef, sliceType);
        return po == null ? null : toEntity(po);
    }

    @Override
    public List<StockDetailSliceStateEntity> queryStockSliceStates(String assetRef) {
        return marketDetailDao.selectStockSliceStates(assetRef).stream().map(this::toEntity).toList();
    }

    @Override
    public void updateStockSliceState(StockDetailSliceStateEntity state) {
        marketDetailDao.updateStockSliceState(toPO(state));
    }

    @Override
    public boolean updateStockSliceStateIfActiveTask(StockDetailSliceStateEntity state) {
        return marketDetailDao.updateStockSliceStateIfActiveTask(toPO(state)) == 1;
    }

    private FundNavHistoryPO toPO(FundNavHistoryEntity e) {
        return FundNavHistoryPO.builder().id(e.getId()).fundCode(e.getFundCode()).navDate(toDate(e.getNavDate()))
                .unitNav(e.getUnitNav()).accumulatedNav(e.getAccumulatedNav()).dailyGrowthRate(e.getDailyGrowthRate())
                .sourceAsOf(toDate(e.getSourceAsOf())).fetchedAt(toDate(e.getFetchedAt())).build();
    }

    private FundPeriodPerformancePO toPO(FundPeriodPerformanceEntity e) {
        return FundPeriodPerformancePO.builder().id(e.getId()).fundCode(e.getFundCode()).period(e.getPeriod())
                .fundReturn(e.getFundReturn()).peerAverage(e.getPeerAverage()).peerRank(e.getPeerRank())
                .peerTotal(e.getPeerTotal()).rankChange(e.getRankChange()).asOf(toDate(e.getAsOf()))
                .fetchedAt(toDate(e.getFetchedAt())).build();
    }

    private MarketDetailSliceStatePO toPO(MarketDetailSliceStateEntity e) {
        return MarketDetailSliceStatePO.builder().id(e.getId()).fundCode(e.getFundCode()).sliceType(e.getSliceType())
                .status(e.getStatus()).activeTaskId(e.getActiveTaskId()).lastAttemptAt(toDate(e.getLastAttemptAt()))
                .lastSuccessAt(toDate(e.getLastSuccessAt())).errorSummary(e.getErrorSummary()).build();
    }

    private StockPriceBarPO toPO(StockPriceBarEntity e) {
        return StockPriceBarPO.builder().id(e.getId()).stockCode(e.getStockCode()).market(e.getMarket())
                .granularity(e.getGranularity()).barTime(toDate(e.getBarTime())).open(e.getOpen()).high(e.getHigh())
                .low(e.getLow()).close(e.getClose()).volume(e.getVolume()).currency(e.getCurrency())
                .sourceAsOf(toDate(e.getSourceAsOf())).fetchedAt(toDate(e.getFetchedAt())).build();
    }

    private StockCompanyProfilePO toPO(StockCompanyProfileEntity e) {
        return StockCompanyProfilePO.builder().id(e.getId()).stockCode(e.getStockCode()).market(e.getMarket())
                .companyName(e.getCompanyName()).industry(e.getIndustry()).businessSummary(e.getBusinessSummary())
                .companyProfile(e.getCompanyProfile()).website(e.getWebsite()).sourceAsOf(toDate(e.getSourceAsOf()))
                .fetchedAt(toDate(e.getFetchedAt())).build();
    }

    private StockDetailSliceStatePO toPO(StockDetailSliceStateEntity e) {
        return StockDetailSliceStatePO.builder().id(e.getId()).assetRef(e.getAssetRef()).sliceType(e.getSliceType())
                .status(e.getStatus()).activeTaskId(e.getActiveTaskId()).lastAttemptAt(toDate(e.getLastAttemptAt()))
                .lastSuccessAt(toDate(e.getLastSuccessAt())).errorSummary(e.getErrorSummary()).build();
    }

    private FundNavHistoryEntity toEntity(FundNavHistoryPO p) {
        return FundNavHistoryEntity.builder().id(p.getId()).fundCode(p.getFundCode())
                .navDate(p.getNavDate() == null ? null : new java.sql.Date(p.getNavDate().getTime()).toLocalDate())
                .unitNav(p.getUnitNav()).accumulatedNav(p.getAccumulatedNav()).dailyGrowthRate(p.getDailyGrowthRate())
                .sourceAsOf(toLocalDateTime(p.getSourceAsOf())).fetchedAt(toLocalDateTime(p.getFetchedAt())).build();
    }

    private FundPeriodPerformanceEntity toEntity(FundPeriodPerformancePO p) {
        return FundPeriodPerformanceEntity.builder().id(p.getId()).fundCode(p.getFundCode()).period(p.getPeriod())
                .fundReturn(p.getFundReturn()).peerAverage(p.getPeerAverage()).peerRank(p.getPeerRank())
                .peerTotal(p.getPeerTotal()).rankChange(p.getRankChange())
                .asOf(p.getAsOf() == null ? null : new java.sql.Date(p.getAsOf().getTime()).toLocalDate())
                .fetchedAt(toLocalDateTime(p.getFetchedAt())).build();
    }

    private MarketDetailSliceStateEntity toEntity(MarketDetailSliceStatePO p) {
        return MarketDetailSliceStateEntity.builder().id(p.getId()).fundCode(p.getFundCode()).sliceType(p.getSliceType())
                .status(p.getStatus()).activeTaskId(p.getActiveTaskId()).lastAttemptAt(toLocalDateTime(p.getLastAttemptAt()))
                .lastSuccessAt(toLocalDateTime(p.getLastSuccessAt())).errorSummary(p.getErrorSummary())
                .createTime(toLocalDateTime(p.getCreateTime())).updateTime(toLocalDateTime(p.getUpdateTime())).build();
    }

    private StockPriceBarEntity toEntity(StockPriceBarPO p) {
        return StockPriceBarEntity.builder().id(p.getId()).stockCode(p.getStockCode()).market(p.getMarket())
                .granularity(p.getGranularity()).barTime(toLocalDateTime(p.getBarTime())).open(p.getOpen()).high(p.getHigh())
                .low(p.getLow()).close(p.getClose()).volume(p.getVolume()).currency(p.getCurrency())
                .sourceAsOf(toLocalDateTime(p.getSourceAsOf())).fetchedAt(toLocalDateTime(p.getFetchedAt())).build();
    }

    private StockCompanyProfileEntity toEntity(StockCompanyProfilePO p) {
        return StockCompanyProfileEntity.builder().id(p.getId()).stockCode(p.getStockCode()).market(p.getMarket())
                .companyName(p.getCompanyName()).industry(p.getIndustry()).businessSummary(p.getBusinessSummary())
                .companyProfile(p.getCompanyProfile()).website(p.getWebsite()).sourceAsOf(toLocalDateTime(p.getSourceAsOf()))
                .fetchedAt(toLocalDateTime(p.getFetchedAt())).build();
    }

    private StockDetailSliceStateEntity toEntity(StockDetailSliceStatePO p) {
        return StockDetailSliceStateEntity.builder().id(p.getId()).assetRef(p.getAssetRef())
                .sliceType(p.getSliceType()).status(p.getStatus()).activeTaskId(p.getActiveTaskId())
                .lastAttemptAt(toLocalDateTime(p.getLastAttemptAt())).lastSuccessAt(toLocalDateTime(p.getLastSuccessAt()))
                .errorSummary(p.getErrorSummary()).createTime(toLocalDateTime(p.getCreateTime()))
                .updateTime(toLocalDateTime(p.getUpdateTime())).build();
    }

    private Date toDate(LocalDate value) {
        return value == null ? null : java.sql.Date.valueOf(value);
    }

    private Date toDate(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(BUSINESS_ZONE).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date value) {
        return value == null ? null : value.toInstant().atZone(BUSINESS_ZONE).toLocalDateTime();
    }
}
