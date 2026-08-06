package com.echoamoy.holdlens.server.cases.bookkeeping.impl;

import com.echoamoy.holdlens.server.cases.bookkeeping.IBookkeepingCase;
import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingCommand;
import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingResult;
import com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository.IBookkeepingRepository;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingBillEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingStatisticsEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingCategoryEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingGranularityEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.service.BookkeepingStatisticsService;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookkeepingCaseImpl implements IBookkeepingCase {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final BookkeepingStatisticsService statisticsService = new BookkeepingStatisticsService();

    @Resource
    private IBookkeepingRepository bookkeepingRepository;

    @Override
    public List<BookkeepingCategoryEnumVO> queryCategories(String type) {
        return BookkeepingCategoryEnumVO.byType(requireType(type));
    }

    @Override
    @Transactional
    public BookkeepingEntryEntity create(BookkeepingCommand.Create command) {
        if (command == null) {
            throw new IllegalArgumentException("收支条目创建参数不能为空");
        }
        BookkeepingEntryEntity.validateUserId(command.getUserId());
        BookkeepingEntryEntity.validateRequestId(command.getRequestId());

        String requestId = command.getRequestId().trim();
        BookkeepingEntryEntity existed = bookkeepingRepository.queryByUserAndRequestId(command.getUserId(), requestId);
        if (existed != null) {
            return existed;
        }

        BookkeepingEntryEntity entry = BookkeepingEntryEntity.create(
                command.getUserId(),
                command.getRequestId(),
                requireType(command.getType()),
                command.getCategoryCode(),
                command.getAmount(),
                command.getEntryDate(),
                command.getNote()
        );
        bookkeepingRepository.insert(entry);
        return queryPersistedEntry(command.getUserId(), requestId, entry);
    }

    @Override
    public BookkeepingEntryEntity queryEntry(Long userId, Long entryId) {
        return requireEntry(userId, entryId);
    }

    @Override
    public BookkeepingResult.EntryList queryEntries(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String type,
            String categoryCode
    ) {
        validateRange(userId, startDate, endDate);
        BookkeepingEntryTypeEnumVO resolvedType = resolveFilter(type, categoryCode);
        List<BookkeepingEntryEntity> entries = bookkeepingRepository.queryActiveEntries(
                userId,
                startDate,
                endDate,
                resolvedType,
                categoryCode
        );
        BookkeepingStatisticsService.Totals totals = statisticsService.totals(entries);
        return BookkeepingResult.EntryList.builder()
                .startDate(startDate)
                .endDate(endDate)
                .income(totals.getIncome())
                .expense(totals.getExpense())
                .balance(totals.getBalance())
                .entries(entries)
                .build();
    }

    @Override
    @Transactional
    public BookkeepingEntryEntity revise(BookkeepingCommand.Revise command) {
        if (command == null) {
            throw new IllegalArgumentException("收支条目修订参数不能为空");
        }
        BookkeepingEntryEntity entry = requireEntry(command.getUserId(), command.getEntryId());
        entry.revise(
                requireType(command.getType()),
                command.getCategoryCode(),
                command.getAmount(),
                command.getEntryDate(),
                command.getNote()
        );
        updateEntry(entry);
        BookkeepingEntryEntity persisted = bookkeepingRepository.queryActiveByUserAndId(
                command.getUserId(),
                command.getEntryId()
        );
        return persisted == null ? entry : persisted;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long entryId) {
        BookkeepingEntryEntity entry = requireEntry(userId, entryId);
        entry.delete();
        updateEntry(entry);
    }

    @Override
    public BookkeepingResult.Statistics statistics(
            Long userId,
            String type,
            String granularity,
            LocalDate anchorDate
    ) {
        BookkeepingEntryEntity.validateUserId(userId);
        BookkeepingEntryTypeEnumVO entryType = requireType(type);
        BookkeepingGranularityEnumVO unit = requireGranularity(granularity);
        if (anchorDate == null) {
            throw new IllegalArgumentException("统计日期不合法");
        }

        LocalDate today = LocalDate.now(ZONE);
        BookkeepingStatisticsService.PeriodRange range = statisticsService.period(unit, anchorDate);
        if (range.start().isAfter(today)) {
            throw new IllegalArgumentException("统计周期尚未开始");
        }
        List<BookkeepingEntryEntity> entries = bookkeepingRepository.queryActiveEntries(
                userId,
                range.start(),
                range.end(),
                entryType,
                null
        );
        return toStatistics(statisticsService.statistics(entryType, unit, range, today, entries));
    }

    @Override
    public BookkeepingResult.MonthlyBill monthlyBill(Long userId, Integer year) {
        BookkeepingEntryEntity.validateUserId(userId);
        LocalDate today = LocalDate.now(ZONE);
        if (year == null || year < 1 || year > today.getYear()) {
            throw new IllegalArgumentException("账单年份不合法");
        }
        List<BookkeepingEntryEntity> entries = bookkeepingRepository.queryActiveEntries(
                userId,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31),
                null,
                null
        );
        return toMonthlyBill(statisticsService.monthlyBill(year, today, entries));
    }

    @Override
    public BookkeepingResult.YearlyBill yearlyBill(Long userId) {
        BookkeepingEntryEntity.validateUserId(userId);
        List<BookkeepingBillEntity.Year> years = new ArrayList<>();
        for (Integer year : bookkeepingRepository.queryActiveYears(userId)) {
            List<BookkeepingEntryEntity> entries = bookkeepingRepository.queryActiveEntries(
                    userId,
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31),
                    null,
                    null
            );
            years.add(statisticsService.yearBill(year, entries));
        }
        return toYearlyBill(statisticsService.yearlyBill(years));
    }

    private BookkeepingEntryEntity queryPersistedEntry(
            Long userId,
            String requestId,
            BookkeepingEntryEntity fallback
    ) {
        BookkeepingEntryEntity persisted = bookkeepingRepository.queryByUserAndRequestId(userId, requestId);
        return persisted == null ? fallback : persisted;
    }

    private BookkeepingEntryEntity requireEntry(Long userId, Long entryId) {
        BookkeepingEntryEntity.validateUserId(userId);
        if (entryId == null || entryId <= 0) {
            throw new IllegalArgumentException("收支条目ID不合法");
        }
        BookkeepingEntryEntity entry = bookkeepingRepository.queryActiveByUserAndId(userId, entryId);
        if (entry == null) {
            throw invisibleEntry();
        }
        return entry;
    }

    private void updateEntry(BookkeepingEntryEntity entry) {
        try {
            bookkeepingRepository.update(entry);
        } catch (IllegalArgumentException exception) {
            throw invisibleEntry();
        }
    }

    private AppException invisibleEntry() {
        return new AppException(
                ResponseCode.ILLEGAL_PARAMETER.getCode(),
                "收支条目不存在或不可见"
        );
    }

    private BookkeepingEntryTypeEnumVO requireType(String type) {
        BookkeepingEntryTypeEnumVO result = BookkeepingEntryTypeEnumVO.from(type);
        if (result == null) {
            throw new IllegalArgumentException("收支类型不合法");
        }
        return result;
    }

    private BookkeepingGranularityEnumVO requireGranularity(String granularity) {
        BookkeepingGranularityEnumVO result = BookkeepingGranularityEnumVO.from(granularity);
        if (result == null) {
            throw new IllegalArgumentException("统计粒度不合法");
        }
        return result;
    }

    private BookkeepingEntryTypeEnumVO resolveFilter(String type, String categoryCode) {
        BookkeepingEntryTypeEnumVO explicitType = type == null ? null : requireType(type);
        if (categoryCode == null) {
            return explicitType;
        }
        BookkeepingCategoryEnumVO category = BookkeepingCategoryEnumVO.require(categoryCode);
        if (explicitType != null && category.getType() != explicitType) {
            throw new IllegalArgumentException("收支分类与类型不匹配");
        }
        return category.getType();
    }

    private void validateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        BookkeepingEntryEntity.validateUserId(userId);
        if (startDate == null || endDate == null
                || startDate.isAfter(endDate)
                || startDate.plusDays(365).isBefore(endDate)) {
            throw new IllegalArgumentException("日期范围不合法");
        }
    }

    private BookkeepingResult.Statistics toStatistics(BookkeepingStatisticsEntity value) {
        return BookkeepingResult.Statistics.builder()
                .type(value.getType())
                .granularity(value.getGranularity().name())
                .periodStart(value.getPeriodStart())
                .periodEnd(value.getPeriodEnd())
                .total(value.getTotal())
                .average(value.getAverage())
                .points(value.getPoints().stream().map(point -> BookkeepingResult.Point.builder()
                        .periodStart(point.getPeriodStart())
                        .periodEnd(point.getPeriodEnd())
                        .label(point.getLabel())
                        .amount(point.getAmount())
                        .build()).toList())
                .categories(value.getCategories().stream().map(category -> BookkeepingResult.Category.builder()
                        .categoryCode(category.getCategoryCode())
                        .categoryName(category.getCategoryName())
                        .amount(category.getAmount())
                        .ratio(category.getRatio())
                        .build()).toList())
                .build();
    }

    private BookkeepingResult.MonthlyBill toMonthlyBill(BookkeepingBillEntity.Monthly value) {
        return BookkeepingResult.MonthlyBill.builder()
                .year(value.getYear())
                .income(value.getIncome())
                .expense(value.getExpense())
                .balance(value.getBalance())
                .months(value.getMonths().stream().map(month -> BookkeepingResult.Month.builder()
                        .month(month.getMonth())
                        .income(month.getIncome())
                        .expense(month.getExpense())
                        .balance(month.getBalance())
                        .build()).toList())
                .build();
    }

    private BookkeepingResult.YearlyBill toYearlyBill(BookkeepingBillEntity.Yearly value) {
        return BookkeepingResult.YearlyBill.builder()
                .income(value.getIncome())
                .expense(value.getExpense())
                .balance(value.getBalance())
                .years(value.getYears().stream().map(year -> BookkeepingResult.Year.builder()
                        .year(year.getYear())
                        .income(year.getIncome())
                        .expense(year.getExpense())
                        .balance(year.getBalance())
                        .build()).toList())
                .build();
    }
}
