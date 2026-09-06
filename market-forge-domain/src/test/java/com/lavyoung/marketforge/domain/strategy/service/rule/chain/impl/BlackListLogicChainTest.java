package com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link BlackListLogicChain} 的黑名单接管与责任链放行行为。
 */
class BlackListLogicChainTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;

    /**
     * Given 用户命中黑名单，When 执行节点，Then 直接返回规则指定奖品且不调用后继节点。
     */
    @Test
    void shouldReturnConfiguredAwardWhenUserIsBlacklisted() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        ILogicChain next = mock(ILogicChain.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn(AWARD_ID + ":user-002/" + USER_ID);
        BlackListLogicChain chain = new BlackListLogicChain(repository);
        chain.appendNex(next);

        // When
        Long result = chain.logic(USER_ID, STRATEGY_ID);

        // Then
        assertEquals(AWARD_ID, result);
        verifyNoInteractions(next);
    }

    /**
     * Given 用户未命中黑名单，When 执行节点，Then 使用原策略标识调用后继节点。
     */
    @Test
    void shouldPassOriginalStrategyIdToNextChainWhenUserIsNotBlacklisted() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        ILogicChain next = mock(ILogicChain.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn(AWARD_ID + ":user-002/user-003");
        when(next.logic(USER_ID, STRATEGY_ID)).thenReturn(AWARD_ID);
        BlackListLogicChain chain = new BlackListLogicChain(repository);
        chain.appendNex(next);

        // When
        Long result = chain.logic(USER_ID, STRATEGY_ID);

        // Then
        assertEquals(AWARD_ID, result);
        verify(next).logic(USER_ID, STRATEGY_ID);
    }

    /**
     * Given 黑名单规则为空，When 执行节点，Then 继续调用后继节点。
     */
    @Test
    void shouldContinueChainWhenRuleValueIsBlank() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        ILogicChain next = mock(ILogicChain.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn(" ");
        when(next.logic(USER_ID, STRATEGY_ID)).thenReturn(AWARD_ID);
        BlackListLogicChain chain = new BlackListLogicChain(repository);
        chain.appendNex(next);

        // When
        Long result = chain.logic(USER_ID, STRATEGY_ID);

        // Then
        assertEquals(AWARD_ID, result);
        verify(next).logic(USER_ID, STRATEGY_ID);
    }

    /**
     * Given 黑名单奖品标识非法，When 执行节点，Then 抛出数字格式异常。
     */
    @Test
    void shouldRejectInvalidAwardId() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn("invalid:" + USER_ID);
        BlackListLogicChain chain = new BlackListLogicChain(repository);

        // When & Then
        assertThrows(NumberFormatException.class, () -> chain.logic(USER_ID, STRATEGY_ID));
    }
}
