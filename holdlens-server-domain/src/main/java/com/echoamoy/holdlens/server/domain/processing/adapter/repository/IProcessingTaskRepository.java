package com.echoamoy.holdlens.server.domain.processing.adapter.repository;

import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingLogEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;

import java.util.List;
import java.time.LocalDateTime;

public interface IProcessingTaskRepository {

    void saveTask(ProcessingTaskEntity taskEntity);

    default boolean saveTaskIfActiveKeyAbsent(ProcessingTaskEntity taskEntity) {
        throw unsupported("saveTaskIfActiveKeyAbsent");
    }

    void updateTask(ProcessingTaskEntity taskEntity);

    default boolean updateTaskIfNonTerminal(ProcessingTaskEntity taskEntity) {
        throw unsupported("updateTaskIfNonTerminal");
    }

    ProcessingTaskEntity queryTask(String serverTaskId);

    default ProcessingTaskEntity queryTaskByActiveKey(String activeKey) { throw unsupported("queryTaskByActiveKey"); }

    default boolean markFailedIfLeaseExpired(String serverTaskId, String activeKey,
                                             LocalDateTime cutoff, String errorSummary) { throw unsupported("markFailedIfLeaseExpired"); }

    default ProcessingTaskEntity queryTaskForUpdate(String serverTaskId) { throw unsupported("queryTaskForUpdate"); }

    boolean existsNonTerminalTask(String taskType);

    boolean saveCallbackIfAbsent(ProcessingCallbackEntity callbackEntity);

    void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary);

    void saveLogs(List<ProcessingLogEntity> logs);

    default List<ProcessingTaskEntity> queryNonTerminalFundSliceTasksUpdatedBefore(LocalDateTime cutoff) { throw unsupported("queryNonTerminalFundSliceTasksUpdatedBefore"); }

    default boolean markCallbackFailedIfTimedOut(String serverTaskId, LocalDateTime cutoff, String errorSummary) {
        throw unsupported("markCallbackFailedIfTimedOut");
    }

    default List<ProcessingCallbackEntity> queryProcessingCatalogCallbacksCreatedBefore(LocalDateTime cutoff) {
        throw unsupported("queryProcessingCatalogCallbacksCreatedBefore");
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException("IProcessingTaskRepository must implement " + operation);
    }

}
