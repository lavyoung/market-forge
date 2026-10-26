package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockAdjustmentEntity;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivitySkuDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivitySkuStockAdjustmentAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivitySkuDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivitySkuStockAdjustmentDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuStockAdjustmentPO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动 SKU 运营定义仓储测试。
 *
 * <p>验证唯一键异常转换、条件更新结果、库存流水编排和缓存失效边界。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivitySkuDefinitionRepositoryTest {

    @Mock
    private IActivitySkuDao activitySkuDao;

    @Mock
    private IActivitySkuStockAdjustmentDao activitySkuStockAdjustmentDao;

    @Mock
    private ActivitySkuDefinitionAssembler definitionAssembler;

    @Mock
    private ActivitySkuStockAdjustmentAssembler adjustmentAssembler;

    @Mock
    private IRedisService redisService;

    private ActivitySkuDefinitionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ActivitySkuDefinitionRepository(activitySkuDao, activitySkuStockAdjustmentDao,
                definitionAssembler, adjustmentAssembler, redisService);
    }

    @Test
    void shouldQueryAndConvertDefinition() {
        // Given
        ActivitySkuPO po = new ActivitySkuPO();
        ActivitySkuDefinitionEntity expected = definition();
        when(activitySkuDao.queryBySku(9011L)).thenReturn(Optional.of(po));
        when(definitionAssembler.toEntity(po)).thenReturn(expected);

        // When
        Optional<ActivitySkuDefinitionEntity> actual = repository.queryBySku(9011L);

        // Then
        assertEquals(Optional.of(expected), actual);
    }

    @Test
    void shouldTranslateDuplicateKeyOnCreate() {
        // Given
        ActivitySkuDefinitionEntity definition = definition();
        ActivitySkuPO po = new ActivitySkuPO();
        DuplicateKeyException cause = new DuplicateKeyException("duplicate SKU");
        when(definitionAssembler.toPO(definition)).thenReturn(po);
        when(activitySkuDao.insert(po)).thenThrow(cause);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> repository.create(definition));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_SKU_DEFINITION_DUPLICATED, exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    @Test
    void shouldEvictDefinitionCacheOnlyAfterConfigurationUpdateSucceeds() {
        // Given
        ActivitySkuDefinitionEntity definition = definition();
        ActivitySkuPO po = new ActivitySkuPO();
        when(definitionAssembler.toPO(definition)).thenReturn(po);
        when(activitySkuDao.updateConfiguration(po)).thenReturn(0, 1);

        // When
        boolean conflict = repository.updateConfiguration(definition);
        boolean updated = repository.updateConfiguration(definition);

        // Then
        assertAll(
                () -> assertFalse(conflict),
                () -> assertTrue(updated),
                () -> verify(redisService, times(1)).delete(Constants.RedisKeys.ACTIVITY_SKU_KEY + definition.sku()),
                () -> verify(redisService, never()).delete(Constants.RedisKeys.ACTIVITY_SKU_STOCK_COUNT_KEY + definition.sku())
        );
    }

    @Test
    void shouldNotWriteRecordOrEvictCacheWhenStockUpdateConflicts() {
        // Given
        ActivitySkuStockAdjustmentEntity adjustment = adjustment();
        ActivitySkuStockAdjustmentPO po = new ActivitySkuStockAdjustmentPO();
        when(adjustmentAssembler.toPO(adjustment)).thenReturn(po);
        when(activitySkuDao.adjustStock(po)).thenReturn(0);

        // When
        boolean adjusted = repository.adjustStockAndRecord(adjustment);

        // Then
        assertAll(
                () -> assertFalse(adjusted),
                () -> verifyNoInteractions(activitySkuStockAdjustmentDao),
                () -> verifyNoInteractions(redisService)
        );
    }

    @Test
    void shouldRejectAdjustmentWhenRecordInsertFails() {
        // Given
        ActivitySkuStockAdjustmentEntity adjustment = adjustment();
        ActivitySkuStockAdjustmentPO po = adjustmentPO();
        when(adjustmentAssembler.toPO(adjustment)).thenReturn(po);
        when(activitySkuDao.adjustStock(po)).thenReturn(1);
        when(activitySkuStockAdjustmentDao.insertAdjustment(po)).thenReturn(0);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> repository.adjustStockAndRecord(adjustment));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_SKU_STOCK_ADJUSTMENT_FAILED, exception.getResponseCode()),
                () -> verifyNoInteractions(redisService)
        );
    }

    @Test
    void shouldTranslateDuplicateAdjustmentRecord() {
        // Given
        ActivitySkuStockAdjustmentEntity adjustment = adjustment();
        ActivitySkuStockAdjustmentPO po = adjustmentPO();
        DuplicateKeyException cause = new DuplicateKeyException("duplicate adjustment");
        when(adjustmentAssembler.toPO(adjustment)).thenReturn(po);
        when(activitySkuDao.adjustStock(po)).thenReturn(1);
        when(activitySkuStockAdjustmentDao.insertAdjustment(po)).thenThrow(cause);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> repository.adjustStockAndRecord(adjustment));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_SKU_STOCK_ADJUSTMENT_DUPLICATED,
                        exception.getResponseCode()),
                () -> assertSame(cause, exception.getCause()),
                () -> verifyNoInteractions(redisService)
        );
    }

    @Test
    void shouldWriteAdjustmentBeforeEvictingBothCaches() {
        // Given
        ActivitySkuStockAdjustmentEntity adjustment = adjustment();
        ActivitySkuStockAdjustmentPO po = adjustmentPO();
        when(adjustmentAssembler.toPO(adjustment)).thenReturn(po);
        when(activitySkuDao.adjustStock(po)).thenReturn(1);
        when(activitySkuStockAdjustmentDao.insertAdjustment(po)).thenReturn(1);

        // When
        boolean adjusted = repository.adjustStockAndRecord(adjustment);

        // Then
        assertTrue(adjusted);
        var ordered = inOrder(activitySkuDao, activitySkuStockAdjustmentDao, redisService);
        ordered.verify(activitySkuDao).adjustStock(po);
        ordered.verify(activitySkuStockAdjustmentDao).insertAdjustment(po);
        ordered.verify(redisService).delete(Constants.RedisKeys.ACTIVITY_SKU_KEY + adjustment.sku());
        ordered.verify(redisService).delete(Constants.RedisKeys.ACTIVITY_SKU_STOCK_COUNT_KEY + adjustment.sku());
    }

    private ActivitySkuDefinitionEntity definition() {
        return new ActivitySkuDefinitionEntity(
                9011L, 100301L, 100302L, 100, 80, 50L,
                3, "admin", "operator", null, null);
    }

    private ActivitySkuStockAdjustmentEntity adjustment() {
        return new ActivitySkuStockAdjustmentEntity(
                9901L, 9011L, 20, 100, 120, 80, 100,
                3, "追加营销库存", "operator", null);
    }

    private ActivitySkuStockAdjustmentPO adjustmentPO() {
        ActivitySkuStockAdjustmentPO po = new ActivitySkuStockAdjustmentPO();
        po.setAdjustmentId(9901L);
        po.setSku(9011L);
        return po;
    }
}
