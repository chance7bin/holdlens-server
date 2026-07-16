package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundAssetAllocationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface IFundAssetAllocationDao {

    void insertBatch(@Param("allocations") List<FundAssetAllocationPO> allocations);

    int deleteByFundCode(@Param("fundCode") String fundCode);

    List<FundAssetAllocationPO> selectByFundCodes(@Param("fundCodes") Collection<String> fundCodes);
}
