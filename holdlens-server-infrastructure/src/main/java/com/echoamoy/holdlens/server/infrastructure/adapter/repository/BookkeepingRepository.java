package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository.IBookkeepingRepository;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryStatusEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.infrastructure.dao.IBookkeepingEntryDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.BookkeepingEntryPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Repository
public class BookkeepingRepository implements IBookkeepingRepository {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private IBookkeepingEntryDao bookkeepingEntryDao;

    @Override
    public BookkeepingEntryEntity queryByUserAndRequestId(Long userId, String requestId) {
        return toEntity(bookkeepingEntryDao.selectByUserAndRequestId(userId, requestId));
    }

    @Override
    public BookkeepingEntryEntity queryActiveByUserAndId(Long userId, Long id) {
        return toEntity(bookkeepingEntryDao.selectActiveByUserAndId(userId, id));
    }

    @Override
    public void insert(BookkeepingEntryEntity entry) {
        BookkeepingEntryPO po = toPO(entry);
        bookkeepingEntryDao.insert(po);
        entry.setId(po.getId());
    }

    @Override
    public void update(BookkeepingEntryEntity entry) {
        if (bookkeepingEntryDao.update(toPO(entry)) != 1) {
            throw new IllegalArgumentException("收支条目不存在或不可见");
        }
    }

    @Override
    public List<BookkeepingEntryEntity> queryActiveEntries(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            BookkeepingEntryTypeEnumVO type,
            String categoryCode
    ) {
        return bookkeepingEntryDao.selectActiveEntries(
                        userId,
                        startDate,
                        endDate,
                        type == null ? null : type.name(),
                        categoryCode
                ).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<Integer> queryActiveYears(Long userId) {
        return bookkeepingEntryDao.selectActiveYears(userId);
    }

    private BookkeepingEntryPO toPO(BookkeepingEntryEntity entry) {
        return BookkeepingEntryPO.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .requestId(entry.getRequestId())
                .type(entry.getType().name())
                .categoryCode(entry.getCategoryCode())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .entryDate(entry.getEntryDate())
                .note(entry.getNote())
                .status(entry.getStatus().name())
                .build();
    }

    private BookkeepingEntryEntity toEntity(BookkeepingEntryPO po) {
        if (po == null) {
            return null;
        }
        return BookkeepingEntryEntity.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .requestId(po.getRequestId())
                .type(BookkeepingEntryTypeEnumVO.from(po.getType()))
                .categoryCode(po.getCategoryCode())
                .amount(po.getAmount())
                .currency(po.getCurrency())
                .entryDate(po.getEntryDate())
                .note(po.getNote())
                .status(BookkeepingEntryStatusEnumVO.valueOf(po.getStatus()))
                .createTime(toLocalDateTime(po.getCreateTime()))
                .updateTime(toLocalDateTime(po.getUpdateTime()))
                .build();
    }

    private LocalDateTime toLocalDateTime(Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZONE);
    }
}
