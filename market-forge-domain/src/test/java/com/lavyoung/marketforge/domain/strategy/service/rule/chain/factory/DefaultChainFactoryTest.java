package com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link DefaultChainFactory} 按策略配置顺序装配责任链的行为。
 */
class DefaultChainFactoryTest {

    private static final Long STRATEGY_ID = 100_001L;

    /**
     * Given 策略未配置规则，When 打开责任链，Then 直接返回默认抽奖节点。
     */
    @Test
    void shouldReturnDefaultChainWhenStrategyHasNoRules() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        ILogicChain defaultChain = mock(ILogicChain.class);
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategy(null));
        DefaultChainFactory factory = new DefaultChainFactory(
                Map.of(RuleModel.DEFAULT, defaultChain), repository
        );

        // When
        ILogicChain result = factory.openLogicChain(STRATEGY_ID);

        // Then
        assertSame(defaultChain, result);
        verify(defaultChain, never()).appendNex(any());
    }

    /**
     * Given 策略以低优先级在前的顺序配置规则，When 打开责任链，Then 按优先级重排并追加默认节点。
     */
    @Test
    void shouldAssembleConfiguredChainsAndAppendDefaultChain() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        ILogicChain blackListChain = mock(ILogicChain.class);
        ILogicChain weightChain = mock(ILogicChain.class);
        ILogicChain defaultChain = mock(ILogicChain.class);
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategy("rule_weight,rule_blacklist"));
        when(blackListChain.appendNex(weightChain)).thenReturn(weightChain);
        when(weightChain.appendNex(defaultChain)).thenReturn(defaultChain);
        DefaultChainFactory factory = new DefaultChainFactory(
                Map.of(
                        RuleModel.RULE_BLACKLIST, blackListChain,
                        RuleModel.WEIGHT, weightChain,
                        RuleModel.DEFAULT, defaultChain
                ),
                repository
        );

        // When
        ILogicChain result = factory.openLogicChain(STRATEGY_ID);

        // Then
        assertSame(blackListChain, result);
        verify(blackListChain).appendNex(weightChain);
        verify(weightChain).appendNex(defaultChain);
    }

    /**
     * Given 策略不存在，When 打开责任链，Then 抛出业务异常而不是返回不完整链路。
     */
    @Test
    void shouldRejectMissingStrategy() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID)).thenReturn(null);
        DefaultChainFactory factory = new DefaultChainFactory(Map.of(), repository);

        // When & Then
        assertThrows(BusinessException.class, () -> factory.openLogicChain(STRATEGY_ID));
    }

    /**
     * 创建指定规则配置的策略实体。
     *
     * @param ruleModels 逗号分隔的规则模型编码
     * @return 策略实体
     */
    private StrategyEntity strategy(String ruleModels) {
        return StrategyEntity.builder().strategyId(STRATEGY_ID).ruleModels(ruleModels).build();
    }
}
