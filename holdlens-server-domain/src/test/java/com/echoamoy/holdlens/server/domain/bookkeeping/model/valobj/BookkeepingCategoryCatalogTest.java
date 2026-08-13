package com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BookkeepingCategoryCatalogTest {

    @Test
    public void exposesEveryClientIconExactlyOnceAcrossTenGroups() {
        List<String> orderedGroupKeys = List.of(
                "food", "transport", "home", "shopping", "health",
                "entertainment", "education", "social", "income", "other"
        );
        List<String> flattened = orderedGroupKeys.stream()
                .flatMap(key -> BookkeepingCategoryCatalog.GROUPS.get(key).stream())
                .toList();

        assertEquals(68, BookkeepingCategoryCatalog.ICONS.size());
        assertEquals(68, flattened.size());
        assertEquals(new HashSet<>(BookkeepingCategoryCatalog.ICONS), new HashSet<>(flattened));
        assertEquals(flattened.size(), new HashSet<>(flattened).size());
        assertTrue(BookkeepingCategoryCatalog.isIconKey("food"));
        assertTrue(BookkeepingCategoryCatalog.isIconKey("stationery-alt-2"));
        assertFalse(BookkeepingCategoryCatalog.isIconKey("remote-svg"));
    }
}
