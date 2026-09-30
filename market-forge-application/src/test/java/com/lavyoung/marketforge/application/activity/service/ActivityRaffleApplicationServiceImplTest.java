package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityRaffleApplicationServiceImpl;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityRaffleSettlementTransaction;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证活动抽奖应用用例会串联活动订单、策略抽奖和中奖结算。
 */
@ExtendWith(MockitoExtension.class)
class ActivityRaffleApplicationServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final Long ACTIVITY_ID = 100_301L;
    private static final Long SKU = 9_011L;
    private static final Long STRATEGY_ID = 900_901_001L;
    private static final Long AWARD_ID = 900_901_011L;
    private static final String ORDER_ID = "RO1003010001";

    @Mock
    private IRaffleActivityPartakeService activityPartakeService;

    @Mock
    private IStrategyRaffleService strategyRaffleService;

    @Mock
    private ActivityRaffleSettlementTransaction settlementTransaction;

    @Mock
    private IActivityArmory activityArmory;

    /**
     * Given 活动订单与策略抽奖结果，When 执行活动抽奖，Then 使用订单中的策略并触发结算。
     */
    @Test
    void shouldCreatePartakeOrderDrawStrategyAndSettleAwardRecord() {
        // Given
        ActivityOrderEntity order = newActivityOrder();
        RaffleResult raffleResult = new RaffleResult(STRATEGY_ID, AWARD_ID, "random_ore", "quantity=1", "随机矿石", "随机矿石");
        when(activityPartakeService.createRaffleOrder(any())).thenReturn(order);
        when(strategyRaffleService.raffle(new RaffleCommand(USER_ID, STRATEGY_ID))).thenReturn(raffleResult);
        ActivityRaffleApplicationServiceImpl service = new ActivityRaffleApplicationServiceImpl(
                activityPartakeService,
                strategyRaffleService,
                settlementTransaction,
                activityArmory
        );

        // When
        ActivityRaffleResult result = service.raffle(new ActivityRaffleCommand(USER_ID, ACTIVITY_ID, SKU));

        // Then
        ArgumentCaptor<PartakeRaffleActivityEntity> partakeCaptor =
                ArgumentCaptor.forClass(PartakeRaffleActivityEntity.class);
        verify(activityPartakeService).createRaffleOrder(partakeCaptor.capture());
        verify(strategyRaffleService).raffle(new RaffleCommand(USER_ID, STRATEGY_ID));
        verify(settlementTransaction).settle(order, raffleResult);
        assertAll(
                () -> assertEquals(USER_ID, partakeCaptor.getValue().userId()),
                () -> assertEquals(ACTIVITY_ID, partakeCaptor.getValue().activityId()),
                () -> assertEquals(SKU, partakeCaptor.getValue().sku()),
                () -> assertEquals(ORDER_ID, result.orderId()),
                () -> assertEquals(ACTIVITY_ID, result.activityId()),
                () -> assertEquals(STRATEGY_ID, result.strategyId()),
                () -> assertEquals(AWARD_ID, result.awardId()),
                () -> assertEquals("随机矿石", result.awardTitle())
        );
    }

    /**
     * Given 抽奖命令为空，When 执行活动抽奖，Then 快速失败且不触发领域服务。
     */
    @Test
    void shouldRejectNullCommand() {
        // Given
        ActivityRaffleApplicationServiceImpl service = new ActivityRaffleApplicationServiceImpl(
                activityPartakeService,
                strategyRaffleService,
                settlementTransaction,
                activityArmory
        );

        // When & Then
        assertThrows(NullPointerException.class, () -> service.raffle(null));
        verifyNoInteractions(activityPartakeService, strategyRaffleService, settlementTransaction);
    }

    private ActivityOrderEntity newActivityOrder() {
        return ActivityOrderEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .sku(SKU)
                .activityName("九宫格活动抽奖")
                .strategyId(STRATEGY_ID)
                .orderId(ORDER_ID)
                .orderTime(LocalDateTime.of(2026, 9, 30, 10, 0))
                .state(UserRaffleOrderStateVO.CREATE.getCode())
                .build();
    }
}
