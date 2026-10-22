package com.lavyoung.marketforge.infrastructure.persistent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ActivityDefinitionSearchParam} 分页边界测试。
 */
class ActivityDefinitionSearchParamTest {

    @Test
    void shouldAcceptMaximumPageSize() {
        // When / Then
        assertDoesNotThrow(() -> new ActivityDefinitionSearchParam(null, null, null, 0, 100));
    }

    @Test
    void shouldRejectNegativeOffset() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new ActivityDefinitionSearchParam(null, null, null, -1, 20));
    }

    @Test
    void shouldRejectPageSizeAboveLimit() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new ActivityDefinitionSearchParam(null, null, null, 0, 101));
    }
}
