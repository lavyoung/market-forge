package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockResultVO;
import com.lavyoung.marketforge.domain.award.service.IAwardUnlockService;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyAwardStockMessageRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证次数锁、库存、幸运奖及默认节点对完整抽奖上下文的处理。
 */
class RuleTreeNodeTest {

    private static final String USER_ID = "user-001";
    private static final Long ACTIVITY_ID = 100_301L;
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;
    private static final LocalDate STATISTICS_DATE = LocalDate.of(2026, 10, 18);
    private static final LocalDateTime END_DATE_TIME = LocalDateTime.of(2026, 10, 31, 23, 59);

    /**
     * Given 活动按日统计且用户当天次数不足，When 执行次数锁节点，Then 接管当前候选奖品。
     */
    @Test
    void shouldTakeOverWhenDailyAwardIsStillLocked() {
        IStrategyRepository strategyRepository = mock(IStrategyRepository.class);
        IAwardUnlockService awardUnlockService = mock(IAwardUnlockService.class);
        RaffleFactorEntity factor = activityFactor(AwardUnlockScopeVO.DAY);
        AwardUnlockCountQueryVO query = new AwardUnlockCountQueryVO(USER_ID, ACTIVITY_ID, AwardUnlockScopeVO.DAY, STATISTICS_DATE);
        when(strategyRepository.queryAwardRuleLockCounts(STRATEGY_ID, List.of(AWARD_ID))).thenReturn(Map.of(AWARD_ID, 3));
        when(awardUnlockService.evaluateUnlock(query, 3)).thenReturn(AwardUnlockResultVO.evaluate(3, 2));

        DefaultTreeFactory.TreeActionEntity result = new RuleLockLogicTreeNode(strategyRepository, awardUnlockService).logic(factor, "strategy_rule");

        assertAll(
                () -> assertEquals(RuleLogicCheckTypeVO.TAKE_OVER, result.ruleLogicCheckTypeVO()),
                () -> assertNull(result.strategyAwardVO().awardId()),
                () -> assertEquals(RuleModel.LOCK, result.strategyAwardVO().ruleModel())
        );
        verify(awardUnlockService).evaluateUnlock(query, 3);
        verify(strategyRepository, never()).queryUserRaffleCount(anyString(), anyLong());
    }

    /**
     * Given 活动按累计次数统计且用户达到门槛，When 执行次数锁节点，Then 放行原候选奖品。
     */
    @Test
    void shouldAllowWhenTotalAwardIsUnlocked() {
        IStrategyRepository strategyRepository = mock(IStrategyRepository.class);
        IAwardUnlockService awardUnlockService = mock(IAwardUnlockService.class);
        RaffleFactorEntity factor = activityFactor(AwardUnlockScopeVO.TOTAL);
        AwardUnlockCountQueryVO query = new AwardUnlockCountQueryVO(USER_ID, ACTIVITY_ID, AwardUnlockScopeVO.TOTAL, STATISTICS_DATE);
        when(strategyRepository.queryAwardRuleLockCounts(STRATEGY_ID, List.of(AWARD_ID))).thenReturn(Map.of(AWARD_ID, 3));
        when(awardUnlockService.evaluateUnlock(query, 3)).thenReturn(AwardUnlockResultVO.evaluate(3, 3));

        DefaultTreeFactory.TreeActionEntity result = new RuleLockLogicTreeNode(strategyRepository, awardUnlockService).logic(factor, "strategy_rule");

        assertAll(
                () -> assertEquals(RuleLogicCheckTypeVO.ALLOW, result.ruleLogicCheckTypeVO()),
                () -> assertEquals(AWARD_ID, result.strategyAwardVO().awardId())
        );
        verify(awardUnlockService).evaluateUnlock(query, 3);
    }

    /**
     * Given 抽奖因子只携带部分活动上下文，When 执行次数锁节点，Then 拒绝使用不明确的统计口径。
     */
    @Test
    void shouldRejectPartialActivityContext() {
        IStrategyRepository strategyRepository = mock(IStrategyRepository.class);
        IAwardUnlockService awardUnlockService = mock(IAwardUnlockService.class);
        RaffleFactorEntity factor = RaffleFactorEntity.builder().userId(USER_ID).strategyId(STRATEGY_ID).awardId(AWARD_ID).activityId(ACTIVITY_ID).build();
        when(strategyRepository.queryAwardRuleLockCounts(STRATEGY_ID, List.of(AWARD_ID))).thenReturn(Map.of(AWARD_ID, 3));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> new RuleLockLogicTreeNode(strategyRepository, awardUnlockService).logic(factor, "strategy_rule"));

