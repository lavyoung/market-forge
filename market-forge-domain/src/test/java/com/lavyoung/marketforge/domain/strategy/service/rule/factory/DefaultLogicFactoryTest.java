package com.lavyoung.marketforge.domain.strategy.service.rule.factory;

import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.ILogicFilter;
import com.lavyoung.marketforge.domain.strategy.service.rule.impl.RuleBlackListLogicFilter;
import com.lavyoung.marketforge.domain.strategy.service.rule.impl.RuleWeightLogicFilter;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * 验证 {@link DefaultLogicFactory} 基于注解注册规则过滤器的行为。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
class DefaultLogicFactoryTest {

    /**
     * Given 包含已注解和未注解的过滤器，When 创建工厂，Then 仅注册声明了规则模型的实现。
     */
    @Test
    void shouldRegisterOnlyAnnotatedLogicFilters() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        RuleBlackListLogicFilter blackListFilter = new RuleBlackListLogicFilter(repository);
        RuleWeightLogicFilter weightFilter = new RuleWeightLogicFilter(repository);
        ILogicFilter<RuleActionEntity.RaffleBeforeEntity> unannotatedFilter = ruleMatter -> null;

        // When
        DefaultLogicFactory factory = new DefaultLogicFactory(
                List.of(blackListFilter, weightFilter, unannotatedFilter)
        );
        Map<RuleModel, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> filters =
                factory.openLogicFilter();

        // Then
        assertEquals(2, filters.size());
        assertSame(blackListFilter, filters.get(RuleModel.RULE_BLACKLIST));
        assertSame(weightFilter, filters.get(RuleModel.WEIGHT));
    }
}
