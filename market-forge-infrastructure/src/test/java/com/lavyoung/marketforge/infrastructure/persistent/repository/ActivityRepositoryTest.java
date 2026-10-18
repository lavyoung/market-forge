package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.DeliveryOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.MessagePublisher;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动仓储测试。
 *
 * <p>重点验证积分支付成功后的活动 SKU 发货逻辑：只有待支付订单可以完成发货，
 * 且订单状态推进和活动账户额度入账必须按同一个业务幂等号执行。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityRepositoryTest {

    private static final String USER_ID = "user-001";
    private static final String OUT_BUSINESS_NO = "exchange-20261011-user-001-9011";
    private static final Long ACTIVITY_ID = 100301L;
    private static final Long SKU = 9011L;
    private static final String ORDER_ID = "ROP100001";
    private static final LocalDateTime ORDER_TIME = LocalDateTime.of(2026, 10, 11, 22, 0);

    @Mock
    private IActivityDao activityDao;
    @Mock
    private IActivitySkuDao activitySkuDao;
    @Mock
    private IActivityCountDao activityCountDao;
    @Mock
    private IActivityOrderDao activityOrderDao;
    @Mock
    private IActivityAccountDao activityAccountDao;
    @Mock
    private IActivityAccountDayDao activityAccountDayDao;
    @Mock
    private IActivityAccountMonthDao activityAccountMonthDao;
    @Mock
    private ActivityAssembler activityAssembler;
    @Mock
    private ActivitySkuAssembler activitySkuAssembler;
    @Mock
    private ActivityCountAssembler activityCountAssembler;
    @Mock
    private ActivityOrderAssembler activityOrderAssembler;
    @Mock
    private ActivityAccountAssembler activityAccountAssembler;
    @Mock
    private ActivityAccountDayAssembler activityAccountDayAssembler;
    @Mock
    private ActivityAccountMonthAssembler activityAccountMonthAssembler;
    @Mock
    private IRedisService redisService;
    @Mock
    private MessagePublisher messagePublisher;

    /**
     * Given 待支付活动 SKU 订单和已存在账户，When 发货，Then 应推进订单状态并累计总、月、日账户。
     */
    @Test
    void shouldDeliverWaitPayCreditOrderAndIncrementAccounts() {
        // Given
        ActivityRepository repository = repository();
        ActivityOrderPO orderPO = new ActivityOrderPO();
        ActivityOrderEntity order = order(OrderStateVO.WAIT_PAY);
        when(activityOrderDao.queryByUserIdAndOutBusinessNo(USER_ID, OUT_BUSINESS_NO))
                .thenReturn(Optional.of(orderPO));
        when(activityOrderAssembler.toEntity(orderPO)).thenReturn(order);
        when(activityOrderDao.updateStateByOutBusinessNo(
                USER_ID,
                OUT_BUSINESS_NO,
                OrderStateVO.WAIT_PAY.getCode(),
                OrderStateVO.COMPLETE.getCode()
        )).thenReturn(1);
        when(activityAccountDao.queryByUserIdAndActivityId(USER_ID, ACTIVITY_ID))
                .thenReturn(Optional.of(new ActivityAccountPO()));
        when(activityAccountDao.incrementTotalCount(USER_ID, ACTIVITY_ID, 10, 1, 3))
                .thenReturn(1);
        when(activityAccountMonthDao.queryByUserIdAndActivityIdAndMonth(
                USER_ID,
                ACTIVITY_ID,
                YearMonth.from(ORDER_TIME).toString()
        )).thenReturn(Optional.of(new ActivityAccountMonthPO()));
        when(activityAccountMonthDao.incrementMonthCount(
                USER_ID,
                ACTIVITY_ID,
                YearMonth.from(ORDER_TIME).toString(),
                3
        )).thenReturn(1);
        when(activityAccountDayDao.queryByUserIdAndActivityIdAndDay(
                USER_ID,
                ACTIVITY_ID,
                LocalDate.from(ORDER_TIME)
        )).thenReturn(Optional.of(new ActivityAccountDayPO()));
        when(activityAccountDayDao.incrementDayCount(
                USER_ID,
                ACTIVITY_ID,
                LocalDate.from(ORDER_TIME),
                1
        )).thenReturn(1);

        // When
        repository.deliverCreditPayOrder(new DeliveryOrderEntity(USER_ID, OUT_BUSINESS_NO));

        // Then
        verify(activityOrderDao).updateStateByOutBusinessNo(
                USER_ID,
                OUT_BUSINESS_NO,
                OrderStateVO.WAIT_PAY.getCode(),
                OrderStateVO.COMPLETE.getCode()
        );
        verify(activityAccountDao).incrementTotalCount(USER_ID, ACTIVITY_ID, 10, 1, 3);
        verify(activityAccountMonthDao).incrementMonthCount(
                USER_ID,
                ACTIVITY_ID,
                YearMonth.from(ORDER_TIME).toString(),
                3
        );
        verify(activityAccountDayDao).incrementDayCount(
                USER_ID,
                ACTIVITY_ID,
                LocalDate.from(ORDER_TIME),
                1
        );
    }

    /**
     * Given 发货订单不存在，When 发货，Then 应抛出订单不存在异常且不更新账户。
     */
    @Test
    void shouldRejectDeliveryWhenOrderDoesNotExist() {
        // Given
        ActivityRepository repository = repository();
        when(activityOrderDao.queryByUserIdAndOutBusinessNo(USER_ID, OUT_BUSINESS_NO))
                .thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> repository.deliverCreditPayOrder(new DeliveryOrderEntity(USER_ID, OUT_BUSINESS_NO))
        );

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_ORDER_NOT_FOUND, exception.getResponseCode());
        verify(activityOrderDao, never()).updateStateByOutBusinessNo(any(), any(), any(), any());
        verify(activityAccountDao, never()).incrementTotalCount(any(), any(), any(), any(), any());
    }

    /**
     * Given 订单已经完成，When 收到重复发货消息，Then 应幂等返回且不重复增加账户额度。
     */
    @Test
    void shouldIgnoreRedeliveryWhenOrderIsComplete() {
        // Given
        ActivityRepository repository = repository();
        ActivityOrderPO orderPO = new ActivityOrderPO();
        ActivityOrderEntity order = order(OrderStateVO.COMPLETE);
        when(activityOrderDao.queryByUserIdAndOutBusinessNo(USER_ID, OUT_BUSINESS_NO))
                .thenReturn(Optional.of(orderPO));
        when(activityOrderAssembler.toEntity(orderPO)).thenReturn(order);

        // When
        repository.deliverCreditPayOrder(new DeliveryOrderEntity(USER_ID, OUT_BUSINESS_NO));

        // Then
        verify(activityOrderDao, never()).updateStateByOutBusinessNo(any(), any(), any(), any());
        verify(activityAccountDao, never()).incrementTotalCount(any(), any(), any(), any(), any());
        verify(activityAccountMonthDao, never()).incrementMonthCount(any(), any(), any(), any());
        verify(activityAccountDayDao, never()).incrementDayCount(any(), any(), any(), any());
    }

    /**
     * Given SKU 存在，When 清空 SKU 库存，Then 初始库存和剩余库存都应清零并删除缓存。
     */
    @Test
    void shouldClearInitialAndSurplusSkuStock() {
        // Given
        ActivityRepository repository = repository();
        ActivitySkuPO skuPO = new ActivitySkuPO();
        skuPO.setSku(SKU);
        skuPO.setStockCount(100L);
        skuPO.setStockCountSurplus(80L);
        when(activitySkuDao.queryBySku(SKU)).thenReturn(Optional.of(skuPO));

        // When
        repository.clearActivitySkuStock(SKU);

        // Then
        assertEquals(0L, skuPO.getStockCount());
        assertEquals(0L, skuPO.getStockCountSurplus());
        verify(activitySkuDao).updateById(skuPO);
        verify(redisService).delete(anyString());
    }

    /**
     * Given SKU 不存在，When 清空 SKU 库存，Then 应返回 SKU 不存在异常且不操作缓存。
     */
    @Test
    void shouldRejectClearingStockWhenSkuDoesNotExist() {
        // Given
        ActivityRepository repository = repository();
        when(activitySkuDao.queryBySku(SKU)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> repository.clearActivitySkuStock(SKU)
        );

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, exception.getResponseCode());
        verify(activitySkuDao, never()).updateById(any(ActivitySkuPO.class));
        verifyNoInteractions(redisService);
    }

    /**
     * Given 系统使用地区时区且缓存中仍有库存，When 预扣库存，Then 应正确计算锁租约并完成 CAS 扣减。
     *
     * <p>该场景防止把 {@code Asia/Hong_Kong} 这类地区时区误当作 {@code +08:00}
     * 固定偏移量解析，从而在创建积分兑换订单前抛出 {@link java.time.DateTimeException}。</p>
     */
    @Test
    void shouldDeductSkuStockWithRegionBasedSystemTimeZone() {
        // Given
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Hong_Kong"));
        try {
            ActivityRepository repository = repository();
            String stockKey = Constants.RedisKeys.ACTIVITY_SKU_STOCK_COUNT_KEY + SKU;
            String lockKey = stockKey + "_5";
            when(redisService.getValue(stockKey, Integer.class)).thenReturn(Optional.of(5));
            when(redisService.setIfAbsent(eq(lockKey), eq(1), any(Duration.class))).thenReturn(true);
            when(redisService.compareAndSetAtomicLong(stockKey, 5, 4)).thenReturn(true);

            // When
            var result = repository.subtractionActivitySkuStock(SKU, LocalDateTime.now().plusDays(1));

            // Then
            assertTrue(result.success());
            verify(redisService).setIfAbsent(eq(lockKey), eq(1), argThat(duration -> !duration.isNegative() && !duration.isZero()));
            verify(redisService).compareAndSetAtomicLong(stockKey, 5, 4);
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    private ActivityRepository repository() {
        return new ActivityRepository(
                activityDao,
                activitySkuDao,
                activityCountDao,
                activityOrderDao,
                activityAccountDao,
                activityAccountDayDao,
                activityAccountMonthDao,
                activityAssembler,
                activitySkuAssembler,
                activityCountAssembler,
                activityOrderAssembler,
                activityAccountAssembler,
                activityAccountDayAssembler,
                activityAccountMonthAssembler,
                redisService,
                messagePublisher
        );
    }

    private static ActivityOrderEntity order(OrderStateVO state) {
        return ActivityOrderEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .sku(SKU)
                .activityName("测试活动")
                .strategyId(100001L)
                .orderId(ORDER_ID)
                .orderTime(ORDER_TIME)
                .totalCount(10)
                .dayCount(1)
                .monthCount(3)
                .state(state.getCode())
                .outBusinessNo(OUT_BUSINESS_NO)
                .build();
    }
}
