package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardResult;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityRaffleApplicationServiceImpl;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityRaffleSettlementTransaction;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountDayEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Mock
    private IActivityRepository activityRepository;

    @Mock
    private IRaffleAward raffleAward;

    @Mock
    private IRuleTreeRepository ruleTreeRepository;

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
        ActivityRaffleApplicationServiceImpl service = newService();

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
        ActivityRaffleApplicationServiceImpl service = newService();

        // When & Then
        assertThrows(NullPointerException.class, () -> service.raffle(null));
        verifyNoInteractions(activityPartakeService, strategyRaffleService, settlementTransaction);
    }

    /**
     * Given 用户当天已参与 1 次且奖品配置次数锁，When 查询活动奖品列表，Then 返回奖品解锁状态和剩余解锁次数。
     */
    @Test
    void shouldQueryActivityAwardListWithUnlockState() {
        // Given
        ActivityEntity activity = ActivityEntity.builder()
                .activityId(ACTIVITY_ID)
                .activityName("九宫格活动抽奖")
                .strategyId(STRATEGY_ID)
                .build();
        StrategyAwardEntity unlockedAward = StrategyAwardEntity.builder()
                .strategyId(STRATEGY_ID)
                .awardId(101L)
                .awardTitle("6等奖")
                .awardSubtitle("谢谢参与")
                .awardRate(new BigDecimal("0.5000"))
                .sort(1)
                .build();
        StrategyAwardEntity lockedAward = StrategyAwardEntity.builder()
                .strategyId(STRATEGY_ID)
                .awardId(108L)
                .awardTitle("1等奖")
                .awardSubtitle("抽奖2次后解锁")
                .awardRate(new BigDecimal("0.0100"))
                .ruleModels("tree_lock_2")
                .sort(8)
                .build();
        ActivityAccountDayEntity dayAccount = ActivityAccountDayEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .day(LocalDate.now())
                .dayCount(3)
                .dayCountSurplus(2)
                .build();
        when(activityRepository.getActivityEntityByIdActivityId(ACTIVITY_ID)).thenReturn(activity);
        when(raffleAward.queryRaffleStrategyAwardList(STRATEGY_ID)).thenReturn(List.of(unlockedAward, lockedAward));
        when(ruleTreeRepository.queryAwardRuleLockCount(List.of("tree_lock_2"))).thenReturn(Map.of("tree_lock_2", 2));
        when(activityRepository.queryActivityAccountDayByUserId(USER_ID, ACTIVITY_ID, LocalDate.now())).thenReturn(dayAccount);

        // When
        List<ActivityAwardResult> results = newService().queryAwardList(new ActivityAwardListCommand(USER_ID, ACTIVITY_ID));

        // Then
        assertAll(
                () -> assertEquals(2, results.size()),
                () -> assertEquals(101L, results.get(0).awardId()),
                () -> assertTrue(results.get(0).isAwardUnlock()),
                () -> assertEquals(0, results.get(0).waitUnLockCount()),
                () -> assertEquals(108L, results.get(1).awardId()),
                () -> assertEquals(2, results.get(1).awardRuleLockCount()),
                () -> assertFalse(results.get(1).isAwardUnlock()),
                () -> assertEquals(1, results.get(1).waitUnLockCount())
        );
    }

    private ActivityRaffleApplicationServiceImpl newService() {
        return new ActivityRaffleApplicationServiceImpl(
                activityPartakeService,
                strategyRaffleService,
                settlementTransaction,
                activityArmory,
                activityRepository,
                raffleAward,
                ruleTreeRepository
        );
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
