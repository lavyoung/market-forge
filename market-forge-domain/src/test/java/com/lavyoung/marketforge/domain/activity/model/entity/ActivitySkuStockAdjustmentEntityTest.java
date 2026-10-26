package com.lavyoung.marketforge.domain.activity.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动 SKU 库存调整实体测试。
 *
 * <p>验证库存补充、库存收回、快照防篡改、边界保护和数值溢出，并确保库存调整不会
 * 改变已经消费的库存数量。</p>
 */
class ActivitySkuStockAdjustmentEntityTest {

    @Test
    void shouldIncreaseTotalAndSurplusStockBySameDelta() {
        // Given
        ActivitySkuDefinitionEntity current = definition(100, 80);

        // When
        ActivitySkuStockAdjustmentEntity adjustment = ActivitySkuStockAdjustmentEntity.create(
                9901L, current, 50, "营销活动追加库存", "operator");

        // Then
        assertAll(
                () -> assertEquals(50, adjustment.quantityDelta()),
                () -> assertEquals(100, adjustment.stockCountBefore()),
                () -> assertEquals(150, adjustment.stockCountAfter()),
                () -> assertEquals(80, adjustment.stockCountSurplusBefore()),
                () -> assertEquals(130, adjustment.stockCountSurplusAfter()),
                () -> assertEquals(20, adjustment.consumedCountBefore()),
                () -> assertEquals(20, adjustment.consumedCountAfter()),
                () -> assertEquals(current.version(), adjustment.expectedVersion())
        );
    }

    @Test
    void shouldDecreaseOnlyUnconsumedStock() {
        // Given
        ActivitySkuDefinitionEntity current = definition(100, 80);

        // When
        ActivitySkuStockAdjustmentEntity adjustment = ActivitySkuStockAdjustmentEntity.create(
                9901L, current, -30, "收回未使用库存", "operator");

        // Then
        assertAll(
                () -> assertEquals(70, adjustment.stockCountAfter()),
                () -> assertEquals(50, adjustment.stockCountSurplusAfter()),
                () -> assertEquals(adjustment.consumedCountBefore(), adjustment.consumedCountAfter())
        );
    }

    @Test
    void shouldRejectZeroQuantityDelta() {
        // Given
        ActivitySkuDefinitionEntity current = definition(100, 80);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> ActivitySkuStockAdjustmentEntity.create(9901L, current, 0, "无效调整", "operator"));
    }

    @Test
    void shouldRejectDecreaseGreaterThanSurplusStock() {
        // Given
        ActivitySkuDefinitionEntity current = definition(100, 80);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> ActivitySkuStockAdjustmentEntity.create(9901L, current, -81, "超额收回", "operator"));
    }

    @Test
    void shouldRejectAdjustmentThatMakesTotalStockZero() {
        // Given
        ActivitySkuDefinitionEntity current = definition(100, 100);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> ActivitySkuStockAdjustmentEntity.create(9901L, current, -100, "错误清零", "operator"));
    }

    @Test
    void shouldRejectTamperedAfterSnapshot() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () -> new ActivitySkuStockAdjustmentEntity(
                9901L, 9011L, 10, 100, 120, 80, 90, 1, "伪造快照", "operator", null));
    }

    @Test
    void shouldRejectBlankReason() {
        // Given
        ActivitySkuDefinitionEntity current = definition(100, 80);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> ActivitySkuStockAdjustmentEntity.create(9901L, current, 10, " ", "operator"));
    }

    @Test
    void shouldExposeArithmeticOverflow() {
        // Given
        ActivitySkuDefinitionEntity current = definition(Long.MAX_VALUE, Long.MAX_VALUE);

        // When & Then
        assertThrows(ArithmeticException.class,
                () -> ActivitySkuStockAdjustmentEntity.create(9901L, current, 1, "溢出验证", "operator"));
    }

    private ActivitySkuDefinitionEntity definition(long stockCount, long stockCountSurplus) {
        return new ActivitySkuDefinitionEntity(
                9011L, 100301L, 100302L, stockCount, stockCountSurplus,
                50L, 3, "admin", "admin", null, null);
    }
}