        assertEquals(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception.getResponseCode());
        verifyNoInteractions(awardUnlockService);
    }

    /**
     * Given 非活动抽奖没有活动上下文，When 执行次数锁节点，Then 兼容使用策略累计次数。
     */
    @Test
    void shouldUseStrategyCountForLegacyRaffle() {
        IStrategyRepository strategyRepository = mock(IStrategyRepository.class);
        IAwardUnlockService awardUnlockService = mock(IAwardUnlockService.class);
        RaffleFactorEntity factor = RaffleFactorEntity.builder().userId(USER_ID).strategyId(STRATEGY_ID).awardId(AWARD_ID).build();
        when(strategyRepository.queryAwardRuleLockCounts(STRATEGY_ID, List.of(AWARD_ID))).thenReturn(Map.of(AWARD_ID, 3));
        when(strategyRepository.queryUserRaffleCount(USER_ID, STRATEGY_ID)).thenReturn(2L);

        DefaultTreeFactory.TreeActionEntity result = new RuleLockLogicTreeNode(strategyRepository, awardUnlockService).logic(factor, "strategy_rule");

        assertEquals(RuleLogicCheckTypeVO.TAKE_OVER, result.ruleLogicCheckTypeVO());
        verifyNoInteractions(awardUnlockService);
    }

    /**
     * Given 奖品库存充足且抽奖因子带有活动结束时间，When 执行库存节点，Then 使用结束时间扣减并发送同步消息。
     */
    @Test
    void shouldPassActivityEndTimeToStockDeduction() {
        IStrategyAwardStockMessageRepository messageRepository = mock(IStrategyAwardStockMessageRepository.class);
        IStrategyDispatch strategyDispatch = mock(IStrategyDispatch.class);
        RaffleFactorEntity factor = activityFactor(AwardUnlockScopeVO.TOTAL);
        when(strategyDispatch.subtractAwardStock(STRATEGY_ID, AWARD_ID, END_DATE_TIME)).thenReturn(true);

        DefaultTreeFactory.TreeActionEntity result = new RuleStockLogicTreeNode(messageRepository, strategyDispatch).logic(factor, null);

        assertEquals(RuleLogicCheckTypeVO.ALLOW, result.ruleLogicCheckTypeVO());
        verify(messageRepository).send(new StrategyAwardStockKeyVO(STRATEGY_ID, AWARD_ID, USER_ID));
    }

    /**
     * Given 幸运奖配置合法，When 执行节点，Then 返回配置的奖品和数量范围。
     */
    @Test
    void shouldParseLuckAwardConfiguration() {
        DefaultTreeFactory.TreeActionEntity result = new RuleLuckAwardLogicTreeNode().logic(activityFactor(AwardUnlockScopeVO.TOTAL), "900901011:1/100");

        assertAll(
                () -> assertEquals(900_901_011L, result.strategyAwardVO().awardId()),
                () -> assertEquals("1/100", result.strategyAwardVO().awardRuleValue()),
                () -> assertEquals(RuleLogicCheckTypeVO.TAKE_OVER, result.ruleLogicCheckTypeVO())
        );
    }

    /**
     * Given 幸运奖标识无法转换为数字，When 执行节点，Then 抛出规则值业务异常。
     */
    @Test
    void shouldRejectInvalidLuckAwardId() {
        assertThrows(BusinessException.class,
                () -> new RuleLuckAwardLogicTreeNode().logic(activityFactor(AwardUnlockScopeVO.TOTAL), "award-x:1/100"));
    }

    /**
     * Given 前置节点全部放行，When 到达默认叶子节点，Then 保留原候选奖品。
     */
    @Test
    void shouldKeepAwardAtDefaultLeaf() {
        DefaultTreeFactory.TreeActionEntity result = new DefaultLogicTreeNode().logic(activityFactor(AwardUnlockScopeVO.TOTAL), "");

        assertAll(
                () -> assertEquals(AWARD_ID, result.strategyAwardVO().awardId()),
                () -> assertEquals(RuleModel.DEFAULT, result.strategyAwardVO().ruleModel()),
                () -> assertEquals(RuleLogicCheckTypeVO.ALLOW, result.ruleLogicCheckTypeVO())
        );
    }

    /**
     * 创建带有完整活动上下文和候选奖品的抽奖因子。
     *
     * @param unlockScope 活动配置的奖品解锁统计范围
     * @return 可进入规则树执行的抽奖因子
     */
    private RaffleFactorEntity activityFactor(AwardUnlockScopeVO unlockScope) {
        return RaffleFactorEntity.builder()
                .userId(USER_ID)
                .strategyId(STRATEGY_ID)
                .awardId(AWARD_ID)
                .activityId(ACTIVITY_ID)
                .awardUnlockScope(unlockScope)
                .statisticsDate(STATISTICS_DATE)
                .endDateTime(END_DATE_TIME)
                .build();
    }
}
