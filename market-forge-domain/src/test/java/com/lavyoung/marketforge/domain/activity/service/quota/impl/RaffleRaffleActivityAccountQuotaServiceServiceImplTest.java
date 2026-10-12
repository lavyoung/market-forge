package com.lavyoung.marketforge.domain.activity.service.quota.impl;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderTradeTypeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivitySkuStockMessageRepository;
import com.lavyoung.marketforge.domain.activity.service.quota.policy.ITradePolicy;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.IActionChain;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动 SKU 额度下单服务测试。
 *
 * <p>重点验证第 27 节引入交易策略后，同一个 SKU 下单模板能够根据订单交易类型
 * 分流到不同策略：返利无支付订单直接完成并充值，积分支付订单只创建待支付订单。</p>
 */
class RaffleRaffleActivityAccountQuotaServiceServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final Long SKU = 9011L;
    private static final Long ACTIVITY_ID = 100301L;
    private static final Long ACTIVITY_COUNT_ID = 200301L;
    private static final String OUT_BUSINESS_NO = "biz-001";

    /**
     * Given 返利无支付交易，When 创建 SKU 订单，Then 应选择无支付策略且订单状态为完成。
     */
    @Test
    void shouldCreateCompletedOrderWithNoPayTradePolicy() {
        // Given
        ITradePolicy noPayPolicy = mock(ITradePolicy.class);
        ITradePolicy creditPayPolicy = mock(ITradePolicy.class);
        RaffleRaffleActivityAccountQuotaServiceServiceImpl service = service(Map.of(
                OrderTradeTypeVO.REBATE_NO_PAY.getCode(), noPayPolicy,
                OrderTradeTypeVO.CREDIT_PAY.getCode(), creditPayPolicy
        ));

        // When
        String orderId = service.createSkuRechargeOrder(new SkuRechargeEntity(
                USER_ID, SKU, OUT_BUSINESS_NO, OrderTradeTypeVO.REBATE_NO_PAY));

        // Then
        CreateQuotaOrderAggregate aggregate = captureAggregate(noPayPolicy);
        assertAll(
                () -> assertNotNull(orderId),
                () -> assertEquals(USER_ID, aggregate.userId()),
                () -> assertEquals(ACTIVITY_ID, aggregate.activityId()),
                () -> assertEquals(OrderStateVO.COMPLETE.getCode(), aggregate.activityOrder().state()),
                () -> assertEquals(OUT_BUSINESS_NO, aggregate.activityOrder().outBusinessNo())
        );
        verify(creditPayPolicy, never()).trade(any());
    }

    /**
     * Given 积分支付交易，When 创建 SKU 订单，Then 应选择积分支付策略且订单状态为待支付。
     */
    @Test
    void shouldCreateWaitPayOrderWithCreditPayTradePolicy() {
        // Given
        ITradePolicy noPayPolicy = mock(ITradePolicy.class);
        ITradePolicy creditPayPolicy = mock(ITradePolicy.class);
        RaffleRaffleActivityAccountQuotaServiceServiceImpl service = service(Map.of(
                OrderTradeTypeVO.REBATE_NO_PAY.getCode(), noPayPolicy,
                OrderTradeTypeVO.CREDIT_PAY.getCode(), creditPayPolicy
        ));

        // When
        String orderId = service.createSkuRechargeOrder(new SkuRechargeEntity(
                USER_ID, SKU, OUT_BUSINESS_NO, OrderTradeTypeVO.CREDIT_PAY));

        // Then
        CreateQuotaOrderAggregate aggregate = captureAggregate(creditPayPolicy);
        assertAll(
                () -> assertNotNull(orderId),
                () -> assertEquals(USER_ID, aggregate.userId()),
                () -> assertEquals(ACTIVITY_ID, aggregate.activityId()),
                () -> assertEquals(OrderStateVO.WAIT_PAY.getCode(), aggregate.activityOrder().state()),
                () -> assertEquals(OUT_BUSINESS_NO, aggregate.activityOrder().outBusinessNo())
        );
        verify(noPayPolicy, never()).trade(any());
    }

    /**
     * Given 缺少对应交易策略，When 创建 SKU 订单，Then 应抛出参数异常且不落库。
     */
    @Test
    void shouldRejectOrderWhenTradePolicyIsMissing() {
        // Given
        RaffleRaffleActivityAccountQuotaServiceServiceImpl service = service(Map.of());
        SkuRechargeEntity request = new SkuRechargeEntity(
                USER_ID, SKU, OUT_BUSINESS_NO, OrderTradeTypeVO.CREDIT_PAY);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createSkuRechargeOrder(request)
        );

        // Then
        assertEquals(CommonResponseCode.PARAM_INVALID, exception.getResponseCode());
    }

    private static RaffleRaffleActivityAccountQuotaServiceServiceImpl service(Map<String, ITradePolicy> tradePolicyGroup) {
        IActivityRepository repository = mock(IActivityRepository.class);
        when(repository.queryActivitySku(SKU)).thenReturn(activitySku());
        when(repository.getActivityEntityByIdActivityId(ACTIVITY_ID)).thenReturn(activity());
        when(repository.queryRaffleActivityCountByActivityCountId(ACTIVITY_COUNT_ID)).thenReturn(activityCount());

        IActionChain baseAction = mock(IActionChain.class);
        IActionChain stockAction = mock(IActionChain.class);
        when(baseAction.appendNext(stockAction)).thenReturn(stockAction);
        when(baseAction.action(any(ActivitySkuEntity.class), any(ActivityEntity.class), any(ActivityCountEntity.class)))
                .thenReturn(true);
        DefaultActivityChainFactory chainFactory = new DefaultActivityChainFactory(Map.of(
                DefaultActivityChainFactory.ActionModel.ACTIVITY_BASE_ACTION.getCode(), baseAction,
                DefaultActivityChainFactory.ActionModel.ACTIVITY_SKU_STOCK_ACTION.getCode(), stockAction
        ));

        return new RaffleRaffleActivityAccountQuotaServiceServiceImpl(
                repository,
                mock(IActivitySkuStockMessageRepository.class),
                chainFactory,
                tradePolicyGroup
        );
    }

    private static CreateQuotaOrderAggregate captureAggregate(ITradePolicy tradePolicy) {
        ArgumentCaptor<CreateQuotaOrderAggregate> captor =
                ArgumentCaptor.forClass(CreateQuotaOrderAggregate.class);
        verify(tradePolicy).trade(captor.capture());
        return captor.getValue();
    }

    private static ActivitySkuEntity activitySku() {
        return new ActivitySkuEntity(SKU, ACTIVITY_ID, ACTIVITY_COUNT_ID, 100, 100, 100L);
    }

    private static ActivityEntity activity() {
        return ActivityEntity.builder()
                .activityId(ACTIVITY_ID)
                .activityName("测试活动")
                .activityDesc("测试活动说明")
                .beginDateTime(LocalDateTime.now().minusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1))
                .stockCount(100)
                .stockCountSurplus(100)
                .activityCountId(ACTIVITY_COUNT_ID)
                .strategyId(100001L)
                .state("open")
                .build();
    }

    private static ActivityCountEntity activityCount() {
        return new ActivityCountEntity(ACTIVITY_COUNT_ID, 10, 1, 3);
    }
}
