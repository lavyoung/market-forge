package com.lavyoung.marketforge.app.strategy;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.raffle.IRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.raffle.impl.DefaultRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.rule.factory.DefaultLogicFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.impl.RuleBlackListLogicFilter;
import com.lavyoung.marketforge.domain.strategy.service.rule.impl.RuleWeightLogicFilter;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * 使用真实规则过滤器和模拟外部端口验证抽奖策略的规则编排与调度行为。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/05
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class RaffleStrategyRunnerTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long DEFAULT_AWARD_ID = 100_011L;
    private static final Long BLACKLIST_AWARD_ID = 100_012L;
    private static final String WEIGHT_KEY = "4000";

    @Mock
    private IStrategyRepository repository;

    @Mock
    private IStrategyDispatch strategyDispatch;

    private IRaffleStrategy raffleStrategy;
    private RuleWeightLogicFilter ruleWeightLogicFilter;

    /**
     * Given 模拟外部端口，When 初始化真实规则链，Then 使用固定用户分值执行每个场景。
     */
    @BeforeEach
    void setUp() {
        RuleBlackListLogicFilter blackListLogicFilter = new RuleBlackListLogicFilter(repository);
        ruleWeightLogicFilter = new RuleWeightLogicFilter(repository);
        ruleWeightLogicFilter.userScore = 4_500L;
        DefaultLogicFactory logicFactory = new DefaultLogicFactory(
                List.of(blackListLogicFilter, ruleWeightLogicFilter)
        );
        raffleStrategy = new DefaultRaffleStrategy(repository, strategyDispatch, logicFactory);
    }

    /**
     * Given 策略没有前置规则，When 执行抽奖，Then 从默认概率表返回奖品。
     */
    @Test
    @DisplayName("无规则接管时执行默认抽奖")
    void shouldPerformDefaultRaffle() {
        // Given
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules(null));
        when(strategyDispatch.getRandomAwardId(STRATEGY_ID)).thenReturn(DEFAULT_AWARD_ID);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(DEFAULT_AWARD_ID, award.getAwardId());
        verify(strategyDispatch).getRandomAwardId(STRATEGY_ID);
        verify(strategyDispatch, never()).getRandomAwardIdAndWeight(STRATEGY_ID, WEIGHT_KEY);
    }

    /**
     * Given 当前用户命中黑名单，When 执行抽奖，Then 黑名单规则接管并返回指定奖品。
     */
    @Test
    @DisplayName("黑名单用户获得规则指定奖品")
    void shouldReturnConfiguredAwardForBlacklistedUser() {
        // Given
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules(RuleModel.RULE_BLACKLIST.getCode()));
        when(repository.queryStrategyRuleValue(
                STRATEGY_ID,
                null,
                RuleModel.RULE_BLACKLIST.getCode()
        )).thenReturn(BLACKLIST_AWARD_ID + ":user-002/" + USER_ID);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(BLACKLIST_AWARD_ID, award.getAwardId());
        verifyNoInteractions(strategyDispatch);
    }

    /**
     * Given 用户分值达到权重门槛，When 执行抽奖，Then 使用对应权重概率表返回奖品。
     */
    @Test
    @DisplayName("达到积分门槛时执行权重抽奖")
    void shouldPerformWeightedRaffleForEligibleUser() {
        // Given
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules(RuleModel.WEIGHT.getCode()));
        when(repository.queryStrategyRuleValue(
                STRATEGY_ID,
                null,
                RuleModel.WEIGHT.getCode()
        )).thenReturn("4000:100011/100012;5000:100011/100012/100013");
        when(strategyDispatch.getRandomAwardIdAndWeight(STRATEGY_ID, WEIGHT_KEY))
                .thenReturn(DEFAULT_AWARD_ID);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(DEFAULT_AWARD_ID, award.getAwardId());
        verify(strategyDispatch).getRandomAwardIdAndWeight(STRATEGY_ID, WEIGHT_KEY);
        verify(strategyDispatch, never()).getRandomAwardId(STRATEGY_ID);
    }

    /**
     * 创建有效抽奖因子。
     *
     * @return 包含固定用户和策略标识的抽奖因子
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
}
