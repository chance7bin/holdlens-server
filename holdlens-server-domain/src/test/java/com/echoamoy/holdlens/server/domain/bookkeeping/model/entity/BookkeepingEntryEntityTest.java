package com.echoamoy.holdlens.server.domain.bookkeeping.model.entity;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

public class BookkeepingEntryEntityTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    public void createsAndNormalizesEntry() {
        BookkeepingEntryEntity entry = BookkeepingEntryEntity.create(
                1L,
                " req ",
                BookkeepingEntryTypeEnumVO.EXPENSE,
                "FOOD",
                new BigDecimal("1.20"),
                LocalDate.now(ZONE),
                " note "
        );

        assertEquals("req", entry.getRequestId());
        assertEquals("note", entry.getNote());
        assertEquals("CNY", entry.getCurrency());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsFutureDate() {
        BookkeepingEntryEntity.create(
                1L,
                "r",
                BookkeepingEntryTypeEnumVO.EXPENSE,
                "FOOD",
                BigDecimal.ONE,
                LocalDate.now(ZONE).plusDays(1),
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidCategoryCode() {
        BookkeepingEntryEntity.create(
                1L,
                "r",
                BookkeepingEntryTypeEnumVO.EXPENSE,
                " ",
                BigDecimal.ONE,
                LocalDate.now(ZONE),
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAmountBeyondDatabaseBoundary() {
        BookkeepingEntryEntity.create(
                1L,
                "r",
                BookkeepingEntryTypeEnumVO.EXPENSE,
                "FOOD",
                new BigDecimal("1000000000000000000.00"),
                LocalDate.now(ZONE),
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsScientificNotationBeyondDatabaseBoundary() {
        BookkeepingEntryEntity.create(
                1L,
                "r",
                BookkeepingEntryTypeEnumVO.EXPENSE,
                "FOOD",
                new BigDecimal("1E+18"),
                LocalDate.now(ZONE),
                null
        );
    }
}
