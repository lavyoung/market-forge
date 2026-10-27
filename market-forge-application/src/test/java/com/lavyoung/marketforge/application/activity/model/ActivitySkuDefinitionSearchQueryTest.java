package com.lavyoung.marketforge.application.activity.model;

import com.lavyoung.marketforge.types.model.PageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 活动 SKU 查询条件边界测试。
 */
class ActivitySkuDefinitionSearchQueryTest {

    @Test
    void shouldAcceptEmptyOptionalFilters() {
        assertDoesNotThrow(() -> new ActivitySkuDefinitionSearchQuery(null, null, null, new PageRequest(1, 20)));
    }

    @Test
    void shouldRejectNonPositiveFilterIdentifier() {
        assertThrows(IllegalArgumentException.class,
                () -> new ActivitySkuDefinitionSearchQuery(0L, null, null, new PageRequest(1, 20)));
    }

    @Test
    void shouldRejectMissingPageWindow() {
        assertThrows(NullPointerException.class,
                () -> new ActivitySkuDefinitionSearchQuery(900001L, null, null, null));
    }
}
