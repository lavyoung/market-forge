package com.lavyoung.marketforge.application.strategy.model;

import com.lavyoung.marketforge.types.model.PageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link StrategyDefinitionSearchQuery} 单元测试。
 *
 * <p>验证策略运营查询条件在进入读取端口前完成文本归一化、状态校验和分页参数校验。</p>
 */
class StrategyDefinitionSearchQueryTest {

    @Test
    void shouldNormalizeOptionalTextFilters() {
        // Given / When
        StrategyDefinitionSearchQuery query = new StrategyDefinitionSearchQuery(100001L, "  九宫格策略  ", " active ", new PageRequest(1, 20));

        // Then
        assertEquals(100001L, query.strategyId());
        assertEquals("九宫格策略", query.strategyDesc());
        assertEquals("active", query.state());
    }

    @Test
    void shouldNormalizeBlankFiltersToNull() {
        // Given / When
        StrategyDefinitionSearchQuery query = new StrategyDefinitionSearchQuery(null, "  ", " ", new PageRequest(1, 20));

        // Then
        assertNull(query.strategyDesc());
        assertNull(query.state());
    }

    @Test
    void shouldRejectUnknownState() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinitionSearchQuery(null, null, "archived", new PageRequest(1, 20)));
    }

    @Test
    void shouldRejectMissingPageRequest() {
        // When / Then
        assertThrows(NullPointerException.class, () -> new StrategyDefinitionSearchQuery(null, null, null, null));
    }
}
