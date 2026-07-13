package com.echoamoy.holdlens.server.domain.processing.adapter.repository;

import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingLogEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;

import java.util.List;
import java.time.LocalDateTime;

public interface IProcessingTaskRepository {

    void saveTask(ProcessingTaskEntity taskEntity);

    void updateTask(ProcessingTaskEntity taskEntity);

    ProcessingTaskEntity queryTask(String serverTaskId);

    boolean existsNonTerminalTask(String taskType);

    boolean saveCallbackIfAbsent(ProcessingCallbackEntity callbackEntity);

    void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary);

    void saveLogs(List<ProcessingLogEntity> logs);

    default List<ProcessingTaskEntity> queryNonTerminalFundSliceTasksUpdatedBefore(LocalDateTime cutoff) { return List.of(); }

    default boolean markCallbackFailedIfTimedOut(String serverTaskId, LocalDateTime cutoff, String errorSummary) {
        return false;
    }

}
