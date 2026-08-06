package com.echoamoy.holdlens.server.domain.bookkeeping.service;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingBillEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingStatisticsEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingCategoryEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingGranularityEnumVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记账统计领域规则。统计结果是活动收支条目的派生视图，不单独持久化。
 */
public class BookkeepingStatisticsService {

    public PeriodRange period(BookkeepingGranularityEnumVO unit, LocalDate anchorDate) {
        if (unit == null || anchorDate == null) {
            throw new IllegalArgumentException("统计周期不合法");
        }
        return switch (unit) {
            case WEEK -> new PeriodRange(
                    anchorDate.with(DayOfWeek.MONDAY),
                    anchorDate.with(DayOfWeek.SUNDAY)
            );
            case MONTH -> new PeriodRange(
                    anchorDate.withDayOfMonth(1),
                    anchorDate.with(TemporalAdjusters.lastDayOfMonth())
            );
            case YEAR -> new PeriodRange(
                    anchorDate.withDayOfYear(1),
                    anchorDate.with(TemporalAdjusters.lastDayOfYear())
            );
        };
    }

    public BookkeepingStatisticsEntity statistics(
            BookkeepingEntryTypeEnumVO type,
            BookkeepingGranularityEnumVO unit,
            PeriodRange range,
            LocalDate today,
            List<BookkeepingEntryEntity> entries
    ) {
        List<BookkeepingEntryEntity> safeEntries = entries == null ? List.of() : entries;
        Map<String, BigDecimal> pointAmounts = new HashMap<>();
        Map<String, BigDecimal> categoryAmounts = new HashMap<>();
        for (BookkeepingEntryEntity entry : safeEntries) {
            String pointKey = unit == BookkeepingGranularityEnumVO.YEAR
                    ? String.valueOf(entry.getEntryDate().getMonthValue())
                    : entry.getEntryDate().toString();
            pointAmounts.merge(pointKey, entry.getAmount(), BigDecimal::add);
            categoryAmounts.merge(entry.getCategoryCode(), entry.getAmount(), BigDecimal::add);
        }

        BigDecimal total = safeEntries.stream()
                .map(BookkeepingEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(
                BigDecimal.valueOf(averageDivisor(unit, range, today)),
                2,
                RoundingMode.HALF_UP
        );
        return BookkeepingStatisticsEntity.builder()
                .type(type)
                .granularity(unit)
                .periodStart(range.start())
                .periodEnd(range.end())
                .total(scale(total))
                .average(average)
                .points(points(unit, range, pointAmounts))
                .categories(categories(categoryAmounts, total))
                .build();
    }

    public BookkeepingBillEntity.Monthly monthlyBill(
            Integer year,
            LocalDate today,
            List<BookkeepingEntryEntity> entries
    ) {
        int maxMonth = year == today.getYear() ? today.getMonthValue() : 12;
        Map<Integer, Totals> totalsByMonth = new HashMap<>();
        for (BookkeepingEntryEntity entry : entries) {
            totalsByMonth.computeIfAbsent(entry.getEntryDate().getMonthValue(), unused -> new Totals()).add(entry);
        }

        List<BookkeepingBillEntity.Month> months = new ArrayList<>();
        Totals all = new Totals();
        for (int month = maxMonth; month >= 1; month--) {
            Totals monthTotals = totalsByMonth.getOrDefault(month, new Totals());
            all.add(monthTotals);
            months.add(BookkeepingBillEntity.Month.builder()
                    .month(month)
                    .income(scale(monthTotals.income))
                    .expense(scale(monthTotals.expense))
                    .balance(scale(monthTotals.balance()))
                    .build());
        }
        return BookkeepingBillEntity.Monthly.builder()
                .year(year)
                .income(scale(all.income))
                .expense(scale(all.expense))
                .balance(scale(all.balance()))
                .months(months)
                .build();
    }

    public BookkeepingBillEntity.Year yearBill(Integer year, List<BookkeepingEntryEntity> entries) {
        Totals totals = totals(entries);
        return BookkeepingBillEntity.Year.builder()
                .year(year)
                .income(scale(totals.income))
                .expense(scale(totals.expense))
                .balance(scale(totals.balance()))
                .build();
    }

    public BookkeepingBillEntity.Yearly yearlyBill(List<BookkeepingBillEntity.Year> years) {
        BigDecimal income = years.stream()
                .map(BookkeepingBillEntity.Year::getIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = years.stream()
                .map(BookkeepingBillEntity.Year::getExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return BookkeepingBillEntity.Yearly.builder()
                .income(scale(income))
                .expense(scale(expense))
                .balance(scale(income.subtract(expense)))
                .years(years)
                .build();
    }

    public Totals totals(List<BookkeepingEntryEntity> entries) {
        Totals totals = new Totals();
        if (entries != null) {
            entries.forEach(totals::add);
        }
        return totals;
    }

    private List<BookkeepingStatisticsEntity.Point> points(
            BookkeepingGranularityEnumVO unit,
            PeriodRange range,
            Map<String, BigDecimal> amounts
    ) {
        List<BookkeepingStatisticsEntity.Point> result = new ArrayList<>();
        if (unit == BookkeepingGranularityEnumVO.YEAR) {
            for (int month = 1; month <= 12; month++) {
                LocalDate date = range.start().withMonth(month);
                result.add(BookkeepingStatisticsEntity.Point.builder()
                        .periodStart(date)
                        .periodEnd(date.with(TemporalAdjusters.lastDayOfMonth()))
                        .label(month + "月")
                        .amount(scale(amounts.getOrDefault(String.valueOf(month), BigDecimal.ZERO)))
                        .build());
            }
            return result;
        }

        for (LocalDate date = range.start(); !date.isAfter(range.end()); date = date.plusDays(1)) {
            result.add(BookkeepingStatisticsEntity.Point.builder()
                    .periodStart(date)
                    .periodEnd(date)
                    .label(date.getMonthValue() + "/" + date.getDayOfMonth())
                    .amount(scale(amounts.getOrDefault(date.toString(), BigDecimal.ZERO)))
                    .build());
        }
        return result;
    }

    private List<BookkeepingStatisticsEntity.Category> categories(
            Map<String, BigDecimal> categoryAmounts,
            BigDecimal total
    ) {
        if (total.signum() <= 0) {
            return List.of();
        }
        return categoryAmounts.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(item -> {
                    BookkeepingCategoryEnumVO category = BookkeepingCategoryEnumVO.require(item.getKey());
                    return BookkeepingStatisticsEntity.Category.builder()
                            .categoryCode(item.getKey())
                            .categoryName(category.getName())
                            .amount(scale(item.getValue()))
                            .ratio(item.getValue()
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(total, 2, RoundingMode.HALF_UP))
                            .build();
                })
                .toList();
    }

    private int averageDivisor(BookkeepingGranularityEnumVO unit, PeriodRange range, LocalDate today) {
        if (!today.isBefore(range.start()) && !today.isAfter(range.end())) {
            return unit == BookkeepingGranularityEnumVO.YEAR
                    ? today.getMonthValue()
                    : (int) (today.toEpochDay() - range.start().toEpochDay() + 1);
        }
        return unit == BookkeepingGranularityEnumVO.YEAR
                ? 12
                : (int) (range.end().toEpochDay() - range.start().toEpochDay() + 1);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record PeriodRange(LocalDate start, LocalDate end) {
    }

    public static class Totals {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;

        private void add(BookkeepingEntryEntity entry) {
            if (entry.getType() == BookkeepingEntryTypeEnumVO.INCOME) {
                income = income.add(entry.getAmount());
            } else {
                expense = expense.add(entry.getAmount());
            }
        }

        private void add(Totals value) {
            income = income.add(value.income);
            expense = expense.add(value.expense);
        }

        public BigDecimal getIncome() {
            return scale(income);
        }

        public BigDecimal getExpense() {
            return scale(expense);
        }

        public BigDecimal getBalance() {
            return scale(income.subtract(expense));
        }

        private BigDecimal balance() {
            return income.subtract(expense);
        }
    }
}
