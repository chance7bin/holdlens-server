package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingTaskPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Date;

@Mapper
public interface IProcessingTaskDao {

    void insert(ProcessingTaskPO processingTaskPO);

    int update(ProcessingTaskPO processingTaskPO);

    ProcessingTaskPO selectByServerTaskId(@Param("serverTaskId") String serverTaskId);

    ProcessingTaskPO selectByServerTaskIdForUpdate(@Param("serverTaskId") String serverTaskId);

    int countNonTerminalByTaskType(@Param("taskType") String taskType);

    List<ProcessingTaskPO> selectNonTerminalFundSliceTasksUpdatedBefore(@Param("cutoff") Date cutoff);

    int markCallbackFailedIfTimedOut(@Param("serverTaskId") String serverTaskId,
                                     @Param("cutoff") Date cutoff,
                                     @Param("errorSummary") String errorSummary);

}
