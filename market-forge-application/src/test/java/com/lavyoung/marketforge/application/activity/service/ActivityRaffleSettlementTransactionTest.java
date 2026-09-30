package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.service.impl.ActivityRaffleSettlementTransaction;
import com.lavyoung.marketforge.application.award.model.SaveUserAwardRecordCommand;
import com.lavyoung.marketforge.application.award.service.IAwardApplicationService;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;

/**
 * 验证活动抽奖结算事务会保存中奖记录并消费活动订单。
 */
@ExtendWith(MockitoExtension.class)
class ActivityRaffleSettlementTransactionTest {

    private static final String USER_ID = "user-001";
    private static final Long ACTIVITY_ID = 100_301L;
    private static final Long SKU = 9_011L;
    private static final Long STRATEGY_ID = 900_901_001L;
    private static final Long AWARD_ID = 900_901_011L;
    private static final String ORDER_ID = "RO1003010001";

    @Mock
    private IRaffleActivityPartakeService activityPartakeService;

    @Mock
    private IAwardApplicationService awardApplicationService;

    /**
     * Given 活动订单和抽奖结果，When 结算，Then 保存中奖记录并消费参与订单。
     */
    @Test
    void shouldSaveAwardRecordAndConsumeRaffleOrder() {
        // Given
        ActivityOrderEntity order = ActivityOrderEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .sku(SKU)
                .activityName("九宫格活动抽奖")
                .strategyId(STRATEGY_ID)
                .orderId(ORDER_ID)
                .orderTime(LocalDateTime.of(2026, 9, 30, 10, 0))
                .state(UserRaffleOrderStateVO.CREATE.getCode())
                .build();
        RaffleResult raffleResult = new RaffleResult(STRATEGY_ID, AWARD_ID, "random_ore", "quantity=1", "随机矿石");
        ActivityRaffleSettlementTransaction transaction =
                new ActivityRaffleSettlementTransaction(activityPartakeService, awardApplicationService);

        // When
        transaction.settle(order, raffleResult);

        // Then
        ArgumentCaptor<SaveUserAwardRecordCommand> commandCaptor =
                ArgumentCaptor.forClass(SaveUserAwardRecordCommand.class);
        InOrder inOrder = inOrder(awardApplicationService, activityPartakeService);
        inOrder.verify(awardApplicationService).saveUserAwardRecord(commandCaptor.capture());
        inOrder.verify(activityPartakeService).consumeRaffleOrder(USER_ID, ORDER_ID);
        assertAll(
                () -> assertEquals(USER_ID, commandCaptor.getValue().userId()),
                () -> assertEquals(ACTIVITY_ID, commandCaptor.getValue().activityId()),
                () -> assertEquals(STRATEGY_ID, commandCaptor.getValue().strategyId()),
                () -> assertEquals(ORDER_ID, commandCaptor.getValue().orderId()),
                () -> assertEquals(AWARD_ID, commandCaptor.getValue().awardId()),
                () -> assertEquals("随机矿石", commandCaptor.getValue().awardTitle())
        );
    }
}
