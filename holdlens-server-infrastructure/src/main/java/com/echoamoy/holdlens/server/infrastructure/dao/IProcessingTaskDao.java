package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingTaskPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IProcessingTaskDao {

    void insert(ProcessingTaskPO processingTaskPO);

    int update(ProcessingTaskPO processingTaskPO);

    ProcessingTaskPO selectByServerTaskId(@Param("serverTaskId") String serverTaskId);

    int countNonTerminalByTaskType(@Param("taskType") String taskType);

}
