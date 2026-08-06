package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.BookkeepingEntryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface IBookkeepingEntryDao {

    BookkeepingEntryPO selectByUserAndRequestId(
            @Param("userId") Long userId,
            @Param("requestId") String requestId
    );

    BookkeepingEntryPO selectActiveByUserAndId(
            @Param("userId") Long userId,
            @Param("id") Long id
    );

    void insert(BookkeepingEntryPO entry);

    int update(BookkeepingEntryPO entry);

    List<BookkeepingEntryPO> selectActiveEntries(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") String type,
            @Param("categoryCode") String categoryCode
    );

    List<Integer> selectActiveYears(@Param("userId") Long userId);
}
