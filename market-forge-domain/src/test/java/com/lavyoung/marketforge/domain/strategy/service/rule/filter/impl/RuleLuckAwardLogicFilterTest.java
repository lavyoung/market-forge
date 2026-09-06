package com.lavyoung.marketforge.domain.strategy.service.rule.filter.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleMatterEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link RuleLuckAwardLogicFilter} 根据抽奖次数阈值决定放行或接管。
 */
@ExtendWith(MockitoExtension.class)
class RuleLuckAwardLogicFilterTest {

    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;

    @Mock
    private IStrategyRepository repository;

    private RuleLuckAwardLogicFilter filter;
    private RuleMatterEntity ruleMatter;

    /**
     * Given 模拟策略仓储，When 初始化过滤器，Then 使用固定执行中规则物料测试。
     */
    @BeforeEach
    void setUp() {
        filter = new RuleLuckAwardLogicFilter(repository);
        ruleMatter = RuleMatterEntity.builder()
                .userId("user-001")
                .strategyId(STRATEGY_ID)
                .awardId(AWARD_ID)
                .ruleModel(RuleModel.LUCK_AWARD.getCode())
                .build();
    }

    /**
     * Given 当前抽奖次数达到零阈值，When 执行过滤，Then 放行原奖品结果。
     */
    @Test
    void shouldAllowWhenRaffleCountReachesThreshold() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, AWARD_ID, RuleModel.LUCK_AWARD.getCode()))
                .thenReturn("0");

        // When
        RuleActionEntity<RuleActionEntity.RaffleExecutingEntity> action = filter.filter(ruleMatter);

        // Then
        assertEquals(RuleLogicCheckTypeVO.ALLOW.getCode(), action.code());
    }

    /**
     * Given 当前抽奖次数未达到阈值，When 执行过滤，Then 幸运奖规则接管结果。
     */
    @Test
    void shouldTakeOverWhenRaffleCountIsBelowThreshold() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, AWARD_ID, RuleModel.LUCK_AWARD.getCode()))
                .thenReturn("1");

        // When
        RuleActionEntity<RuleActionEntity.RaffleExecutingEntity> action = filter.filter(ruleMatter);

        // Then
        assertEquals(RuleLogicCheckTypeVO.TAKE_OVER.getCode(), action.code());
        assertEquals(RuleModel.LUCK_AWARD.getCode(), action.ruleModel());
    }

    /**
     * Given 规则阈值不是整数，When 执行过滤，Then 抛出数字格式异常。
     */
    @Test
    void shouldRejectInvalidThreshold() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, AWARD_ID, RuleModel.LUCK_AWARD.getCode()))
                .thenReturn("invalid");

        // When & Then
        assertThrows(NumberFormatException.class, () -> filter.filter(ruleMatter));
    }
}
