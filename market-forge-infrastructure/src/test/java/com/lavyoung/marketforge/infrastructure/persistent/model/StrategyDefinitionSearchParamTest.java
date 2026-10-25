package com.lavyoung.marketforge.infrastructure.persistent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link StrategyDefinitionSearchParam} 单元测试。
 *
 * <p>验证数据库分页窗口不会接收负偏移量、空页或超过项目上限的页大小。</p>
 */
class StrategyDefinitionSearchParamTest {

    @Test
    void shouldAcceptValidPageWindow() {
        // Given / When
        StrategyDefinitionSearchParam param = new StrategyDefinitionSearchParam(100001L, "九宫格", "active", 20L, 20);

        // Then
        assertEquals(20L, param.offset());
        assertEquals(20, param.pageSize());
    }

    @Test
    void shouldRejectNegativeOffset() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinitionSearchParam(null, null, null, -1L, 20));
    }

    @Test
    void shouldRejectPageSizeOutsideLimit() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinitionSearchParam(null, null, null, 0L, 0));
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinitionSearchParam(null, null, null, 0L, 101));
    }
}
