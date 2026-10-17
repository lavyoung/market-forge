package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.*;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityRaffleApplicationServiceImpl;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityRaffleSettlementTransaction;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockResultVO;
import com.lavyoung.marketforge.domain.award.service.IAwardUnlockService;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private IAwardUnlockService awardUnlockService;

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

    @Mock
    private IStrategyRepository strategyRepository;

    @Mock
    private IActivityDegradeSwitch activityDegradeSwitch;

    @Mock
    private IActivityAccessPolicy activityAccessPolicy;

    /**
     * Given 活动订单与策略抽奖结果，When 执行活动抽奖，Then 使用订单中的策略并触发结算。
     */
    @Test
    void shouldCreatePartakeOrderDrawStrategyAndSettleAwardRecord() {
        // Given
        ActivityOrderEntity order = newActivityOrder();
        RaffleResult raffleResult = new RaffleResult(STRATEGY_ID, AWARD_ID, "random_ore", "quantity=1", "随机矿石", "随机矿石");
        when(activityAccessPolicy.isAllowed(USER_ID)).thenReturn(true);
        when(activityPartakeService.createRaffleOrder(any())).thenReturn(order);
        when(strategyRaffleService.raffle(new RaffleCommand(USER_ID, STRATEGY_ID))).thenReturn(raffleResult);
        ActivityRaffleApplicationServiceImpl service = newService();

        // When
        ActivityRaffleResult result = service.raffle(new ActivityRaffleCommand(USER_ID, ACTIVITY_ID, SKU));

        // Then
        ArgumentCaptor<PartakeRaffleActivityEntity> partakeCaptor =
                ArgumentCaptor.forClass(PartakeRaffleActivityEntity.class);
        verify(activityPartakeService).createRaffleOrder(partakeCaptor.capture());
        verify(activityAccessPolicy).isAllowed(USER_ID);
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
     * Given 活动处于动态降级状态，When 执行活动抽奖，Then 返回降级业务异常且不创建活动订单。
     */
    @Test
    void shouldRejectRaffleWhenActivityIsDegraded() {
        // Given
        when(activityDegradeSwitch.isDegraded()).thenReturn(true);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> newService().raffle(
                        new ActivityRaffleCommand(USER_ID, ACTIVITY_ID, SKU)
                )
        );

        // Then
        assertEquals(
                BusinessResponseCode.ACTIVITY_DEGRADED.getCode(),
                exception.getCode()
        );
        verifyNoInteractions(
                activityPartakeService,
                strategyRaffleService,
                settlementTransaction,
                activityAccessPolicy
        );
    }

    /**
     * Given 用户未通过活动访问策略，When 执行活动抽奖，Then 返回访问拒绝异常且不创建参与订单。
     *
     * <p>目的：验证动态配置黑白名单已经接入活动抽奖入口，并且拦截点位于
     * 活动参与订单创建之前，避免被拒绝用户消耗活动额度或进入策略抽奖链路。</p>
     */
    @Test
    void shouldRejectRaffleWhenUserIsDeniedByActivityAccessPolicy() {
        // Given
        when(activityAccessPolicy.isAllowed(USER_ID)).thenReturn(false);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> newService().raffle(new ActivityRaffleCommand(USER_ID, ACTIVITY_ID, SKU))
        );

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_ACCESS_DENIED, exception.getResponseCode()),
                () -> assertEquals(BusinessResponseCode.ACTIVITY_ACCESS_DENIED.getCode(), exception.getCode())
        );
        verify(activityAccessPolicy).isAllowed(USER_ID);
        verifyNoInteractions(
                activityPartakeService,
                strategyRaffleService,
                settlementTransaction
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
     * Given 活动按历史累计次数解锁且用户已完成 1 次，When 查询奖品列表，Then 批量返回解锁状态和范围。
     */
    @Test
    void shouldQueryActivityAwardListWithUnlockState() {
        // Given
        ActivityEntity activity = ActivityEntity.builder()
                .activityId(ACTIVITY_ID)
                .activityName("九宫格活动抽奖")
                .strategyId(STRATEGY_ID)
                .awardUnlockScope(AwardUnlockScopeVO.TOTAL)
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
        when(activityRepository.getActivityEntityByIdActivityId(ACTIVITY_ID)).thenReturn(activity);
        when(raffleAward.queryRaffleStrategyAwardList(STRATEGY_ID)).thenReturn(List.of(unlockedAward, lockedAward));
        when(ruleTreeRepository.queryAwardRuleLockCount(List.of("tree_lock_2"))).thenReturn(Map.of("tree_lock_2", 2));
        AwardUnlockCountQueryVO unlockQuery = new AwardUnlockCountQueryVO(
                USER_ID,
                ACTIVITY_ID,
                AwardUnlockScopeVO.TOTAL,
                LocalDate.now()
        );
        when(awardUnlockService.evaluateUnlocks(unlockQuery, Set.of(2)))
                .thenReturn(Map.of(2, AwardUnlockResultVO.evaluate(2, 1L)));
        when(strategyRepository.getStrategyRule(STRATEGY_ID, RuleModel.WEIGHT)).thenReturn(StrategyRuleEntity.builder()
                .strategyId(STRATEGY_ID)
                .ruleModel(RuleModel.WEIGHT.getCode())
                .ruleValue("2:108")
                .build());
        when(strategyRepository.queryUserRaffleCount(USER_ID, STRATEGY_ID)).thenReturn(1L);

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
                () -> assertEquals("total", results.get(1).awardUnlockScope()),
                () -> assertFalse(results.get(1).isAwardUnlock()),
                () -> assertEquals(1, results.get(1).waitUnLockCount()),
                () -> assertNull(results.get(0).awardRuleWeightCount()),
                () -> assertTrue(results.get(0).isAwardWeightUnlock()),
                () -> assertEquals(0, results.get(0).waitAwardWeightCount()),
                () -> assertEquals(2, results.get(1).awardRuleWeightCount()),
                () -> assertFalse(results.get(1).isAwardWeightUnlock()),
                () -> assertEquals(1, results.get(1).waitAwardWeightCount())
        );
        verify(awardUnlockService).evaluateUnlocks(unlockQuery, Set.of(2));
        verify(activityRepository, never()).queryActivityAccountDayByUserId(anyString(), anyLong(), any());
        verify(strategyRepository).queryUserRaffleCount(USER_ID, STRATEGY_ID);
    }

    /**
     * Given 用户没有活动账户，When 查询活动账户额度，Then 返回全 0 结果。
     */
    @Test
    void shouldReturnZeroActivityAccountWhenAccountAbsent() {
        // Given
        when(activityRepository.queryActivityAccountByUserId(USER_ID, ACTIVITY_ID)).thenReturn(null);

        // When
        ActivityAccountResult result = newService().queryActivityAccount(new ActivityAwardListCommand(USER_ID, ACTIVITY_ID));

        // Then
        assertEquals(ActivityAccountResult.zero(), result);
        verify(activityRepository).queryActivityAccountByUserId(USER_ID, ACTIVITY_ID);
        verifyNoMoreInteractions(activityRepository);
    }

    /**
     * Given 用户有总账户但没有当前日/月账户，When 查询活动账户额度，Then 使用总账户中的日/月额度兜底。
     */
    @Test
    void shouldFallbackToTotalAccountQuotaWhenPeriodAccountsAbsent() {
        // Given
        ActivityAccountEntity totalAccount = ActivityAccountEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .totalCount(10)
                .totalCountSurplus(8)
                .dayCount(3)
                .dayCountSurplus(2)
                .monthCount(7)
                .monthCountSurplus(6)
                .build();
        when(activityRepository.queryActivityAccountByUserId(USER_ID, ACTIVITY_ID)).thenReturn(totalAccount);
        when(activityRepository.queryActivityAccountMonthByUserId(USER_ID, ACTIVITY_ID, YearMonth.now())).thenReturn(null);
        when(activityRepository.queryActivityAccountDayByUserId(USER_ID, ACTIVITY_ID, LocalDate.now())).thenReturn(null);

        // When
        ActivityAccountResult result = newService().queryActivityAccount(new ActivityAwardListCommand(USER_ID, ACTIVITY_ID));

        // Then
        assertAll(
                () -> assertEquals(10, result.totalCount()),
                () -> assertEquals(8, result.totalCountSurplus()),
                () -> assertEquals(3, result.dayCount()),
                () -> assertEquals(2, result.dayCountSurplus()),
                () -> assertEquals(7, result.monthCount()),
                () -> assertEquals(6, result.monthCountSurplus())
        );
    }

    /**
     * Given 用户存在总账户和当前日/月账户，When 查询活动账户额度，Then 优先返回当前日/月账户中的额度。
     */
    @Test
    void shouldUsePeriodAccountsWhenTheyExist() {
        // Given
        ActivityAccountEntity totalAccount = ActivityAccountEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .totalCount(10)
                .totalCountSurplus(8)
                .dayCount(3)
                .dayCountSurplus(3)
                .monthCount(7)
                .monthCountSurplus(7)
                .build();
        ActivityAccountMonthEntity monthAccount = ActivityAccountMonthEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .month(YearMonth.now())
                .monthCount(5)
                .monthCountSurplus(4)
                .build();
        ActivityAccountDayEntity dayAccount = ActivityAccountDayEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .day(LocalDate.now())
                .dayCount(2)
                .dayCountSurplus(1)
                .build();
        when(activityRepository.queryActivityAccountByUserId(USER_ID, ACTIVITY_ID)).thenReturn(totalAccount);
        when(activityRepository.queryActivityAccountMonthByUserId(USER_ID, ACTIVITY_ID, YearMonth.now())).thenReturn(monthAccount);
        when(activityRepository.queryActivityAccountDayByUserId(USER_ID, ACTIVITY_ID, LocalDate.now())).thenReturn(dayAccount);

        // When
        ActivityAccountResult result = newService().queryActivityAccount(new ActivityAwardListCommand(USER_ID, ACTIVITY_ID));

        // Then
        assertAll(
                () -> assertEquals(10, result.totalCount()),
                () -> assertEquals(8, result.totalCountSurplus()),
                () -> assertEquals(2, result.dayCount()),
                () -> assertEquals(1, result.dayCountSurplus()),
                () -> assertEquals(5, result.monthCount()),
                () -> assertEquals(4, result.monthCountSurplus())
        );
    }

    private ActivityRaffleApplicationServiceImpl newService() {
        return new ActivityRaffleApplicationServiceImpl(
                activityPartakeService,
                awardUnlockService,
                strategyRaffleService,
                settlementTransaction,
                activityArmory,
                activityRepository,
                raffleAward,
                ruleTreeRepository,
                strategyRepository,
                activityDegradeSwitch,
                activityAccessPolicy
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
