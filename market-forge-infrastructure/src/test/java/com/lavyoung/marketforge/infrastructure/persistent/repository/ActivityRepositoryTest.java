package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.DeliveryOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountDayPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountMonthPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityOrderPO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.MessagePublisher;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
     * Given 订单已不是待支付状态，When 发货，Then 应抛出状态异常且不重复入账。
     */
    @Test
    void shouldRejectDeliveryWhenOrderStateIsNotWaitPay() {
        // Given
        ActivityRepository repository = repository();
        ActivityOrderPO orderPO = new ActivityOrderPO();
        ActivityOrderEntity order = order(OrderStateVO.COMPLETE);
        when(activityOrderDao.queryByUserIdAndOutBusinessNo(USER_ID, OUT_BUSINESS_NO))
                .thenReturn(Optional.of(orderPO));
        when(activityOrderAssembler.toEntity(orderPO)).thenReturn(order);
        when(activityOrderDao.updateStateByOutBusinessNo(
                USER_ID,
                OUT_BUSINESS_NO,
                OrderStateVO.WAIT_PAY.getCode(),
                OrderStateVO.COMPLETE.getCode()
        )).thenReturn(0);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> repository.deliverCreditPayOrder(new DeliveryOrderEntity(USER_ID, OUT_BUSINESS_NO))
        );

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_ORDER_STATE_ERROR, exception.getResponseCode());
        verify(activityAccountDao, never()).incrementTotalCount(any(), any(), any(), any(), any());
        verify(activityAccountMonthDao, never()).incrementMonthCount(any(), any(), any(), any());
        verify(activityAccountDayDao, never()).incrementDayCount(any(), any(), any(), any());
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
