package com.lavyoung.marketforge.domain.activity.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动次数配置运营定义实体测试。
 *
 * <p>验证额度层级关系、基础字段约束，以及不可变更新时对标识、版本和创建信息的保留。</p>
 */
class ActivityCountDefinitionEntityTest {

    @Test
    void shouldCreateValidDefinition() {
        // Given & When
        ActivityCountDefinitionEntity definition = definition(100, 10, 50);

        // Then
        assertAll(
                () -> assertEquals(100301L, definition.activityCountId()),
                () -> assertEquals("签到次数包", definition.activityCountName()),
                () -> assertEquals(100, definition.totalCount()),
                () -> assertEquals(10, definition.dayCount()),
                () -> assertEquals(50, definition.monthCount()),
                () -> assertEquals(1, definition.version()),
                () -> assertNull(definition.createTime()),
                () -> assertNull(definition.updateTime())
        );
    }

    @Test
    void shouldRejectZeroDayCount() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () -> definition(100, 0, 50));
    }

    @Test
    void shouldRejectMonthCountLessThanDayCount() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () -> definition(100, 20, 10));
    }

    @Test
    void shouldRejectTotalCountLessThanMonthCount() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () -> definition(40, 10, 50));
    }

    @Test
    void shouldPreserveIdentityAndCreationMetadataWhenUpdated() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 26, 10, 0);
        LocalDateTime updateTime = LocalDateTime.of(2026, 10, 26, 11, 0);
        ActivityCountDefinitionEntity current = new ActivityCountDefinitionEntity(
                100301L, "签到次数包", 100, 10, 50, 3, "creator", "creator", createTime, updateTime);

        // When
        ActivityCountDefinitionEntity updated = current.update("积分兑换次数包", 200, 20, 100, "operator");

        // Then
        assertAll(
                () -> assertEquals(current.activityCountId(), updated.activityCountId()),
                () -> assertEquals("积分兑换次数包", updated.activityCountName()),
                () -> assertEquals(200, updated.totalCount()),
                () -> assertEquals(20, updated.dayCount()),
                () -> assertEquals(100, updated.monthCount()),
                () -> assertEquals(current.version(), updated.version()),
                () -> assertEquals(current.createdBy(), updated.createdBy()),
                () -> assertEquals("operator", updated.updatedBy()),
                () -> assertEquals(current.createTime(), updated.createTime()),
                () -> assertEquals(current.updateTime(), updated.updateTime())
        );
    }

    private ActivityCountDefinitionEntity definition(int totalCount, int dayCount, int monthCount) {
        return new ActivityCountDefinitionEntity(
                100301L, "签到次数包", totalCount, dayCount, monthCount, 1, "admin", "admin", null, null);
    }
}
