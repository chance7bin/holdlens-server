package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingCallbackDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingTaskDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingCallbackPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingTaskPO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;

@Repository
public class ProcessingTaskRepository implements IProcessingTaskRepository {

    @Resource
    private IProcessingTaskDao processingTaskDao;

    @Resource
    private IProcessingCallbackDao processingCallbackDao;

    @Override
    public void saveTask(ProcessingTaskEntity taskEntity) {
        processingTaskDao.insert(toPO(taskEntity));
    }

    @Override
    public void updateTask(ProcessingTaskEntity taskEntity) {
        processingTaskDao.update(toPO(taskEntity));
    }

    @Override
    public ProcessingTaskEntity queryTask(String serverTaskId) {
        return toEntity(processingTaskDao.selectByServerTaskId(serverTaskId));
    }

    @Override
    public boolean saveCallbackIfAbsent(ProcessingCallbackEntity callbackEntity) {
        try {
            processingCallbackDao.insert(toPO(callbackEntity));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary) {
        processingCallbackDao.updateProcessStatus(serverTaskId, idempotencyKey, processStatus, errorSummary);
    }

    private ProcessingTaskPO toPO(ProcessingTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProcessingTaskPO.builder()
                .id(entity.getId())
                .serverTaskId(entity.getServerTaskId())
                .taskType(entity.getTaskType())
                .fundCodeCount(entity.getFundCodeCount())
                .sourceType(entity.getSourceType())
                .sourceRefId(entity.getSourceRefId())
                .status(entity.getStatus() == null ? null : entity.getStatus().getCode())
                .agentTaskRef(entity.getAgentTaskRef())
                .errorSummary(entity.getErrorSummary())
                .callbackDiagnosticStatus(entity.getCallbackDiagnosticStatus())
                .build();
    }

    private ProcessingTaskEntity toEntity(ProcessingTaskPO po) {
        if (po == null) {
            return null;
        }
        return ProcessingTaskEntity.builder()
                .id(po.getId())
                .serverTaskId(po.getServerTaskId())
                .taskType(po.getTaskType())
                .fundCodeCount(po.getFundCodeCount())
                .sourceType(po.getSourceType())
                .sourceRefId(po.getSourceRefId())
                .status(ProcessingTaskStatusEnumVO.fromCode(po.getStatus()))
                .agentTaskRef(po.getAgentTaskRef())
                .errorSummary(po.getErrorSummary())
                .callbackDiagnosticStatus(po.getCallbackDiagnosticStatus())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    private ProcessingCallbackPO toPO(ProcessingCallbackEntity entity) {
        return ProcessingCallbackPO.builder()
                .id(entity.getId())
                .serverTaskId(entity.getServerTaskId())
                .idempotencyKey(entity.getIdempotencyKey())
                .callbackStatus(entity.getCallbackStatus())
                .processStatus(entity.getProcessStatus())
                .errorSummary(entity.getErrorSummary())
                .build();
    }

}
