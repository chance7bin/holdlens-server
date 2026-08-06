package com.echoamoy.holdlens.server.domain.bookkeeping.model.entity;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingGranularityEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookkeepingStatisticsEntity {

    private BookkeepingEntryTypeEnumVO type;
    private BookkeepingGranularityEnumVO granularity;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal total;
    private BigDecimal average;
    private List<Point> points;
    private List<Category> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String label;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        private String categoryCode;
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal ratio;
    }
}
