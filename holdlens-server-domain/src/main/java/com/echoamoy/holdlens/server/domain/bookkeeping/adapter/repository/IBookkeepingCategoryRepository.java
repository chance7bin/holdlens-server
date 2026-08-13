package com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingCategoryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;

import java.util.List;

public interface IBookkeepingCategoryRepository {

    List<BookkeepingCategoryEntity> queryVisible(Long userId, BookkeepingEntryTypeEnumVO type);

    BookkeepingCategoryEntity queryVisibleByCode(Long userId, String code);

    BookkeepingCategoryEntity queryByOwnerAndRequestId(Long userId, String requestId);

    boolean insertUserCategory(BookkeepingCategoryEntity category);

    void upsertConfig(Long userId, Long categoryId, String status, Integer sortOrder);

    int disableAndDeleteActiveEntries(Long userId, String type, String categoryCode);
}
