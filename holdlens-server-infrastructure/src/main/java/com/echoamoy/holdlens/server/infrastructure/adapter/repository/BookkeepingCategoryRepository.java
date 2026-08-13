package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository.IBookkeepingCategoryRepository;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingCategoryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.infrastructure.dao.IBookkeepingCategoryDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.BookkeepingCategoryPO;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BookkeepingCategoryRepository implements IBookkeepingCategoryRepository {

    @Resource
    private IBookkeepingCategoryDao dao;

    @Override
    public List<BookkeepingCategoryEntity> queryVisible(
            Long userId,
            BookkeepingEntryTypeEnumVO type
    ) {
        return dao.selectVisible(userId, type.name()).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public BookkeepingCategoryEntity queryVisibleByCode(Long userId, String code) {
        return toEntity(dao.selectVisibleByCode(userId, code));
    }

    @Override
    public BookkeepingCategoryEntity queryByOwnerAndRequestId(Long userId, String requestId) {
        return toEntity(dao.selectByOwnerAndRequestId(userId, requestId));
    }

    @Override
    public boolean insertUserCategory(BookkeepingCategoryEntity category) {
        BookkeepingCategoryPO persistence = toPersistence(category);
        try {
            dao.insert(persistence);
            category.setId(persistence.getId());
            return true;
        } catch (DuplicateKeyException exception) {
            BookkeepingCategoryPO existing = dao.selectByOwnerAndRequestIdForUpdate(
                    category.getOwnerUserId(),
                    category.getCreateRequestId()
            );
            if (existing == null) {
                throw new IllegalArgumentException("收支分类名称重复");
            }
            copy(toEntity(existing), category);
            category.setStatus("ENABLED");
            category.setSortOrder(category.getDefaultSortOrder());
            category.setActiveEntryCount(0L);
            return false;
        }
    }

    @Override
    public void upsertConfig(Long userId, Long categoryId, String status, Integer sortOrder) {
        dao.upsertConfig(userId, categoryId, status, sortOrder);
    }

    @Override
    public int disableAndDeleteActiveEntries(Long userId, String type, String categoryCode) {
        return dao.deleteActiveEntries(userId, type, categoryCode);
    }

    private BookkeepingCategoryPO toPersistence(BookkeepingCategoryEntity category) {
        BookkeepingCategoryPO persistence = new BookkeepingCategoryPO();
        persistence.setCode(category.getCode());
        persistence.setScope("USER");
        persistence.setOwnerUserId(category.getOwnerUserId());
        persistence.setEntryType(category.getType().name());
        persistence.setName(category.getName());
        persistence.setIconKey(category.getIconKey());
        persistence.setDefaultEnabled(true);
        persistence.setDefaultSortOrder(category.getSortOrder());
        persistence.setCreateRequestId(category.getCreateRequestId());
        return persistence;
    }

    private BookkeepingCategoryEntity toEntity(BookkeepingCategoryPO persistence) {
        if (persistence == null) {
            return null;
        }
        return BookkeepingCategoryEntity.builder()
                .id(persistence.getId())
                .code(persistence.getCode())
                .scope(persistence.getScope())
                .ownerUserId(persistence.getOwnerUserId())
                .type(BookkeepingEntryTypeEnumVO.from(persistence.getEntryType()))
                .name(persistence.getName())
                .iconKey(persistence.getIconKey())
                .defaultEnabled(persistence.getDefaultEnabled())
                .defaultSortOrder(persistence.getDefaultSortOrder())
                .createRequestId(persistence.getCreateRequestId())
                .status(persistence.getStatus())
                .sortOrder(persistence.getSortOrder())
                .activeEntryCount(persistence.getActiveEntryCount())
                .build();
    }

    private void copy(BookkeepingCategoryEntity source, BookkeepingCategoryEntity target) {
        target.setId(source.getId());
        target.setCode(source.getCode());
        target.setScope(source.getScope());
        target.setOwnerUserId(source.getOwnerUserId());
        target.setType(source.getType());
        target.setName(source.getName());
        target.setIconKey(source.getIconKey());
        target.setDefaultEnabled(source.getDefaultEnabled());
        target.setDefaultSortOrder(source.getDefaultSortOrder());
        target.setCreateRequestId(source.getCreateRequestId());
    }
}
