package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.Data;

import java.util.Date;

@Data
public class BookkeepingCategoryPO {

    private Long id;
    private String code;
    private String scope;
    private Long ownerUserId;
    private String entryType;
    private String name;
    private String iconKey;
    private Boolean defaultEnabled;
    private Integer defaultSortOrder;
    private String createRequestId;
    private String status;
    private Integer sortOrder;
    private Long activeEntryCount;
    private Date createTime;
    private Date updateTime;
}
