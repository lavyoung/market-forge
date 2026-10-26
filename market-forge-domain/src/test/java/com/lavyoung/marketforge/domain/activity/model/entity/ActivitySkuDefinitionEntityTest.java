package com.lavyoung.marketforge.domain.activity.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动 SKU 运营定义实体测试。
 *
 * <p>验证库存边界、积分兑换开关、使用状态识别，以及普通配置修改不能重置库存或
 * 篡改已使用 SKU 业务归属的领域约束。</p>
 */
class ActivitySkuDefinitionEntityTest {

    @Test
    void shouldCreateValidDefinition() {
        // Given & When
        ActivitySkuDefinitionEntity definition = definition(100, 100, 50L);

        // Then
        assertAll(
                () -> assertEquals(9011L, definition.sku()),
                () -> assertEquals(100301L, definition.activityId()),
                () -> assertEquals(100302L, definition.activityCountId()),
                () -> assertEquals(100, definition.stockCount()),
                () -> assertEquals(100, definition.stockCountSurplus()),
                () -> assertTrue(definition.supportsCreditExchange()),
                () -> assertFalse(definition.hasBeenUsed()),
                () -> assertEquals(1, definition.version())
        );
    }

    @Test
    void shouldAllowSkuWithoutCreditExchangePrice() {
        // Given & When
        ActivitySkuDefinitionEntity definition = definition(100, 100, null);

        // Then
        assertFalse(definition.supportsCreditExchange());
    }

    @Test
    void shouldRejectInvalidStockBoundary() {
        // Given & When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> definition(0, 0, 50L)),
                () -> assertThrows(IllegalArgumentException.class, () -> definition(100, -1, 50L)),
                () -> assertThrows(IllegalArgumentException.class, () -> definition(100, 101, 50L))
        );
    }

    @Test
    void shouldRejectNonPositiveCreditAmount() {
        // Given & When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> definition(100, 100, 0L)),
                () -> assertThrows(IllegalArgumentException.class, () -> definition(100, 100, -1L))
        );
    }

    @Test
    void shouldRecognizeUsedSkuFromSurplusStock() {
        // Given & When
        ActivitySkuDefinitionEntity definition = definition(100, 80, 50L);

        // Then
        assertTrue(definition.hasBeenUsed());
    }

    @Test
    void shouldPreserveInventoryAndCreationMetadataWhenConfigurationUpdated() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 26, 10, 0);
        LocalDateTime updateTime = LocalDateTime.of(2026, 10, 26, 11, 0);
        ActivitySkuDefinitionEntity current = new ActivitySkuDefinitionEntity(
                9011L, 100301L, 100302L, 100, 100, 50L, 3,
                "creator", "creator", createTime, updateTime);

        // When
        ActivitySkuDefinitionEntity updated = current.updateConfiguration(100401L, 100402L, 80L, "operator");

        // Then
        assertAll(
                () -> assertEquals(current.sku(), updated.sku()),
                () -> assertEquals(100401L, updated.activityId()),
                () -> assertEquals(100402L, updated.activityCountId()),
                () -> assertEquals(current.stockCount(), updated.stockCount()),
                () -> assertEquals(current.stockCountSurplus(), updated.stockCountSurplus()),
                () -> assertEquals(80L, updated.creditAmount()),
                () -> assertEquals(current.version(), updated.version()),
                () -> assertEquals(current.createdBy(), updated.createdBy()),
                () -> assertEquals("operator", updated.updatedBy()),
                () -> assertEquals(current.createTime(), updated.createTime()),
                () -> assertEquals(current.updateTime(), updated.updateTime())
        );
    }

    @Test
    void shouldRejectRelationChangeAfterSkuHasBeenUsed() {
        // Given
        ActivitySkuDefinitionEntity current = definition(100, 80, 50L);

        // When & Then
        assertAll(
                () -> assertThrows(IllegalStateException.class,
                        () -> current.updateConfiguration(100401L, current.activityCountId(), 50L, "operator")),
                () -> assertThrows(IllegalStateException.class,
                        () -> current.updateConfiguration(current.activityId(), 100402L, 50L, "operator"))
        );
    }

    @Test
    void shouldAllowPriceChangeAfterSkuHasBeenUsed() {
        // Given
        ActivitySkuDefinitionEntity current = definition(100, 80, 50L);

        // When
        ActivitySkuDefinitionEntity updated = current.updateConfiguration(
                current.activityId(), current.activityCountId(), 80L, "operator");

        // Then
        assertAll(
                () -> assertEquals(80L, updated.creditAmount()),
                () -> assertEquals(100, updated.stockCount()),
                () -> assertEquals(80, updated.stockCountSurplus()),
                () -> assertEquals("operator", updated.updatedBy())
        );
    }

    private ActivitySkuDefinitionEntity definition(long stockCount, long stockCountSurplus, Long creditAmount) {
        return new ActivitySkuDefinitionEntity(
                9011L, 100301L, 100302L, stockCount, stockCountSurplus, creditAmount,
                1, "admin", "admin", null, null);
    }
}
