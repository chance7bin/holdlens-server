package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.BookkeepingDTO;
import com.echoamoy.holdlens.server.api.request.BookkeepingRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;

import java.time.LocalDate;
import java.util.List;

public interface IBookkeepingService {

    Response<List<BookkeepingDTO.Category>> queryCategories(Long userId, String type);

    Response<BookkeepingDTO.Entry> createEntry(BookkeepingRequestDTO.CreateEntryDTO request);

    Response<BookkeepingDTO.Entry> queryEntry(Long entryId, Long userId);

    Response<BookkeepingDTO.EntryList> queryEntries(Long userId, LocalDate startDate, LocalDate endDate, String type, String categoryCode);

    Response<BookkeepingDTO.Entry> reviseEntry(Long entryId, BookkeepingRequestDTO.ReviseEntryDTO request);

    Response<Void> deleteEntry(Long entryId, BookkeepingRequestDTO.UserOperationDTO request);

    Response<BookkeepingDTO.Statistics> statistics(Long userId, String type, String granularity, LocalDate anchorDate);

    Response<BookkeepingDTO.MonthlyBill> monthlyBill(Long userId, Integer year);

    Response<BookkeepingDTO.YearlyBill> yearlyBill(Long userId);
}
