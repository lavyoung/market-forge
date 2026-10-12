package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuProductResult;
import com.lavyoung.marketforge.application.activity.service.impl.ActivitySkuProductApplicationServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动 SKU 商品查询应用服务测试。
 *
 * <p>测试重点是验证商品查询会把 SKU 配置和活动次数配置组装成前端需要的结果，
 * 并且通过批量查询次数配置避免对每个 SKU 单独查询。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivitySkuProductApplicationServiceImplTest {

    private static final Long ACTIVITY_ID = 100301L;
    private static final Long SKU = 9011L;
    private static final Long ACTIVITY_COUNT_ID = 11101L;

    @Mock
    private IActivityRepository activityRepository;

    /**
     * Given 活动存在 SKU 和次数配置，When 查询商品，Then 应组装库存、次数和积分价格。
     */
    @Test
    void shouldAssembleSkuProductWithActivityCount() {
        // Given
        when(activityRepository.queryActivitySkuListByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(new ActivitySkuEntity(SKU, ACTIVITY_ID, ACTIVITY_COUNT_ID,
                        100000, 99888, 100L)));
        when(activityRepository.queryRaffleActivityCountListByActivityIds(List.of(ACTIVITY_COUNT_ID)))
                .thenReturn(List.of(new ActivityCountEntity(ACTIVITY_COUNT_ID, 100, 10, 100)));
        ActivitySkuProductApplicationServiceImpl service = new ActivitySkuProductApplicationServiceImpl(activityRepository);

        // When
        List<ActivitySkuProductResult> results = service.querySkuProducts(ACTIVITY_ID);

        // Then
        assertEquals(1, results.size());
        ActivitySkuProductResult result = results.get(0);
        assertAll(
                () -> assertEquals(SKU, result.sku()),
                () -> assertEquals(ACTIVITY_ID, result.activityId()),
                () -> assertEquals(100000, result.stockCount()),
                () -> assertEquals(99888, result.stockCountSurplus()),
                () -> assertEquals(100, result.totalCount()),
                () -> assertEquals(10, result.dayCount()),
                () -> assertEquals(100, result.monthCount()),
                () -> assertEquals(100L, result.creditAmount())
        );
        verify(activityRepository).queryRaffleActivityCountListByActivityIds(List.of(ACTIVITY_COUNT_ID));
    }

    /**
     * Given 活动没有 SKU，When 查询商品，Then 应返回空列表且不查询次数配置。
     */
    @Test
    void shouldReturnEmptyListWhenActivityHasNoSku() {
        // Given
        when(activityRepository.queryActivitySkuListByActivityId(ACTIVITY_ID)).thenReturn(List.of());
        ActivitySkuProductApplicationServiceImpl service = new ActivitySkuProductApplicationServiceImpl(activityRepository);

        // When
        List<ActivitySkuProductResult> results = service.querySkuProducts(ACTIVITY_ID);

        // Then
        assertEquals(List.of(), results);
        verifyNoInteractionsAfterSkuQuery();
    }

    /**
     * Given SKU 关联的次数配置不存在，When 查询商品，Then 应返回次数配置缺失业务异常。
     */
    @Test
    void shouldRejectProductWhenActivityCountDoesNotExist() {
        // Given
        when(activityRepository.queryActivitySkuListByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(activitySku(100L)));
        when(activityRepository.queryRaffleActivityCountListByActivityIds(List.of(ACTIVITY_COUNT_ID)))
                .thenReturn(List.of());
        ActivitySkuProductApplicationServiceImpl service = new ActivitySkuProductApplicationServiceImpl(activityRepository);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.querySkuProducts(ACTIVITY_ID));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, exception.getResponseCode());
    }

    /**
     * Given SKU 没有配置有效积分价格，When 查询商品，Then 应返回积分价格无效业务异常。
     */
    @Test
    void shouldRejectProductWhenCreditAmountIsInvalid() {
        // Given
        when(activityRepository.queryActivitySkuListByActivityId(ACTIVITY_ID))
                .thenReturn(List.of(activitySku(0L)));
        when(activityRepository.queryRaffleActivityCountListByActivityIds(List.of(ACTIVITY_COUNT_ID)))
                .thenReturn(List.of(new ActivityCountEntity(ACTIVITY_COUNT_ID, 100, 10, 100)));
        ActivitySkuProductApplicationServiceImpl service = new ActivitySkuProductApplicationServiceImpl(activityRepository);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.querySkuProducts(ACTIVITY_ID));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_CREDIT_AMOUNT_INVALID, exception.getResponseCode());
    }

    /**
     * 创建测试用活动 SKU。
     *
     * @param creditAmount SKU 积分价格
     * @return 活动 SKU 领域实体
     */
    private ActivitySkuEntity activitySku(Long creditAmount) {
        return new ActivitySkuEntity(SKU, ACTIVITY_ID, ACTIVITY_COUNT_ID, 100000, 99888, creditAmount);
    }

    /**
     * 校验空商品场景只发生了 SKU 查询。
     */
    private void verifyNoInteractionsAfterSkuQuery() {
        verify(activityRepository).queryActivitySkuListByActivityId(ACTIVITY_ID);
        verifyNoMoreInteractions(activityRepository);
    }
}
