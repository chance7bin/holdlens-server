package com.echoamoy.holdlens.server.domain.processing.adapter.repository;

import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;

public interface IProcessingTaskRepository {

    void saveTask(ProcessingTaskEntity taskEntity);

    void updateTask(ProcessingTaskEntity taskEntity);

    ProcessingTaskEntity queryTask(String serverTaskId);

    boolean saveCallbackIfAbsent(ProcessingCallbackEntity callbackEntity);

    void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary);

}
