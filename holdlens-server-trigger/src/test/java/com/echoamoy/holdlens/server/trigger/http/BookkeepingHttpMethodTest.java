package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.request.BookkeepingRequestDTO;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.time.LocalDate;

public class BookkeepingHttpMethodTest {

    @Test
    public void allNineEndpointsMatchBookkeepingContract() throws Exception {
        Method categories = BookkeepingController.class.getMethod("queryCategories", Long.class, String.class);
        assertGet(categories, "/api/bookkeeping/categories");
        assertRequestParam(categories, 0, "userId", true);
        assertRequestParam(categories, 1, "type", true);

        Method create = BookkeepingController.class.getMethod("createEntry", BookkeepingRequestDTO.CreateEntryDTO.class);
        assertPost(create, "/api/bookkeeping/entries");

        Method detail = BookkeepingController.class.getMethod("queryEntry", Long.class, Long.class);
        assertGet(detail, "/api/bookkeeping/entries/{entryId}");
        assertPath(detail, 0, "entryId");
        assertRequestParam(detail, 1, "userId", true);

        Method entries = BookkeepingController.class.getMethod("queryEntries", Long.class, LocalDate.class, LocalDate.class, String.class, String.class);
        assertGet(entries, "/api/bookkeeping/entries");
        assertRequestParam(entries, 0, "userId", true);
        assertRequestParam(entries, 1, "startDate", true);
        assertRequestParam(entries, 2, "endDate", true);
        assertRequestParam(entries, 3, "type", false);
        assertRequestParam(entries, 4, "categoryCode", false);

        Method revise = BookkeepingController.class.getMethod("reviseEntry", Long.class, BookkeepingRequestDTO.ReviseEntryDTO.class);
        assertPost(revise, "/api/bookkeeping/entries/{entryId}/revise");
        assertPath(revise, 0, "entryId");

        Method delete = BookkeepingController.class.getMethod("deleteEntry", Long.class, BookkeepingRequestDTO.UserOperationDTO.class);
        assertPost(delete, "/api/bookkeeping/entries/{entryId}/delete");
        assertPath(delete, 0, "entryId");

        Method statistics = BookkeepingController.class.getMethod("statistics", Long.class, String.class, String.class, LocalDate.class);
        assertGet(statistics, "/api/bookkeeping/statistics");
        assertRequestParam(statistics, 0, "userId", true);
        assertRequestParam(statistics, 1, "type", true);
        assertRequestParam(statistics, 2, "granularity", true);
        assertRequestParam(statistics, 3, "anchorDate", true);

        Method monthly = BookkeepingController.class.getMethod("monthlyBill", Long.class, Integer.class);
        assertGet(monthly, "/api/bookkeeping/bills/monthly");
        assertRequestParam(monthly, 0, "userId", true);
        assertRequestParam(monthly, 1, "year", true);

        Method yearly = BookkeepingController.class.getMethod("yearlyBill", Long.class);
        assertGet(yearly, "/api/bookkeeping/bills/yearly");
        assertRequestParam(yearly, 0, "userId", true);
    }

    private void assertGet(Method method, String path) {
        Assert.assertArrayEquals(new String[]{path}, method.getAnnotation(GetMapping.class).value());
    }

    private void assertPost(Method method, String path) {
        Assert.assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
    }

    private void assertPath(Method method, int index, String name) {
        Assert.assertEquals(name, method.getParameters()[index].getAnnotation(PathVariable.class).value());
    }

    private void assertRequestParam(Method method, int index, String name, boolean required) {
        RequestParam annotation = method.getParameters()[index].getAnnotation(RequestParam.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(name, annotation.value());
        Assert.assertEquals(required, annotation.required());
    }
}
