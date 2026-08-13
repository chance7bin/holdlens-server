package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingCategoryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.infrastructure.dao.IBookkeepingCategoryDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.BookkeepingCategoryPO;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class BookkeepingCategoryRepositoryTest {

    @Test
    public void concurrentSameRequestReturnsWinner() throws Exception {
        FakeDao dao = new FakeDao();
        dao.winner = persistence("CUS_WINNER", "same-request", "先到", "shopping");
        BookkeepingCategoryRepository repository = repository(dao);
        BookkeepingCategoryEntity candidate = category("same-request", "后到", "food");

        assertFalse(repository.insertUserCategory(candidate));
        assertEquals(Long.valueOf(88L), candidate.getId());
        assertEquals("CUS_WINNER", candidate.getCode());
        assertEquals("先到", candidate.getName());
        assertEquals("shopping", candidate.getIconKey());
    }

    @Test
    public void concurrentDifferentRequestWithSameNameBecomesBusinessDuplicate() throws Exception {
        BookkeepingCategoryRepository repository = repository(new FakeDao());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.insertUserCategory(category("second-request", "重名", "food"))
        );

        assertEquals("收支分类名称重复", exception.getMessage());
    }

    private BookkeepingCategoryRepository repository(IBookkeepingCategoryDao dao) throws Exception {
        BookkeepingCategoryRepository repository = new BookkeepingCategoryRepository();
        Field field = BookkeepingCategoryRepository.class.getDeclaredField("dao");
        field.setAccessible(true);
        field.set(repository, dao);
        return repository;
    }

    private BookkeepingCategoryEntity category(String requestId, String name, String iconKey) {
        return BookkeepingCategoryEntity.builder()
                .code("CUS_CANDIDATE")
                .scope("USER")
                .ownerUserId(1L)
                .type(BookkeepingEntryTypeEnumVO.EXPENSE)
                .name(name)
                .iconKey(iconKey)
                .sortOrder(10)
                .createRequestId(requestId)
                .build();
    }

    private BookkeepingCategoryPO persistence(
            String code,
            String requestId,
            String name,
            String iconKey
    ) {
        BookkeepingCategoryPO persistence = new BookkeepingCategoryPO();
        persistence.setId(88L);
        persistence.setCode(code);
        persistence.setScope("USER");
        persistence.setOwnerUserId(1L);
        persistence.setEntryType("EXPENSE");
        persistence.setName(name);
        persistence.setIconKey(iconKey);
        persistence.setDefaultEnabled(true);
        persistence.setDefaultSortOrder(10);
        persistence.setCreateRequestId(requestId);
        return persistence;
    }

    private static class FakeDao implements IBookkeepingCategoryDao {
        private BookkeepingCategoryPO winner;

        @Override
        public List<BookkeepingCategoryPO> selectVisible(Long userId, String type) {
            return List.of();
        }

        @Override
        public BookkeepingCategoryPO selectVisibleByCode(Long userId, String code) {
            return null;
        }

        @Override
        public BookkeepingCategoryPO selectByOwnerAndRequestId(Long userId, String requestId) {
            return null;
        }

        @Override
        public BookkeepingCategoryPO selectByOwnerAndRequestIdForUpdate(
                Long userId,
                String requestId
        ) {
            return winner != null && requestId.equals(winner.getCreateRequestId()) ? winner : null;
        }

        @Override
        public void insert(BookkeepingCategoryPO category) {
            throw new DuplicateKeyException("duplicate");
        }

        @Override
        public int upsertConfig(Long userId, Long categoryId, String status, Integer sortOrder) {
            return 0;
        }

        @Override
        public int deleteActiveEntries(Long userId, String type, String categoryCode) {
            return 0;
        }
    }
}
