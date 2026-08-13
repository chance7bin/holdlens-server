package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.request.BookkeepingRequestDTO;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;

public class BookkeepingCategoryHttpMethodTest {
    @Test
    public void categoryManagementUsesReadOnlyGetAndExplicitPostActions() throws Exception {
        assertPath(
                BookkeepingController.class.getMethod("queryCategories", Long.class, String.class),
                GetMapping.class,
                "/api/bookkeeping/categories"
        );
        assertPath(
                BookkeepingController.class.getMethod("queryCategorySettings", Long.class, String.class),
                GetMapping.class,
                "/api/bookkeeping/category-settings"
        );
        assertPath(
                BookkeepingController.class.getMethod("queryCategoryIcons"),
                GetMapping.class,
                "/api/bookkeeping/category-icons"
        );
        assertPath(
                BookkeepingController.class.getMethod(
                        "createCategory",
                        BookkeepingRequestDTO.CreateCategoryDTO.class
                ),
                PostMapping.class,
                "/api/bookkeeping/categories"
        );
        assertPath(
                BookkeepingController.class.getMethod(
                        "enableCategory",
                        String.class,
                        BookkeepingRequestDTO.CategoryOperationDTO.class
                ),
                PostMapping.class,
                "/api/bookkeeping/categories/{categoryCode}/enable"
        );
        assertPath(
                BookkeepingController.class.getMethod(
                        "disableCategory",
                        String.class,
                        BookkeepingRequestDTO.CategoryOperationDTO.class
                ),
                PostMapping.class,
                "/api/bookkeeping/categories/{categoryCode}/disable"
        );
        assertPath(
                BookkeepingController.class.getMethod(
                        "reorderCategories",
                        BookkeepingRequestDTO.ReorderCategoriesDTO.class
                ),
                PostMapping.class,
                "/api/bookkeeping/categories/reorder"
        );
    }

    private void assertPath(Method method, Class<?> annotation, String path) {
        if (annotation == GetMapping.class) {
            assertArrayEquals(new String[]{path}, method.getAnnotation(GetMapping.class).value());
        } else {
            assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
        }
    }
}
