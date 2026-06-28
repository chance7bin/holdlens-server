package com.echoamoy.holdlens.server.trigger.job;

import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AgentStockQuoteRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.StockQuoteRefreshCreateCommand;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.funddata.model.entity.FundRefreshTargetEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteEntity;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AgentRefreshScheduleJobTest {

    @Test
    public void disabledFundScheduleDoesNotScan() throws Exception {
        FakeAgentFundRefreshCase refreshCase = new FakeAgentFundRefreshCase();
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository(List.of(
                FundRefreshTargetEntity.builder().id(1L).fundCode("000001").build()));
        AgentRefreshScheduleJob job = newJob(refreshCase, fundDataRepository, new FakeStockMarketRepository(List.of()));
        setField(job, "fundRefreshScheduleEnabled", false);

        job.runFundRefreshSchedule();

        Assert.assertEquals(0, fundDataRepository.queryCount);
        Assert.assertTrue(refreshCase.fundCommands.isEmpty());
    }

    @Test
    public void fundScheduleSkipsWhenSameTypeTaskIsActive() throws Exception {
        FakeAgentFundRefreshCase refreshCase = new FakeAgentFundRefreshCase();
        refreshCase.activeTaskTypes.add(ProcessingTaskEntity.FUND_DETAIL_REFRESH);
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository(List.of(
                FundRefreshTargetEntity.builder().id(1L).fundCode("000001").build()));
        AgentRefreshScheduleJob job = newJob(refreshCase, fundDataRepository, new FakeStockMarketRepository(List.of()));
        setField(job, "fundRefreshScheduleEnabled", true);
        setField(job, "fundRefreshBatchSize", 20);

        job.runFundRefreshSchedule();

        Assert.assertEquals(0, fundDataRepository.queryCount);
        Assert.assertTrue(refreshCase.fundCommands.isEmpty());
    }

    @Test
    public void fundScheduleScansAllPagesAndCreatesBatches() throws Exception {
        FakeAgentFundRefreshCase refreshCase = new FakeAgentFundRefreshCase();
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository(List.of(
                FundRefreshTargetEntity.builder().id(1L).fundCode("000001").build(),
                FundRefreshTargetEntity.builder().id(2L).fundCode("000002").build(),
                FundRefreshTargetEntity.builder().id(3L).fundCode("000003").build()));
        AgentRefreshScheduleJob job = newJob(refreshCase, fundDataRepository, new FakeStockMarketRepository(List.of()));
        setField(job, "fundRefreshScheduleEnabled", true);
        setField(job, "fundRefreshBatchSize", 2);

        job.runFundRefreshSchedule();

        Assert.assertEquals(3, fundDataRepository.queryCount);
        Assert.assertEquals(2, refreshCase.fundCommands.size());
        Assert.assertEquals(List.of("000001", "000002"), refreshCase.fundCommands.get(0).getFundCodes());
        Assert.assertEquals(List.of("000003"), refreshCase.fundCommands.get(1).getFundCodes());
        Assert.assertEquals("schedule", refreshCase.fundCommands.get(0).getTrigger());
    }

    @Test
    public void stockScheduleStopsWhenBatchStatusIsAbnormal() throws Exception {
        FakeAgentFundRefreshCase refreshCase = new FakeAgentFundRefreshCase();
        refreshCase.nextStockStatuses.add("dispatch_failed");
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository(List.of(
                StockQuoteTargetEntity.builder().id(1L).stockCode("600000").market("1").build(),
                StockQuoteTargetEntity.builder().id(2L).stockCode("000001").market("0").build(),
                StockQuoteTargetEntity.builder().id(3L).stockCode("000002").market("0").build()));
        AgentRefreshScheduleJob job = newJob(refreshCase, new FakeFundDataRepository(List.of()), stockMarketRepository);
        setField(job, "stockRefreshScheduleEnabled", true);
        setField(job, "stockRefreshBatchSize", 2);

        job.runStockRefreshSchedule();

        Assert.assertEquals(1, stockMarketRepository.queryCount);
        Assert.assertEquals(1, refreshCase.stockCommands.size());
        Assert.assertEquals(2, refreshCase.stockCommands.get(0).getStocks().size());
    }

    private AgentRefreshScheduleJob newJob(FakeAgentFundRefreshCase refreshCase,
                                           FakeFundDataRepository fundDataRepository,
                                           FakeStockMarketRepository stockMarketRepository) throws Exception {
        AgentRefreshScheduleJob job = new AgentRefreshScheduleJob();
        setField(job, "agentFundRefreshCase", refreshCase);
        setField(job, "fundDataRepository", fundDataRepository);
        setField(job, "stockMarketRepository", stockMarketRepository);
        return job;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeAgentFundRefreshCase implements IAgentFundRefreshCase {
        private final Set<String> activeTaskTypes = new java.util.HashSet<>();
        private final List<FundRefreshCreateCommand> fundCommands = new ArrayList<>();
        private final List<StockQuoteRefreshCreateCommand> stockCommands = new ArrayList<>();
        private final List<String> nextFundStatuses = new ArrayList<>();
        private final List<String> nextStockStatuses = new ArrayList<>();

        @Override
        public FundRefreshTaskResult createAndDispatch(FundRefreshCreateCommand command) {
            fundCommands.add(command);
            return result(commandStatus(nextFundStatuses), ProcessingTaskEntity.FUND_DETAIL_REFRESH, fundCommands.size());
        }

        @Override
        public FundRefreshTaskResult handleCallback(AgentFundRefreshCallbackCommand command) {
            return null;
        }

        @Override
        public FundRefreshTaskResult queryTask(String serverTaskId) {
            return null;
        }

        @Override
        public FundRefreshTaskResult createAndDispatchStockQuotes() {
            return createAndDispatchStockQuotes(null);
        }

        @Override
        public FundRefreshTaskResult createAndDispatchStockQuotes(StockQuoteRefreshCreateCommand command) {
            stockCommands.add(command);
            return result(commandStatus(nextStockStatuses), ProcessingTaskEntity.STOCK_QUOTE_REFRESH, stockCommands.size());
        }

        @Override
        public boolean hasNonTerminalTask(String taskType) {
            return activeTaskTypes.contains(taskType);
        }

        @Override
        public FundRefreshTaskResult handleStockQuoteCallback(AgentStockQuoteRefreshCallbackCommand command) {
            return null;
        }

        private String commandStatus(List<String> statuses) {
            if (statuses.isEmpty()) {
                return "running";
            }
            return statuses.remove(0);
        }

        private FundRefreshTaskResult result(String status, String taskType, int index) {
            return FundRefreshTaskResult.builder()
                    .serverTaskId(taskType + "_" + index)
                    .taskType(taskType)
                    .status(status)
                    .errorSummary("dispatch_failed".equals(status) ? "agent rejected" : null)
                    .build();
        }
    }

    private static class FakeFundDataRepository implements IFundDataRepository {
        private final List<FundRefreshTargetEntity> targets;
        private int queryCount;

        private FakeFundDataRepository(List<FundRefreshTargetEntity> targets) {
            this.targets = targets;
        }

        @Override
        public void saveCurrentData(FundCurrentDataAggregate aggregate) {
        }

        @Override
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            return Map.of();
        }

        @Override
        public List<FundRefreshTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit) {
            queryCount++;
            return targets.stream()
                    .filter(target -> target.getId() > lastId)
                    .limit(limit)
                    .toList();
        }
    }

    private static class FakeStockMarketRepository implements IStockMarketRepository {
        private final List<StockQuoteTargetEntity> targets;
        private int queryCount;

        private FakeStockMarketRepository(List<StockQuoteTargetEntity> targets) {
            this.targets = targets;
        }

        @Override
        public List<StockQuoteTargetEntity> queryAllQuoteTargets() {
            return targets;
        }

        @Override
        public List<StockQuoteTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit) {
            queryCount++;
            return targets.stream()
                    .filter(target -> target.getId() > lastId)
                    .limit(limit)
                    .toList();
        }

        @Override
        public void registerQuoteTargets(List<StockQuoteEntity> quoteTargets) {
        }

        @Override
        public void upsertQuotes(List<StockQuoteEntity> quotes) {
        }

        @Override
        public Map<String, StockQuoteEntity> queryByStockKeys(Collection<String> stockKeys) {
            return Map.of();
        }
    }

}
