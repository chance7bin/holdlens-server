package com.echoamoy.holdlens.server.cases.agent.impl;

import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundDetailSnapshotAggregate;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentFundRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO;
import com.echoamoy.holdlens.server.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AgentFundRefreshCaseImplTest {

    @Test
    public void createAndDispatchDeduplicatesCodesAndMarksRunning() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeAgentPort agentPort = new FakeAgentPort(true, "running");
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort, fundDataRepository);

        FundRefreshTaskResult result = refreshCase.createAndDispatch(FundRefreshCreateCommand.builder()
                .fundCodes(List.of("000001", "000001", " 161725 "))
                .build());
        Assert.assertEquals("running", result.getStatus());
        Assert.assertEquals(2, result.getFundCodeCount().intValue());
        Assert.assertEquals(List.of("000001", "161725"), agentPort.lastCommand.getFundCodes());
        Assert.assertEquals(Boolean.TRUE, agentPort.lastCommand.getAllowNetwork());
    }

    @Test
    public void createAndDispatchMarksDispatchFailedWhenAgentRejects() throws Exception {
        AgentFundRefreshCaseImpl refreshCase = newCase(new FakeProcessingRepository(),
                new FakeAgentPort(false, "rejected"), new FakeFundDataRepository());

        FundRefreshTaskResult result = refreshCase.createAndDispatch(FundRefreshCreateCommand.builder()
                .fundCodes(List.of("000001"))
                .build());

        Assert.assertEquals("dispatch_failed", result.getStatus());
    }

    @Test
    public void handleCallbackPersistsOnlyOnceForDuplicateIdempotencyKey() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"), fundDataRepository);

        ProcessingTaskEntity task = ProcessingTaskEntity.builder()
                .serverTaskId("task_1")
                .taskType(ProcessingTaskEntity.FUND_DETAIL_REFRESH)
                .fundCodeCount(1)
                .sourceType("system")
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build();
        processingRepository.saveTask(task);

        AgentFundRefreshCallbackCommand callback = AgentFundRefreshCallbackCommand.builder()
                .schemaVersion("fund-detail-refresh-result/v1")
                .serverTaskId("task_1")
                .idempotencyKey("task_1:result:1")
                .status("succeeded")
                .generatedAt("2026-06-16T10:00:00Z")
                .funds(List.of(AgentFundRefreshCallbackCommand.FundDetail.builder()
                        .fundCode("000001")
                        .fundName("测试基金")
                        .build()))
                .build();

        FundRefreshTaskResult first = refreshCase.handleCallback(callback);
        FundRefreshTaskResult duplicate = refreshCase.handleCallback(callback);

        Assert.assertEquals("succeeded", first.getStatus());
        Assert.assertEquals("succeeded", duplicate.getStatus());
        Assert.assertEquals(1, fundDataRepository.saveCount);
    }

    @Test
    public void handleCallbackFailedKeepsDiagnosticStatusWithoutSavingFundData() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"), fundDataRepository);
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("task_callback_failed")
                .taskType(ProcessingTaskEntity.FUND_DETAIL_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .fundCodeCount(1)
                .build());

        FundRefreshTaskResult result = refreshCase.handleCallback(AgentFundRefreshCallbackCommand.builder()
                .schemaVersion("fund-detail-refresh-result/v1")
                .serverTaskId("task_callback_failed")
                .idempotencyKey("task_callback_failed:result:1")
                .status("callback_failed")
                .errorSummary("server callback failed after retries")
                .build());

        Assert.assertEquals("callback_failed", result.getStatus());
        Assert.assertEquals("callback_failed", result.getCallbackDiagnosticStatus());
        Assert.assertEquals(0, fundDataRepository.saveCount);
    }

    @Test
    public void handleCallbackRejectsUnsupportedSchemaAndMarksTaskFailed() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"), new FakeFundDataRepository());
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("task_bad_schema")
                .taskType(ProcessingTaskEntity.FUND_DETAIL_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .fundCodeCount(1)
                .build());

        try {
            refreshCase.handleCallback(AgentFundRefreshCallbackCommand.builder()
                    .schemaVersion("unknown/v1")
                    .serverTaskId("task_bad_schema")
                    .idempotencyKey("task_bad_schema:result:1")
                    .status("succeeded")
                    .build());
            Assert.fail("should reject unsupported schema");
        } catch (AppException e) {
            Assert.assertEquals("failed", processingRepository.queryTask("task_bad_schema").getStatus().getCode());
        }
    }

    private AgentFundRefreshCaseImpl newCase(FakeProcessingRepository processingRepository,
                                             FakeAgentPort agentPort,
                                             FakeFundDataRepository fundDataRepository) throws Exception {
        AgentFundRefreshCaseImpl refreshCase = new AgentFundRefreshCaseImpl();
        setField(refreshCase, "processingTaskRepository", processingRepository);
        setField(refreshCase, "agentFundRefreshPort", agentPort);
        setField(refreshCase, "fundDataRepository", fundDataRepository);
        setField(refreshCase, "callbackUrl", "http://server/internal/agent/fund-detail-refresh/callback");
        return refreshCase;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeAgentPort implements IAgentFundRefreshPort {
        private final boolean accepted;
        private final String status;
        private FundRefreshDispatchCommandEntity lastCommand;

        private FakeAgentPort(boolean accepted, String status) {
            this.accepted = accepted;
            this.status = status;
        }

        @Override
        public FundRefreshDispatchResultEntity dispatch(FundRefreshDispatchCommandEntity commandEntity) {
            lastCommand = commandEntity;
            return FundRefreshDispatchResultEntity.builder()
                    .accepted(accepted)
                    .agentStatus(status)
                    .agentTaskRef("agent_task_1")
                    .errorSummary(accepted ? null : "rejected")
                    .build();
        }
    }

    private static class FakeProcessingRepository implements IProcessingTaskRepository {
        private final Map<String, ProcessingTaskEntity> tasks = new HashMap<>();
        private final Set<String> callbacks = new java.util.HashSet<>();

        @Override
        public void saveTask(ProcessingTaskEntity taskEntity) {
            tasks.put(taskEntity.getServerTaskId(), copy(taskEntity));
        }

        @Override
        public void updateTask(ProcessingTaskEntity taskEntity) {
            tasks.put(taskEntity.getServerTaskId(), copy(taskEntity));
        }

        @Override
        public ProcessingTaskEntity queryTask(String serverTaskId) {
            return copy(tasks.get(serverTaskId));
        }

        @Override
        public boolean saveCallbackIfAbsent(ProcessingCallbackEntity callbackEntity) {
            return callbacks.add(callbackEntity.getServerTaskId() + ":" + callbackEntity.getIdempotencyKey());
        }

        @Override
        public void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary) {
        }

        private ProcessingTaskEntity copy(ProcessingTaskEntity task) {
            if (task == null) {
                return null;
            }
            return ProcessingTaskEntity.builder()
                    .id(task.getId())
                    .serverTaskId(task.getServerTaskId())
                    .taskType(task.getTaskType())
                    .fundCodeCount(task.getFundCodeCount())
                    .sourceType(task.getSourceType())
                    .sourceRefId(task.getSourceRefId())
                    .status(task.getStatus())
                    .agentTaskRef(task.getAgentTaskRef())
                    .errorSummary(task.getErrorSummary())
                    .callbackDiagnosticStatus(task.getCallbackDiagnosticStatus())
                    .createTime(task.getCreateTime())
                    .updateTime(task.getUpdateTime())
                    .build();
        }
    }

    private static class FakeFundDataRepository implements IFundDataRepository {
        private int saveCount;

        @Override
        public Long saveSnapshot(FundDetailSnapshotAggregate aggregate) {
            saveCount++;
            Assert.assertEquals("000001", aggregate.getFunds().get(0).getFundCode());
            return 1L;
        }

        @Override
        public Map<String, FundDetailSnapshotAggregate.FundDetail> queryLatestDetails(Set<String> fundCodes) {
            return Map.of();
        }
    }

}
