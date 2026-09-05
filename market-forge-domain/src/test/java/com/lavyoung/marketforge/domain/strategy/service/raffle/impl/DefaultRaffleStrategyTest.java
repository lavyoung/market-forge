package com.lavyoung.marketforge.domain.strategy.service.raffle.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.*;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.ILogicFilter;
import com.lavyoung.marketforge.domain.strategy.service.rule.factory.DefaultLogicFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link DefaultRaffleStrategy} 的规则编排与抽奖分派行为。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@ExtendWith(MockitoExtension.class)
class DefaultRaffleStrategyTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long DEFAULT_AWARD_ID = 100_011L;
    private static final Long RULE_AWARD_ID = 100_012L;

    @Mock
    private IStrategyRepository repository;

    @Mock
    private IStrategyDispatch strategyDispatch;

    @Mock
    private DefaultLogicFactory logicFactory;

    @Mock
    private ILogicFilter<RuleActionEntity.RaffleBeforeEntity> blackListFilter;

    @Mock
    private ILogicFilter<RuleActionEntity.RaffleBeforeEntity> weightFilter;

    private DefaultRaffleStrategy raffleStrategy;

    /**
     * Given 模拟领域依赖，When 初始化默认策略，Then 注入可控的规则工厂。
     */
    @BeforeEach
    void setUp() {
        raffleStrategy = new DefaultRaffleStrategy(repository, strategyDispatch, logicFactory);
    }

    /**
     * Given 抽奖参数缺少策略标识，When 执行抽奖，Then 抛出业务异常且不访问依赖。
     */
    @Test
    void shouldRejectRaffleFactorWithoutStrategyId() {
        // Given
        RaffleFactorEntity factor = RaffleFactorEntity.builder().userId(USER_ID).build();

        // When
        assertThrows(BusinessException.class, () -> raffleStrategy.performRaffle(factor));

        // Then
        verifyNoInteractions(repository, strategyDispatch, logicFactory);
    }

    /**
     * Given 策略未配置前置规则，When 执行抽奖，Then 使用默认概率表选取奖品。
     */
    @Test
    void shouldUseDefaultDispatchWhenNoRuleTakesOver() {
        // Given
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules(null));
        when(logicFactory.openLogicFilter()).thenReturn(Map.of());
        when(strategyDispatch.getRandomAwardId(STRATEGY_ID)).thenReturn(DEFAULT_AWARD_ID);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(DEFAULT_AWARD_ID, award.getAwardId());
        verify(strategyDispatch).getRandomAwardId(STRATEGY_ID);
        verify(strategyDispatch, never()).getRandomAwardIdAndWeight(any(), any());
    }

    /**
     * Given 黑名单规则接管且同时配置权重规则，When 执行抽奖，Then 优先返回黑名单奖品。
     */
    @Test
    void shouldPrioritizeBlackListTakeover() {
        // Given
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules("rule_weight,rule_blacklist"));
        when(logicFactory.<RuleActionEntity.RaffleBeforeEntity>openLogicFilter()).thenReturn(ruleFilters());
        when(blackListFilter.filter(any(RuleMatterEntity.class)))
                .thenReturn(takeOverAction(RuleModel.RULE_BLACKLIST, RULE_AWARD_ID, null));

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(RULE_AWARD_ID, award.getAwardId());
        verify(weightFilter, never()).filter(any());
        verifyNoInteractions(strategyDispatch);
    }

    /**
     * Given 黑名单放行且权重规则接管，When 执行抽奖，Then 按命中权重概率表选取奖品。
     */
    @Test
    void shouldUseWeightedDispatchWhenWeightRuleTakesOver() {
        // Given
        String weightKey = "5000";
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules("rule_blacklist,rule_weight"));
        when(logicFactory.<RuleActionEntity.RaffleBeforeEntity>openLogicFilter()).thenReturn(ruleFilters());
        when(blackListFilter.filter(any(RuleMatterEntity.class))).thenReturn(allowAction());
        when(weightFilter.filter(any(RuleMatterEntity.class)))
                .thenReturn(takeOverAction(RuleModel.WEIGHT, null, weightKey));
        when(strategyDispatch.getRandomAwardIdAndWeight(STRATEGY_ID, weightKey))
                .thenReturn(DEFAULT_AWARD_ID);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(DEFAULT_AWARD_ID, award.getAwardId());
        InOrder ruleOrder = inOrder(blackListFilter, weightFilter);
        ruleOrder.verify(blackListFilter).filter(any(RuleMatterEntity.class));
        ruleOrder.verify(weightFilter).filter(any(RuleMatterEntity.class));
        verify(strategyDispatch).getRandomAwardIdAndWeight(STRATEGY_ID, weightKey);
        verify(strategyDispatch, never()).getRandomAwardId(any());
    }

    /**
     * 创建包含固定用户和策略标识的有效抽奖因子。
     *
     * @return 有效抽奖因子
     */
    private RaffleFactorEntity validFactor() {
        return RaffleFactorEntity.builder()
                .userId(USER_ID)
                .strategyId(STRATEGY_ID)
                .build();
    }

    /**
     * 创建指定规则配置的策略实体。
     *
     * @param ruleModels 逗号分隔的规则模型编码
     * @return 策略实体
     */
    private StrategyEntity strategyWithRules(String ruleModels) {
        return StrategyEntity.builder()
                .strategyId(STRATEGY_ID)
                .ruleModels(ruleModels)
                .build();
    }

    /**
     * 创建用于测试的前置规则过滤器映射。
     *
     * @return 黑名单与权重过滤器映射
     */
    private Map<RuleModel, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> ruleFilters() {
        return Map.of(
                RuleModel.RULE_BLACKLIST, blackListFilter,
                RuleModel.WEIGHT, weightFilter
        );
    }

    /**
     * 创建规则放行动作。
     *
     * @return 放行动作
     */
    private RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> allowAction() {
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .msg(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }

    /**
     * 创建规则接管动作。
     *
     * @param ruleModel 接管流程的规则模型
     * @param awardId   规则指定的奖品标识
     * @param weightKey 命中的权重规则值
     * @return 接管动作
     */
    private RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> takeOverAction(
            RuleModel ruleModel,
            Long awardId,
            String weightKey) {
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                .msg(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                .ruleModel(ruleModel.getCode())
                .data(RuleActionEntity.RaffleBeforeEntity.builder()
                        .strategyId(STRATEGY_ID)
                        .awardId(awardId)
                        .ruleWeightValueKey(weightKey)
                        .build())
                .build();
    }
}
