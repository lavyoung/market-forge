package com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link DefaultRuleChain} 委托默认概率调度服务选取奖品。
 */
class DefaultRuleChainTest {

    /**
     * Given 已装配的策略概率表，When 执行默认节点，Then 返回调度服务选中的奖品。
     */
    @Test
    void shouldReturnAwardSelectedByDefaultDispatch() {
        // Given
        Long strategyId = 100_001L;
        Long awardId = 100_011L;
        IStrategyDispatch dispatch = mock(IStrategyDispatch.class);
        when(dispatch.getRandomAwardId(strategyId)).thenReturn(awardId);
        DefaultRuleChain chain = new DefaultRuleChain(dispatch);

        // When
        DefaultChainFactory.StrategyAwardVO result = chain.logic("user-001", strategyId);

        // Then
        assertEquals(awardId, result.awardId());
        assertEquals(RuleModel.DEFAULT, result.ruleModel());
        verify(dispatch).getRandomAwardId(strategyId);
    }
}
