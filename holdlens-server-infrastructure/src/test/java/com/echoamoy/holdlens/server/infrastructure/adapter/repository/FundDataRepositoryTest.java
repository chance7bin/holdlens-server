package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundDetailSnapshotAggregate;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDetailItemDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDetailSnapshotDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundTopHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingLogDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailItemPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailSnapshotPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingLogPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class FundDataRepositoryTest {

    @Test
    public void saveSnapshotPersistsProcessingLogWithModuleAndEvent() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeSnapshotDao snapshotDao = new FakeSnapshotDao();
        FakeProcessingLogDao processingLogDao = new FakeProcessingLogDao();
        setField(repository, "fundDetailSnapshotDao", snapshotDao);
        setField(repository, "fundDetailItemDao", new FakeFundDetailItemDao());
        setField(repository, "fundTopHoldingDao", new FakeFundTopHoldingDao());
        setField(repository, "processingLogDao", processingLogDao);

        repository.saveSnapshot(FundDetailSnapshotAggregate.builder()
                .schemaVersion("fund-detail-refresh-result/v1")
                .generatedAt(new Date())
                .snapshotStatus("partial_failed")
                .sourceRefId("task_1")
                .warnings(List.of(FundDetailSnapshotAggregate.RefreshWarning.builder()
                        .module("fund_refresh")
                        .event("provider_fund_failed")
                        .message("provider failed for one fund")
                        .severity("error")
                        .build()))
                .build());

        Assert.assertEquals("task_1", snapshotDao.inserted.getSourceRefId());
        Assert.assertEquals("task_1", processingLogDao.inserted.getSourceRefId());
        Assert.assertEquals("fund_refresh", processingLogDao.inserted.getModule());
        Assert.assertEquals("provider_fund_failed", processingLogDao.inserted.getEvent());
        Assert.assertEquals("provider failed for one fund", processingLogDao.inserted.getMessage());
        Assert.assertEquals("error", processingLogDao.inserted.getSeverity());
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeSnapshotDao implements IFundDetailSnapshotDao {
        private FundDetailSnapshotPO inserted;

        @Override
        public void insert(FundDetailSnapshotPO fundDetailSnapshotPO) {
            fundDetailSnapshotPO.setId(1L);
            inserted = fundDetailSnapshotPO;
        }

        @Override
        public FundDetailSnapshotPO selectById(Long id) {
            return null;
        }

        @Override
        public List<FundDetailSnapshotPO> selectBySourceRefId(String sourceRefId) {
            return List.of();
        }
    }

    private static class FakeFundDetailItemDao implements IFundDetailItemDao {
        @Override
        public void insert(FundDetailItemPO fundDetailItemPO) {
        }

        @Override
        public FundDetailItemPO selectById(Long id) {
            return null;
        }

        @Override
        public List<FundDetailItemPO> selectBySnapshotId(Long snapshotId) {
            return List.of();
        }

        @Override
        public List<FundDetailItemPO> selectLatestByFundCodes(Collection<String> fundCodes) {
            return List.of();
        }
    }

    private static class FakeFundTopHoldingDao implements IFundTopHoldingDao {
        @Override
        public void insert(FundTopHoldingPO fundTopHoldingPO) {
        }

        @Override
        public FundTopHoldingPO selectById(Long id) {
            return null;
        }

        @Override
        public List<FundTopHoldingPO> selectByFundDetailItemId(Long fundDetailItemId) {
            return List.of();
        }

        @Override
        public List<FundTopHoldingPO> selectBySnapshotId(Long snapshotId) {
            return List.of();
        }

        @Override
        public List<FundTopHoldingPO> selectByStockCode(String stockCode) {
            return List.of();
        }
    }

    private static class FakeProcessingLogDao implements IProcessingLogDao {
        private ProcessingLogPO inserted;

        @Override
        public void insert(ProcessingLogPO processingLogPO) {
            inserted = processingLogPO;
        }

        @Override
        public ProcessingLogPO selectById(Long id) {
            return null;
        }

        @Override
        public List<ProcessingLogPO> selectBySourceRefId(String sourceRefId) {
            return List.of();
        }
    }

}
