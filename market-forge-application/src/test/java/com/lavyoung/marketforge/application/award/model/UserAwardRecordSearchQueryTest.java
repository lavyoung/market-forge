package com.lavyoung.marketforge.application.award.model;

import com.lavyoung.marketforge.types.model.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link UserAwardRecordSearchQuery} 单元测试。
 *
 * <p>验证中奖记录过滤条件规范化、公共分页组合和左闭右开时间范围约束。</p>
 */
class UserAwardRecordSearchQueryTest {

    /**
     * 验证文本过滤条件会去除首尾空白，并保留公共分页参数。
     */
    @Test
    void shouldNormalizeTextFiltersAndKeepPageRequest() {
        // Given
        PageRequest page = new PageRequest(2, 20);

        // When
        UserAwardRecordSearchQuery query = new UserAwardRecordSearchQuery(
                " user-001 ", 101L, " completed ", null, null, page);

        // Then
        assertEquals("user-001", query.userId());
        assertEquals("completed", query.awardState());
        assertEquals(page, query.page());
    }

    /**
     * 验证空白过滤条件被规范化为空，基础设施层无需重复判断空白字符串。
     */
    @Test
    void shouldNormalizeBlankFiltersToNull() {
        // When
        UserAwardRecordSearchQuery query = new UserAwardRecordSearchQuery(
                " ", null, "", null, null, new PageRequest(1, 20));

        // Then
        assertNull(query.userId());
        assertNull(query.awardState());
    }

    /**
     * 验证分页参数不能为空。
     */
    @Test
    void shouldRejectNullPageRequest() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new UserAwardRecordSearchQuery(null, null, null, null, null, null));
    }

    /**
     * 验证开始时间必须严格早于结束时间。
     */
    @Test
    void shouldRejectInvalidTimeRange() {
        // Given
        LocalDateTime boundary = LocalDateTime.of(2026, 10, 22, 10, 0);

        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new UserAwardRecordSearchQuery(
                        null, null, null, boundary, boundary, new PageRequest(1, 20)));
    }
}
