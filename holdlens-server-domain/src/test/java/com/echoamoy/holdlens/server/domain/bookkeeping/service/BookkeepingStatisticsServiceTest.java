package com.echoamoy.holdlens.server.domain.bookkeeping.service;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingBillEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingStatisticsEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingGranularityEnumVO;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BookkeepingStatisticsServiceTest {

    private final BookkeepingStatisticsService service = new BookkeepingStatisticsService();

    @Test
    public void monthStatisticsFillDaysAndCalculateCurrentAverageAndCategoryRatio() {
        LocalDate today = LocalDate.of(2026, 8, 6);
        BookkeepingStatisticsService.PeriodRange range = service.period(
                BookkeepingGranularityEnumVO.MONTH,
                today
        );
        BookkeepingStatisticsEntity statistics = service.statistics(
                BookkeepingEntryTypeEnumVO.EXPENSE,
                BookkeepingGranularityEnumVO.MONTH,
                range,
                today,
                List.of(
                        entry(BookkeepingEntryTypeEnumVO.EXPENSE, "FOOD", "30.00", LocalDate.of(2026, 8, 1)),
                        entry(BookkeepingEntryTypeEnumVO.EXPENSE, "TRANSPORT", "30.00", LocalDate.of(2026, 8, 6))
                )
        );

        assertEquals(31, statistics.getPoints().size());
        assertEquals(new BigDecimal("60.00"), statistics.getTotal());
        assertEquals(new BigDecimal("10.00"), statistics.getAverage());
        assertEquals(2, statistics.getCategories().size());
        assertEquals(new BigDecimal("50.00"), statistics.getCategories().get(0).getRatio());
    }

    @Test
    public void yearlyStatisticsFillMonthsAndEmptyCategoriesStayEmpty() {
        LocalDate today = LocalDate.of(2026, 8, 6);
        BookkeepingStatisticsService.PeriodRange range = service.period(
                BookkeepingGranularityEnumVO.YEAR,
                LocalDate.of(2025, 3, 1)
        );
        BookkeepingStatisticsEntity statistics = service.statistics(
                BookkeepingEntryTypeEnumVO.INCOME,
                BookkeepingGranularityEnumVO.YEAR,
                range,
                today,
                List.of()
        );

        assertEquals(12, statistics.getPoints().size());
        assertEquals(new BigDecimal("0.00"), statistics.getAverage());
        assertTrue(statistics.getCategories().isEmpty());
    }

    @Test
    public void monthlyAndYearlyBillsKeepNaturalPeriodOrderingAndTotals() {
        BookkeepingBillEntity.Monthly monthly = service.monthlyBill(
                2026,
                LocalDate.of(2026, 8, 6),
                List.of(
                        entry(BookkeepingEntryTypeEnumVO.INCOME, "SALARY", "100.00", LocalDate.of(2026, 1, 1)),
                        entry(BookkeepingEntryTypeEnumVO.EXPENSE, "FOOD", "25.00", LocalDate.of(2026, 8, 1))
                )
        );
        BookkeepingBillEntity.Year year = service.yearBill(
                2026,
                List.of(
                        entry(BookkeepingEntryTypeEnumVO.INCOME, "SALARY", "100.00", LocalDate.of(2026, 1, 1)),
                        entry(BookkeepingEntryTypeEnumVO.EXPENSE, "FOOD", "25.00", LocalDate.of(2026, 8, 1))
                )
        );
        BookkeepingBillEntity.Yearly yearly = service.yearlyBill(List.of(year));

        assertEquals(8, monthly.getMonths().size());
        assertEquals(Integer.valueOf(8), monthly.getMonths().get(0).getMonth());
        assertEquals(new BigDecimal("75.00"), monthly.getBalance());
        assertEquals(new BigDecimal("75.00"), yearly.getBalance());
    }

    private BookkeepingEntryEntity entry(
            BookkeepingEntryTypeEnumVO type,
            String category,
            String amount,
            LocalDate date
    ) {
        return BookkeepingEntryEntity.builder()
                .type(type)
                .categoryCode(category)
                .amount(new BigDecimal(amount))
                .entryDate(date)
                .build();
    }
}
