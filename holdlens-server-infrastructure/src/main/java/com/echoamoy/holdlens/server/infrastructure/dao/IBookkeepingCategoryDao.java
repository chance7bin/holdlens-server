package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.BookkeepingCategoryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IBookkeepingCategoryDao {

    List<BookkeepingCategoryPO> selectVisible(
            @Param("userId") Long userId,
            @Param("type") String type
    );

    BookkeepingCategoryPO selectVisibleByCode(
            @Param("userId") Long userId,
            @Param("code") String code
    );

    BookkeepingCategoryPO selectByOwnerAndRequestId(
            @Param("userId") Long userId,
            @Param("requestId") String requestId
    );

    BookkeepingCategoryPO selectByOwnerAndRequestIdForUpdate(
            @Param("userId") Long userId,
            @Param("requestId") String requestId
    );

    void insert(BookkeepingCategoryPO category);

    int upsertConfig(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("sortOrder") Integer sortOrder
    );

    int deleteActiveEntries(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("categoryCode") String categoryCode
    );
}
