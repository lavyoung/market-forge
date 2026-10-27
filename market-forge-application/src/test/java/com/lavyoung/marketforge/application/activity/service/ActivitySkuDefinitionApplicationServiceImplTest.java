package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.AdjustActivitySkuStockCommand;
import com.lavyoung.marketforge.application.activity.model.CreateActivitySkuDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivitySkuDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.impl.ActivitySkuDefinitionApplicationServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockAdjustmentEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivitySkuDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 活动 SKU 运营应用服务测试。
 *
 * <p>验证应用层对活动生命周期、次数配置关系、乐观锁和库存调整的编排，确保无效请求
 * 在进入持久化写操作前被拒绝，并确认跨活动迁移使用稳定的数据库加锁顺序。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivitySkuDefinitionApplicationServiceImplTest {

    private static final Long SKU = 900001L;
    private static final Long CURRENT_ACTIVITY_ID = 200001L;
    private static final Long TARGET_ACTIVITY_ID = 100001L;
    private static final Long ACTIVITY_COUNT_ID = 300001L;

    @Mock
    private IActivitySkuDefinitionRepository activitySkuDefinitionRepository;

    @Mock
    private IActivityDefinitionRepository activityDefinitionRepository;

    @Mock
    private IActivityCountDefinitionRepository activityCountDefinitionRepository;

    private ActivitySkuDefinitionApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivitySkuDefinitionApplicationServiceImpl(activitySkuDefinitionRepository,
                activityDefinitionRepository, activityCountDefinitionRepository);
    }

    @Test
    void shouldCreateSkuWithFullRemainingStock() {
        // Given
        CreateActivitySkuDefinitionCommand command = new CreateActivitySkuDefinitionCommand(
                TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 500L, "admin");
        when(activityDefinitionRepository.lockByActivityId(TARGET_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activityCountDefinitionRepository.queryByActivityCountId(ACTIVITY_COUNT_ID))
                .thenReturn(Optional.of(activityCountDefinition()));
        when(activitySkuDefinitionRepository.queryBySku(anyLong())).thenAnswer(invocation ->
                Optional.of(skuDefinition(invocation.getArgument(0), TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID,
                        100L, 100L, 500L, 1)));

        // When
        ActivitySkuDefinitionResult result = service.create(command);

        // Then
        ArgumentCaptor<ActivitySkuDefinitionEntity> captor = ArgumentCaptor.forClass(ActivitySkuDefinitionEntity.class);
        verify(activitySkuDefinitionRepository).create(captor.capture());
        assertAll(() -> assertEquals(18, captor.getValue().sku().toString().length()),
                () -> assertEquals(100L, captor.getValue().stockCount()),
                () -> assertEquals(100L, captor.getValue().stockCountSurplus()),
                () -> assertEquals(0L, result.consumedCount()),
                () -> assertEquals(1, result.version()));
    }

    @Test
    void shouldRejectCreatingSkuForOpenActivity() {
        // Given
        CreateActivitySkuDefinitionCommand command = new CreateActivitySkuDefinitionCommand(
                TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 500L, "admin");
        when(activityDefinitionRepository.lockByActivityId(TARGET_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.OPEN)));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_ACTIVITY_STATE_ERROR, exception.getResponseCode());
        verify(activitySkuDefinitionRepository, never()).create(any());
    }

    @Test
    void shouldRejectActivityCountMismatchBeforeCreatingSku() {
        // Given
        CreateActivitySkuDefinitionCommand command = new CreateActivitySkuDefinitionCommand(
                TARGET_ACTIVITY_ID, 399999L, 100L, 500L, "admin");
        when(activityDefinitionRepository.lockByActivityId(TARGET_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_ACTIVITY_COUNT_MISMATCH, exception.getResponseCode());
        verify(activityCountDefinitionRepository, never()).queryByActivityCountId(anyLong());
        verify(activitySkuDefinitionRepository, never()).create(any());
    }

    @Test
    void shouldRejectMissingActivityCountDefinition() {
        // Given
        CreateActivitySkuDefinitionCommand command = new CreateActivitySkuDefinitionCommand(
                TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 500L, "admin");
        when(activityDefinitionRepository.lockByActivityId(TARGET_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activityCountDefinitionRepository.queryByActivityCountId(ACTIVITY_COUNT_ID)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, exception.getResponseCode());
        verify(activitySkuDefinitionRepository, never()).create(any());
    }

    @Test
    void shouldUpdateConfigurationAndLockActivitiesInAscendingOrder() {
        // Given
        ActivitySkuDefinitionEntity current = skuDefinition(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 100L, 500L, 2);
        ActivitySkuDefinitionEntity updated = skuDefinition(
                SKU, TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 100L, 600L, 3);
        UpdateActivitySkuDefinitionCommand command = new UpdateActivitySkuDefinitionCommand(
                SKU, TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, 600L, 2, "operator");
        when(activitySkuDefinitionRepository.queryBySku(SKU)).thenReturn(Optional.of(current), Optional.of(updated));
        when(activityDefinitionRepository.lockByActivityId(TARGET_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activityDefinitionRepository.lockByActivityId(CURRENT_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activityCountDefinitionRepository.queryByActivityCountId(ACTIVITY_COUNT_ID))
                .thenReturn(Optional.of(activityCountDefinition()));
        when(activitySkuDefinitionRepository.updateConfiguration(any())).thenReturn(true);

        // When
        ActivitySkuDefinitionResult result = service.updateConfiguration(command);

        // Then
        InOrder lockOrder = inOrder(activityDefinitionRepository);
        lockOrder.verify(activityDefinitionRepository).lockByActivityId(TARGET_ACTIVITY_ID);
        lockOrder.verify(activityDefinitionRepository).lockByActivityId(CURRENT_ACTIVITY_ID);
        ArgumentCaptor<ActivitySkuDefinitionEntity> captor = ArgumentCaptor.forClass(ActivitySkuDefinitionEntity.class);
        verify(activitySkuDefinitionRepository).updateConfiguration(captor.capture());
        assertAll(() -> assertEquals(TARGET_ACTIVITY_ID, captor.getValue().activityId()),
                () -> assertEquals(600L, captor.getValue().creditAmount()),
                () -> assertEquals(100L, captor.getValue().stockCountSurplus()),
                () -> assertEquals(3, result.version()));
    }

    @Test
    void shouldRejectStaleVersionBeforeLockingActivity() {
        // Given
        ActivitySkuDefinitionEntity current = skuDefinition(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 100L, 500L, 3);
        when(activitySkuDefinitionRepository.queryBySku(SKU)).thenReturn(Optional.of(current));
        UpdateActivitySkuDefinitionCommand command = new UpdateActivitySkuDefinitionCommand(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 600L, 2, "operator");

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateConfiguration(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_DEFINITION_CONFLICT, exception.getResponseCode());
        verify(activityDefinitionRepository, never()).lockByActivityId(anyLong());
        verify(activitySkuDefinitionRepository, never()).updateConfiguration(any());
    }

    @Test
    void shouldRejectChangingRelationAfterSkuHasBeenUsed() {
        // Given
        ActivitySkuDefinitionEntity current = skuDefinition(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 80L, 500L, 2);
        UpdateActivitySkuDefinitionCommand command = new UpdateActivitySkuDefinitionCommand(
                SKU, TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, 600L, 2, "operator");
        when(activitySkuDefinitionRepository.queryBySku(SKU)).thenReturn(Optional.of(current));
        when(activityDefinitionRepository.lockByActivityId(TARGET_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(TARGET_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activityDefinitionRepository.lockByActivityId(CURRENT_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activityCountDefinitionRepository.queryByActivityCountId(ACTIVITY_COUNT_ID))
                .thenReturn(Optional.of(activityCountDefinition()));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateConfiguration(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_RELATION_CHANGE_NOT_ALLOWED, exception.getResponseCode());
        verify(activitySkuDefinitionRepository, never()).updateConfiguration(any());
    }

    @Test
    void shouldReportLostConfigurationUpdateAsConflict() {
        // Given
        ActivitySkuDefinitionEntity current = skuDefinition(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 100L, 500L, 2);
        UpdateActivitySkuDefinitionCommand command = new UpdateActivitySkuDefinitionCommand(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 600L, 2, "operator");
        when(activitySkuDefinitionRepository.queryBySku(SKU)).thenReturn(Optional.of(current));
        when(activityDefinitionRepository.lockByActivityId(CURRENT_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activityCountDefinitionRepository.queryByActivityCountId(ACTIVITY_COUNT_ID))
                .thenReturn(Optional.of(activityCountDefinition()));
        when(activitySkuDefinitionRepository.updateConfiguration(any())).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateConfiguration(command));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_DEFINITION_CONFLICT, exception.getResponseCode());
    }

    @Test
    void shouldAdjustTotalAndRemainingStockWithoutChangingConsumedCount() {
        // Given
        ActivitySkuDefinitionEntity current = skuDefinition(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 80L, 500L, 2);
        ActivitySkuDefinitionEntity adjusted = skuDefinition(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 150L, 130L, 500L, 3);
        when(activitySkuDefinitionRepository.queryBySku(SKU)).thenReturn(Optional.of(current), Optional.of(adjusted));
        when(activityDefinitionRepository.lockByActivityId(CURRENT_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activitySkuDefinitionRepository.adjustStockAndRecord(any())).thenReturn(true);

        // When
        ActivitySkuDefinitionResult result = service.adjustStock(
                new AdjustActivitySkuStockCommand(SKU, 50L, "活动追加预算", 2, "operator"));

        // Then
        ArgumentCaptor<ActivitySkuStockAdjustmentEntity> captor =
                ArgumentCaptor.forClass(ActivitySkuStockAdjustmentEntity.class);
        verify(activitySkuDefinitionRepository).adjustStockAndRecord(captor.capture());
        assertAll(() -> assertEquals(100L, captor.getValue().stockCountBefore()),
                () -> assertEquals(150L, captor.getValue().stockCountAfter()),
                () -> assertEquals(80L, captor.getValue().stockCountSurplusBefore()),
                () -> assertEquals(130L, captor.getValue().stockCountSurplusAfter()),
                () -> assertEquals(20L, captor.getValue().consumedCountBefore()),
                () -> assertEquals(20L, captor.getValue().consumedCountAfter()),
                () -> assertEquals(20L, result.consumedCount()));
    }

    @Test
    void shouldRejectReducingMoreThanRemainingStock() {
        // Given
        ActivitySkuDefinitionEntity current = skuDefinition(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 80L, 500L, 2);
        when(activitySkuDefinitionRepository.queryBySku(SKU)).thenReturn(Optional.of(current));
        when(activityDefinitionRepository.lockByActivityId(CURRENT_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.adjustStock(
                new AdjustActivitySkuStockCommand(SKU, -81L, "回收库存", 2, "operator")));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_STOCK_ADJUSTMENT_FAILED, exception.getResponseCode());
        verify(activitySkuDefinitionRepository, never()).adjustStockAndRecord(any());
    }

    @Test
    void shouldReportLostStockAdjustmentAsConflict() {
        // Given
        ActivitySkuDefinitionEntity current = skuDefinition(
                SKU, CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, 100L, 80L, 500L, 2);
        when(activitySkuDefinitionRepository.queryBySku(SKU)).thenReturn(Optional.of(current));
        when(activityDefinitionRepository.lockByActivityId(CURRENT_ACTIVITY_ID))
                .thenReturn(Optional.of(activity(CURRENT_ACTIVITY_ID, ACTIVITY_COUNT_ID, ActivityDefinitionStateVO.DRAFT)));
        when(activitySkuDefinitionRepository.adjustStockAndRecord(any())).thenReturn(false);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.adjustStock(
                new AdjustActivitySkuStockCommand(SKU, 10L, "追加库存", 2, "operator")));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_DEFINITION_CONFLICT, exception.getResponseCode());
    }

    private ActivityDefinitionEntity activity(Long activityId, Long activityCountId, ActivityDefinitionStateVO state) {
        return new ActivityDefinitionEntity(activityId, "测试活动", "测试活动说明", begin(), end(), 1000,
                1000, activityCountId, 400001L, AwardUnlockScopeVO.TOTAL, state, 1,
                "admin", "admin", null, null);
    }

    private ActivityCountDefinitionEntity activityCountDefinition() {
        return new ActivityCountDefinitionEntity(ACTIVITY_COUNT_ID, "基础次数配置", 100, 10, 50,
                1, "admin", "admin", null, null);
    }

    private ActivitySkuDefinitionEntity skuDefinition(Long sku, Long activityId, Long activityCountId,
                                                      long stockCount, long surplus, Long creditAmount, int version) {
        return new ActivitySkuDefinitionEntity(sku, activityId, activityCountId, stockCount, surplus,
                creditAmount, version, "admin", "operator", null, null);
    }

    private LocalDateTime begin() {
        return LocalDateTime.of(2026, 11, 1, 0, 0);
    }

    private LocalDateTime end() {
        return LocalDateTime.of(2026, 11, 30, 23, 59);
    }
}
