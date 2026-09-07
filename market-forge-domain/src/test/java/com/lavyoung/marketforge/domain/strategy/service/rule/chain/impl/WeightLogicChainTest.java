package com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link WeightLogicChain} 的权重档位匹配与责任链放行行为。
 */
class WeightLogicChainTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;

    /**
     * Given 默认用户分值达到多个档位，When 执行权重节点，Then 使用最高可用档位抽奖。
     */
    @Test
    void shouldDispatchWithHighestEligibleWeight() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        IStrategyDispatch dispatch = mock(IStrategyDispatch.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.WEIGHT.getCode()))
                .thenReturn("3000:100011;4000:100011/100012;5000:100011/100012/100013");
        when(dispatch.getRandomAwardIdAndWeight(STRATEGY_ID, "4000")).thenReturn(AWARD_ID);
        WeightLogicChain chain = new WeightLogicChain(repository, dispatch);

        // When
        DefaultChainFactory.StrategyAwardVO result = chain.logic(USER_ID, STRATEGY_ID);

        // Then
        assertEquals(AWARD_ID, result.awardId());
        assertEquals(RuleModel.WEIGHT, result.ruleModel());
        verify(dispatch).getRandomAwardIdAndWeight(STRATEGY_ID, "4000");
    }

    /**
     * Given 用户分值低于最低权重档位，When 执行节点，Then 将请求传递给后继节点。
     */
    @Test
    void shouldContinueChainWhenNoWeightIsEligible() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        IStrategyDispatch dispatch = mock(IStrategyDispatch.class);
        ILogicChain next = mock(ILogicChain.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.WEIGHT.getCode()))
                .thenReturn("5000:100011;6000:100012");
        DefaultChainFactory.StrategyAwardVO nextResult = strategyAward();
        when(next.logic(USER_ID, STRATEGY_ID)).thenReturn(nextResult);
        WeightLogicChain chain = new WeightLogicChain(repository, dispatch);
        chain.appendNex(next);

        // When
        DefaultChainFactory.StrategyAwardVO result = chain.logic(USER_ID, STRATEGY_ID);

        // Then
        assertSame(nextResult, result);
        verify(next).logic(USER_ID, STRATEGY_ID);
        verifyNoInteractions(dispatch);
    }

    /**
     * Given 权重规则值为空，When 执行节点，Then 将请求传递给后继节点。
     */
    @Test
    void shouldContinueChainWhenRuleValueIsBlank() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        IStrategyDispatch dispatch = mock(IStrategyDispatch.class);
        ILogicChain next = mock(ILogicChain.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.WEIGHT.getCode())).thenReturn(null);
        DefaultChainFactory.StrategyAwardVO nextResult = strategyAward();
        when(next.logic(USER_ID, STRATEGY_ID)).thenReturn(nextResult);
        WeightLogicChain chain = new WeightLogicChain(repository, dispatch);
        chain.appendNex(next);

        // When
        DefaultChainFactory.StrategyAwardVO result = chain.logic(USER_ID, STRATEGY_ID);

        // Then
        assertSame(nextResult, result);
        verify(next).logic(USER_ID, STRATEGY_ID);
    }

    /**
     * Given 权重规则缺少键值分隔符，When 执行节点，Then 抛出非法参数异常。
     */
    @Test
    void shouldRejectMalformedRuleValue() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        IStrategyDispatch dispatch = mock(IStrategyDispatch.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.WEIGHT.getCode()))
                .thenReturn("4000-100011");
        WeightLogicChain chain = new WeightLogicChain(repository, dispatch);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> chain.logic(USER_ID, STRATEGY_ID));
    }

    /**
     * 创建默认节点返回的固定奖品结果。
     *
     * @return 默认责任链奖品结果
     */
    private DefaultChainFactory.StrategyAwardVO strategyAward() {
        return DefaultChainFactory.StrategyAwardVO.builder()
                .awardId(AWARD_ID)
                .ruleModel(RuleModel.DEFAULT)
                .build();
    }
}
