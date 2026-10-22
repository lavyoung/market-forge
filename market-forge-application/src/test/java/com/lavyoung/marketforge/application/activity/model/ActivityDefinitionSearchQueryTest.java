package com.lavyoung.marketforge.application.activity.model;

import com.lavyoung.marketforge.types.model.PageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ActivityDefinitionSearchQuery} 单元测试。
 *
 * <p>验证运营查询条件在进入数据读取端口前完成文本归一化和分页参数校验。</p>
 */
class ActivityDefinitionSearchQueryTest {

    @Test
    void shouldNormalizeOptionalTextFilters() {
        // Given / When
        ActivityDefinitionSearchQuery query = new ActivityDefinitionSearchQuery(100301L, "  九宫格活动  ", "   ", new PageRequest(1, 20));

        // Then
        assertEquals(100301L, query.activityId());
        assertEquals("九宫格活动", query.activityName());
        assertNull(query.state());
    }

    @Test
    void shouldRejectMissingPageRequest() {
        // When / Then
        assertThrows(NullPointerException.class, () -> new ActivityDefinitionSearchQuery(null, null, null, null));
    }
}
