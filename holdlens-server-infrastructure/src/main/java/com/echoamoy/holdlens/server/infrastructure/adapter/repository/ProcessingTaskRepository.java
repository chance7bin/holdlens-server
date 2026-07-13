package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingLogEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingCallbackDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingLogDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingTaskDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingCallbackPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingLogPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingTaskPO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Repository
public class ProcessingTaskRepository implements IProcessingTaskRepository {

    @Resource
    private IProcessingTaskDao processingTaskDao;

    @Resource
    private IProcessingCallbackDao processingCallbackDao;

    @Resource
    private IProcessingLogDao processingLogDao;

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
    public boolean existsNonTerminalTask(String taskType) {
        return processingTaskDao.countNonTerminalByTaskType(taskType) > 0;
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

    @Override
    public void saveLogs(List<ProcessingLogEntity> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        for (ProcessingLogEntity log : logs) {
            processingLogDao.insert(toPO(log));
        }
    }

    @Override
    public List<ProcessingTaskEntity> queryNonTerminalFundSliceTasksUpdatedBefore(LocalDateTime cutoff) {
        if (cutoff == null) {
            return List.of();
        }
        Date value = Date.from(cutoff.atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        return processingTaskDao.selectNonTerminalFundSliceTasksUpdatedBefore(value).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public boolean markCallbackFailedIfTimedOut(String serverTaskId, LocalDateTime cutoff, String errorSummary) {
        if (serverTaskId == null || cutoff == null) {
            return false;
        }
        Date value = Date.from(cutoff.atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        return processingTaskDao.markCallbackFailedIfTimedOut(serverTaskId, value, errorSummary) == 1;
    }

    private ProcessingTaskPO toPO(ProcessingTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProcessingTaskPO.builder()
                .id(entity.getId())
                .serverTaskId(entity.getServerTaskId())
                .taskType(entity.getTaskType())
                .taskParamsJson(entity.getTaskParamsJson())
                .status(entity.getStatus() == null ? null : entity.getStatus().getCode())
                .errorSummary(entity.getErrorSummary())
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
                .taskParamsJson(po.getTaskParamsJson())
                .status(ProcessingTaskStatusEnumVO.fromCode(po.getStatus()))
                .errorSummary(po.getErrorSummary())
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

    private ProcessingLogPO toPO(ProcessingLogEntity entity) {
        return ProcessingLogPO.builder()
                .sourceRefId(entity.getSourceRefId())
                .module(entity.getModule())
                .event(entity.getEvent())
                .message(entity.getMessage())
                .severity(entity.getSeverity())
                .build();
    }

}
