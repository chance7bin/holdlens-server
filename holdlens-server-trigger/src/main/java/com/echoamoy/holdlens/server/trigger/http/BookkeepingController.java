package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IBookkeepingService;
import com.echoamoy.holdlens.server.api.dto.BookkeepingDTO;
import com.echoamoy.holdlens.server.api.request.BookkeepingRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.bookkeeping.IBookkeepingCase;
import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingCommand;
import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingResult;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingCategoryEnumVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class BookkeepingController implements IBookkeepingService {

    @Resource
    private IBookkeepingCase bookkeepingCase;

    public BookkeepingController() {
    }

    BookkeepingController(IBookkeepingCase bookkeepingCase) {
        this.bookkeepingCase = bookkeepingCase;
    }

    @Override
    @GetMapping("/api/bookkeeping/categories")
    public Response<List<BookkeepingDTO.Category>> queryCategories(
            @RequestParam("userId") Long userId,
            @RequestParam("type") String type
    ) {
        requireUser(userId);
        return Response.ok(bookkeepingCase.queryCategories(type).stream().map(this::toCategory).toList());
    }

    @Override
    @PostMapping("/api/bookkeeping/entries")
    public Response<BookkeepingDTO.Entry> createEntry(
            @Valid @RequestBody BookkeepingRequestDTO.CreateEntryDTO request
    ) {
        BookkeepingCommand.Create command = BookkeepingCommand.Create.builder()
                .userId(request.getUserId())
                .requestId(request.getRequestId())
                .type(request.getType())
                .categoryCode(request.getCategoryCode())
                .amount(request.getAmount())
                .entryDate(request.getEntryDate())
                .note(request.getNote())
                .build();
        return Response.ok(toEntry(bookkeepingCase.create(command)));
    }

    @Override
    @GetMapping("/api/bookkeeping/entries/{entryId}")
    public Response<BookkeepingDTO.Entry> queryEntry(
            @PathVariable("entryId") Long entryId,
            @RequestParam("userId") Long userId
    ) {
        return Response.ok(toEntry(bookkeepingCase.queryEntry(userId, entryId)));
    }

    @Override
    @GetMapping("/api/bookkeeping/entries")
    public Response<BookkeepingDTO.EntryList> queryEntries(
            @RequestParam("userId") Long userId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "categoryCode", required = false) String categoryCode
    ) {
        return Response.ok(toEntryList(bookkeepingCase.queryEntries(
                userId,
                startDate,
                endDate,
                type,
                categoryCode
        )));
    }

    @Override
    @PostMapping("/api/bookkeeping/entries/{entryId}/revise")
    public Response<BookkeepingDTO.Entry> reviseEntry(
            @PathVariable("entryId") Long entryId,
            @Valid @RequestBody BookkeepingRequestDTO.ReviseEntryDTO request
    ) {
        BookkeepingCommand.Revise command = BookkeepingCommand.Revise.builder()
                .userId(request.getUserId())
                .entryId(entryId)
                .type(request.getType())
                .categoryCode(request.getCategoryCode())
                .amount(request.getAmount())
                .entryDate(request.getEntryDate())
                .note(request.getNote())
                .build();
        return Response.ok(toEntry(bookkeepingCase.revise(command)));
    }

    @Override
    @PostMapping("/api/bookkeeping/entries/{entryId}/delete")
    public Response<Void> deleteEntry(
            @PathVariable("entryId") Long entryId,
            @Valid @RequestBody BookkeepingRequestDTO.UserOperationDTO request
    ) {
        bookkeepingCase.delete(request.getUserId(), entryId);
        return Response.ok(null);
    }

    @Override
    @GetMapping("/api/bookkeeping/statistics")
    public Response<BookkeepingDTO.Statistics> statistics(
            @RequestParam("userId") Long userId,
            @RequestParam("type") String type,
            @RequestParam("granularity") String granularity,
            @RequestParam("anchorDate") LocalDate anchorDate
    ) {
        return Response.ok(toStatistics(bookkeepingCase.statistics(userId, type, granularity, anchorDate)));
    }

    @Override
    @GetMapping("/api/bookkeeping/bills/monthly")
    public Response<BookkeepingDTO.MonthlyBill> monthlyBill(
            @RequestParam("userId") Long userId,
            @RequestParam("year") Integer year
    ) {
        return Response.ok(toMonthlyBill(bookkeepingCase.monthlyBill(userId, year)));
    }

    @Override
    @GetMapping("/api/bookkeeping/bills/yearly")
    public Response<BookkeepingDTO.YearlyBill> yearlyBill(@RequestParam("userId") Long userId) {
        return Response.ok(toYearlyBill(bookkeepingCase.yearlyBill(userId)));
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不合法");
        }
    }

    private BookkeepingDTO.Category toCategory(BookkeepingCategoryEnumVO value) {
        return BookkeepingDTO.Category.builder()
                .code(value.name())
                .name(value.getName())
                .type(value.getType().name())
                .sortOrder(value.getSortOrder())
                .build();
    }

    private BookkeepingDTO.Entry toEntry(BookkeepingEntryEntity value) {
        BookkeepingCategoryEnumVO category = BookkeepingCategoryEnumVO.require(value.getCategoryCode());
        return BookkeepingDTO.Entry.builder()
                .id(value.getId())
                .type(value.getType().name())
                .categoryCode(value.getCategoryCode())
                .categoryName(category.getName())
                .amount(value.getAmount())
                .currency(value.getCurrency())
                .entryDate(value.getEntryDate())
                .note(value.getNote())
                .status(value.getStatus().name())
                .createTime(value.getCreateTime())
                .updateTime(value.getUpdateTime())
                .build();
    }

    private BookkeepingDTO.EntryList toEntryList(BookkeepingResult.EntryList value) {
        return BookkeepingDTO.EntryList.builder()
                .startDate(value.getStartDate())
                .endDate(value.getEndDate())
                .income(value.getIncome())
                .expense(value.getExpense())
                .balance(value.getBalance())
                .entries(value.getEntries().stream().map(this::toEntry).toList())
                .build();
    }

    private BookkeepingDTO.Statistics toStatistics(BookkeepingResult.Statistics value) {
        return BookkeepingDTO.Statistics.builder()
                .type(value.getType().name())
                .granularity(value.getGranularity())
                .periodStart(value.getPeriodStart())
                .periodEnd(value.getPeriodEnd())
                .total(value.getTotal())
                .average(value.getAverage())
                .points(value.getPoints().stream().map(point -> BookkeepingDTO.StatisticPoint.builder()
                        .periodStart(point.getPeriodStart())
                        .periodEnd(point.getPeriodEnd())
                        .label(point.getLabel())
                        .amount(point.getAmount())
                        .build()).toList())
                .categories(value.getCategories().stream().map(category -> BookkeepingDTO.CategoryAmount.builder()
                        .categoryCode(category.getCategoryCode())
                        .categoryName(category.getCategoryName())
                        .amount(category.getAmount())
                        .ratio(category.getRatio())
                        .build()).toList())
                .build();
    }

    private BookkeepingDTO.MonthlyBill toMonthlyBill(BookkeepingResult.MonthlyBill value) {
        return BookkeepingDTO.MonthlyBill.builder()
                .year(value.getYear())
                .income(value.getIncome())
                .expense(value.getExpense())
                .balance(value.getBalance())
                .months(value.getMonths().stream().map(month -> BookkeepingDTO.MonthBill.builder()
                        .month(month.getMonth())
                        .income(month.getIncome())
                        .expense(month.getExpense())
                        .balance(month.getBalance())
                        .build()).toList())
                .build();
    }

    private BookkeepingDTO.YearlyBill toYearlyBill(BookkeepingResult.YearlyBill value) {
        return BookkeepingDTO.YearlyBill.builder()
                .income(value.getIncome())
                .expense(value.getExpense())
                .balance(value.getBalance())
                .years(value.getYears().stream().map(year -> BookkeepingDTO.YearBill.builder()
                        .year(year.getYear())
                        .income(year.getIncome())
                        .expense(year.getExpense())
                        .balance(year.getBalance())
                        .build()).toList())
                .build();
    }
}
