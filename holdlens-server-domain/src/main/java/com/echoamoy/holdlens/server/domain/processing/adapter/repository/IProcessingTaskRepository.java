package com.echoamoy.holdlens.server.domain.processing.adapter.repository;

import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingLogEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;

import java.util.List;
import java.time.LocalDateTime;

public interface IProcessingTaskRepository {

    void saveTask(ProcessingTaskEntity taskEntity);

    default boolean saveTaskIfActiveKeyAbsent(ProcessingTaskEntity taskEntity) {
        saveTask(taskEntity);
        return true;
    }

    void updateTask(ProcessingTaskEntity taskEntity);

    default boolean updateTaskIfNonTerminal(ProcessingTaskEntity taskEntity) {
        updateTask(taskEntity);
        return true;
    }

    ProcessingTaskEntity queryTask(String serverTaskId);

    default ProcessingTaskEntity queryTaskByActiveKey(String activeKey) { return null; }

    default boolean markFailedIfLeaseExpired(String serverTaskId, String activeKey,
                                             LocalDateTime cutoff, String errorSummary) { return false; }

    default ProcessingTaskEntity queryTaskForUpdate(String serverTaskId) { return queryTask(serverTaskId); }

    boolean existsNonTerminalTask(String taskType);

    boolean saveCallbackIfAbsent(ProcessingCallbackEntity callbackEntity);

    void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary);

    void saveLogs(List<ProcessingLogEntity> logs);

    default List<ProcessingTaskEntity> queryNonTerminalFundSliceTasksUpdatedBefore(LocalDateTime cutoff) { return List.of(); }

    default boolean markCallbackFailedIfTimedOut(String serverTaskId, LocalDateTime cutoff, String errorSummary) {
        return false;
    }

    default List<ProcessingCallbackEntity> queryProcessingCatalogCallbacksCreatedBefore(LocalDateTime cutoff) {
        return List.of();
    }

}
