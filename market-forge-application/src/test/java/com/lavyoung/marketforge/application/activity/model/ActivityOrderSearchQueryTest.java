package com.lavyoung.marketforge.application.activity.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动订单查询条件测试。
 *
 * <p>验证查询条件在进入基础设施层之前完成字符串规范化、状态约束、
 * 时间范围校验和分页边界保护。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
class ActivityOrderSearchQueryTest {

    /**
     * 验证查询字符串会去除首尾空白，纯空白值会被规范化为 {@code null}。
     */
    @Test
    void shouldNormalizeSearchConditionsAndCalculateOffset() {
        // Given / When
        ActivityOrderSearchQuery query = new ActivityOrderSearchQuery(
                "  user-001  ",
                100301L,
                "   ",
                "  used  ",
                null,
                null,
                3,
                20
        );

        // Then
        assertAll(
                () -> assertEquals("user-001", query.userId()),
                () -> assertEquals(100301L, query.activityId()),
                () -> assertNull(query.orderId()),
                () -> assertEquals("used", query.orderState()),
                () -> assertEquals(40, query.offset())
        );
    }

    /**
     * 验证活动标识必须是正数。
     */
    @Test
    void shouldRejectInvalidActivityId() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class, () -> new ActivityOrderSearchQuery(
                null, 0L, null, null, null, null, 1, 20
        ));
    }

    /**
     * 验证未声明的订单状态不能进入搜索基础设施。
     */
    @Test
    void shouldRejectUnsupportedOrderState() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class, () -> new ActivityOrderSearchQuery(
                null, null, null, "unknown", null, null, 1, 20
        ));
    }

    /**
     * 验证时间范围必须遵循左闭右开语义，开始时间需要严格早于结束时间。
     */
    @Test
    void shouldRejectInvalidTimeRange() {
        // Given
        LocalDateTime boundary = LocalDateTime.of(2026, 10, 18, 10, 0);

        // When / Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new ActivityOrderSearchQuery(
                        null, null, null, null, boundary, boundary, 1, 20
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new ActivityOrderSearchQuery(
                        null, null, null, null, boundary.plusSeconds(1), boundary, 1, 20
                ))
        );
    }

    /**
     * 验证页码和每页记录数必须处于允许范围内。
     */
    @Test
    void shouldRejectInvalidPagination() {
        // Given / When / Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new ActivityOrderSearchQuery(
                        null, null, null, null, null, null, 0, 20
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new ActivityOrderSearchQuery(
                        null, null, null, null, null, null, 1, 0
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new ActivityOrderSearchQuery(
                        null, null, null, null, null, null, 1, 101
                ))
        );
    }

    /**
     * 验证分页偏移量超过整数范围时直接暴露算术异常，避免生成错误查询范围。
     */
    @Test
    void shouldExposeOffsetOverflow() {
        // Given
        ActivityOrderSearchQuery query = new ActivityOrderSearchQuery(
                null, null, null, null, null, null, Integer.MAX_VALUE, 100
        );

        // When / Then
        assertThrows(ArithmeticException.class, query::offset);
    }
}
