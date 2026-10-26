package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockAdjustmentEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuStockAdjustmentPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动 SKU 运营定义和库存调整流水转换测试。
 *
 * <p>验证运营字段、库存快照及审计时间在领域模型和持久化对象之间完整传递。</p>
 */
class ActivitySkuDefinitionAssemblerTest {

    private final ActivitySkuDefinitionAssembler definitionAssembler = Mappers.getMapper(ActivitySkuDefinitionAssembler.class);
    private final ActivitySkuStockAdjustmentAssembler adjustmentAssembler = Mappers.getMapper(ActivitySkuStockAdjustmentAssembler.class);

    @Test
    void shouldConvertSkuDefinitionInBothDirections() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 26, 10, 0);
        LocalDateTime updateTime = LocalDateTime.of(2026, 10, 26, 11, 0);
        ActivitySkuDefinitionEntity expected = new ActivitySkuDefinitionEntity(
                9011L, 100301L, 100302L, 100, 80, 50L, 3,
                "creator", "operator", createTime, updateTime);

        // When
        ActivitySkuPO po = definitionAssembler.toPO(expected);
        ActivitySkuDefinitionEntity actual = definitionAssembler.toEntity(po);

        // Then
        assertAll(
                () -> assertNull(po.getId()),
                () -> assertEquals(expected, actual)
        );
    }

    @Test
    void shouldConvertStockAdjustmentInBothDirections() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 26, 12, 0);
        ActivitySkuStockAdjustmentEntity expected = new ActivitySkuStockAdjustmentEntity(
                9901L, 9011L, 20, 100, 120, 80, 100,
                3, "追加营销库存", "operator", createTime);

        // When
        ActivitySkuStockAdjustmentPO po = adjustmentAssembler.toPO(expected);
        ActivitySkuStockAdjustmentEntity actual = adjustmentAssembler.toEntity(po);

        // Then
        assertAll(
                () -> assertNull(po.getId()),
                () -> assertEquals(expected, actual)
        );
    }
}
