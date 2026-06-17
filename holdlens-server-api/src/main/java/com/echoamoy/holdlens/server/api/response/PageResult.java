package com.echoamoy.holdlens.server.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = -716833851917450456L;

    private List<T> items;
    private Long total;
    private Integer pageNo;
    private Integer pageSize;
    private Long pages;

    public static <T> PageResult<T> of(List<T> items, long total, int pageNo, int pageSize) {
        return PageResult.<T>builder()
                .items(items == null ? List.of() : items)
                .total(total)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .pages(pageSize <= 0 ? 0L : (total + pageSize - 1) / pageSize)
                .build();
    }
}
