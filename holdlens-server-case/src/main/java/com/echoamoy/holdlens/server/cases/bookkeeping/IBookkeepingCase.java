package com.echoamoy.holdlens.server.cases.bookkeeping;

import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingCommand;
import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingResult;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingCategoryEnumVO;

import java.time.LocalDate;
import java.util.List;

public interface IBookkeepingCase {

    List<BookkeepingCategoryEnumVO> queryCategories(String type);

    BookkeepingEntryEntity create(BookkeepingCommand.Create command);

    BookkeepingEntryEntity queryEntry(Long userId, Long entryId);

    BookkeepingResult.EntryList queryEntries(Long userId, LocalDate startDate, LocalDate endDate, String type, String categoryCode);

    BookkeepingEntryEntity revise(BookkeepingCommand.Revise command);

    void delete(Long userId, Long entryId);

    BookkeepingResult.Statistics statistics(Long userId, String type, String granularity, LocalDate anchorDate);

    BookkeepingResult.MonthlyBill monthlyBill(Long userId, Integer year);

    BookkeepingResult.YearlyBill yearlyBill(Long userId);
}
