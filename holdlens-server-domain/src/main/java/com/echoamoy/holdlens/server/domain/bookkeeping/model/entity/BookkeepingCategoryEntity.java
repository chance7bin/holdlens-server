package com.echoamoy.holdlens.server.domain.bookkeeping.model.entity;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookkeepingCategoryEntity {
    private Long id;
    private String code;
    private String scope;
    private Long ownerUserId;
    private BookkeepingEntryTypeEnumVO type;
    private String name;
    private String iconKey;
    private Boolean defaultEnabled;
    private Integer defaultSortOrder;
    private String createRequestId;
    private String status;
    private Integer sortOrder;
    private Long activeEntryCount;

    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }
}
