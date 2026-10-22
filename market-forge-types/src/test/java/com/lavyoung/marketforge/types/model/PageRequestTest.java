package com.lavyoung.marketforge.types.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link PageRequest} 单元测试。
 *
 * <p>验证公共分页边界和偏移量计算，防止各查询模块重复实现不一致的分页规则。</p>
 */
class PageRequestTest {

    /**
     * 验证第二页的偏移量按照页大小正确计算。
     */
    @Test
    void shouldCalculateOffset() {
        // Given
        PageRequest request = new PageRequest(2, 20);

        // When / Then
        assertEquals(20L, request.offset());
    }

    /**
     * 验证偏移量使用长整型计算，不因较大页码发生整数溢出。
     */
    @Test
    void shouldCalculateLargeOffsetUsingLongArithmetic() {
        // Given
        PageRequest request = new PageRequest(Integer.MAX_VALUE, 100);

        // When / Then
        assertEquals(214_748_364_600L, request.offset());
    }

    /**
     * 验证页码必须从一开始。
     */
    @Test
    void shouldRejectInvalidPageNumber() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 20));
    }

    /**
     * 验证单页查询数量不能超过全局上限。
     */
    @Test
    void shouldRejectPageSizeAboveMaximum() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(1, 101));
    }
}
