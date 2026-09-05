package com.lavyoung.marketforge.domain.strategy.service.rule.impl;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link RuleWeightLogicFilter} 的权重门槛匹配行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class RuleWeightLogicFilterTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final String RULE_VALUE = "4000:102/103;5000:102/103/104;6000:102/103/104/105";

    @Mock
    private IStrategyRepository repository;

    private RuleWeightLogicFilter filter;
    private RuleMatterEntity ruleMatter;

    /**
     * Given 模拟策略仓储，When 初始化过滤器，Then 使用固定规则物料执行测试。
     */
    @BeforeEach
    void setUp() {
        filter = new RuleWeightLogicFilter(repository);
        ruleMatter = RuleMatterEntity.builder()
                .userId(USER_ID)
                .strategyId(STRATEGY_ID)
                .ruleModel(RuleModel.WEIGHT.getCode())
                .build();
    }

    /**
     * Given 用户分值满足多档门槛，When 执行过滤，Then 命中不超过分值的最高权重档位。
     */
    @Test
    void shouldTakeOverWithHighestEligibleWeight() {
        // Given
        filter.userScore = 5_500L;
        when(repository.queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.WEIGHT.getCode()))
                .thenReturn(RULE_VALUE);

        // When
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> action = filter.filter(ruleMatter);

        // Then
        assertEquals(RuleLogicCheckTypeVO.TAKE_OVER.getCode(), action.code());
        assertEquals(RuleModel.WEIGHT.getCode(), action.ruleModel());
        assertEquals("5000", action.data().ruleWeightValueKey());
        assertEquals(STRATEGY_ID, action.data().strategyId());
    }

    /**
     * Given 用户分值低于最低门槛，When 执行过滤，Then 放行默认抽奖流程。
     */
    @Test
    void shouldAllowWhenUserScoreIsBelowMinimumWeight() {
        // Given
        filter.userScore = 3_999L;
        when(repository.queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.WEIGHT.getCode()))
                .thenReturn(RULE_VALUE);

        // When
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> action = filter.filter(ruleMatter);

        // Then
        assertEquals(RuleLogicCheckTypeVO.ALLOW.getCode(), action.code());
        assertNull(action.data());
    }

    /**
     * Given 权重规则值为空，When 执行过滤，Then 直接放行。
     */
    @Test
    void shouldAllowWhenRuleValueIsBlank() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.WEIGHT.getCode()))
                .thenReturn(null);

        // When
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> action = filter.filter(ruleMatter);

        // Then
        assertEquals(RuleLogicCheckTypeVO.ALLOW.getCode(), action.code());
        assertNull(action.data());
    }

    /**
     * Given 权重规则缺少键值分隔符，When 执行过滤，Then 拒绝非法配置。
     */
    @Test
    void shouldRejectMalformedRuleValue() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.WEIGHT.getCode()))
                .thenReturn("4000-102/103");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> filter.filter(ruleMatter));
    }
}
