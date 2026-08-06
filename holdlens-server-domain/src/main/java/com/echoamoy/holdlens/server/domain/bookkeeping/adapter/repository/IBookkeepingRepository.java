package com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;

import java.time.LocalDate;
import java.util.List;

public interface IBookkeepingRepository {

    BookkeepingEntryEntity queryByUserAndRequestId(Long userId, String requestId);

    BookkeepingEntryEntity queryActiveByUserAndId(Long userId, Long id);

    void insert(BookkeepingEntryEntity entry);

    void update(BookkeepingEntryEntity entry);

    List<BookkeepingEntryEntity> queryActiveEntries(Long userId, LocalDate startDate, LocalDate endDate, BookkeepingEntryTypeEnumVO type, String categoryCode);

    List<Integer> queryActiveYears(Long userId);
}
