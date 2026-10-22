package com.lavyoung.marketforge.infrastructure.persistent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AwardDefinitionSearchParam} 单元测试。
 *
 * <p>验证 MyBatis 分页参数在进入 SQL 执行前满足偏移量和单页容量约束。</p>
 */
class AwardDefinitionSearchParamTest {

    /**
     * 验证零偏移量和最大页大小属于合法边界。
     */
    @Test
    void shouldAcceptValidBoundary() {
        // Given / When / Then
        assertDoesNotThrow(() -> new AwardDefinitionSearchParam(null, null, null, null, 0, 100));
    }

    /**
     * 验证负偏移量不会进入分页 SQL。
     */
    @Test
    void shouldRejectNegativeOffset() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new AwardDefinitionSearchParam(null, null, null, null, -1, 20));
    }

    /**
     * 验证每页记录数不能超过公共分页上限。
     */
    @Test
    void shouldRejectPageSizeAboveMaximum() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new AwardDefinitionSearchParam(null, null, null, null, 0, 101));
    }
}
