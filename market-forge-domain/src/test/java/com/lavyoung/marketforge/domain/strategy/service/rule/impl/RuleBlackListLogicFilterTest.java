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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link RuleBlackListLogicFilter} 的黑名单匹配行为。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@ExtendWith(MockitoExtension.class)
class RuleBlackListLogicFilterTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long BLACKLIST_AWARD_ID = 100_011L;

    @Mock
    private IStrategyRepository repository;

    private RuleBlackListLogicFilter filter;
    private RuleMatterEntity ruleMatter;

    /**
     * Given 模拟策略仓储，When 初始化过滤器，Then 使用固定规则物料执行测试。
     */
    @BeforeEach
    void setUp() {
        filter = new RuleBlackListLogicFilter(repository);
        ruleMatter = RuleMatterEntity.builder()
                .userId(USER_ID)
                .strategyId(STRATEGY_ID)
                .ruleModel(RuleModel.RULE_BLACKLIST.getCode())
                .build();
    }

    /**
     * Given 用户位于黑名单中，When 执行过滤，Then 规则接管并返回指定奖品。
     */
    @Test
    void shouldTakeOverAndReturnConfiguredAwardWhenUserIsBlacklisted() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn(BLACKLIST_AWARD_ID + ":user-002/" + USER_ID);

        // When
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> action = filter.filter(ruleMatter);

        // Then
        assertEquals(RuleLogicCheckTypeVO.TAKE_OVER.getCode(), action.getCode());
        assertEquals(RuleModel.RULE_BLACKLIST.getCode(), action.getRuleModel());
        assertEquals(STRATEGY_ID, action.getData().getStrategyId());
        assertEquals(BLACKLIST_AWARD_ID, action.getData().getAwardId());
        verify(repository).queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.RULE_BLACKLIST.getCode());
    }

    /**
     * Given 用户不在黑名单中，When 执行过滤，Then 放行且不返回规则数据。
     */
    @Test
    void shouldAllowWhenUserIsNotBlacklisted() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn(BLACKLIST_AWARD_ID + ":user-002/user-003");

        // When
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> action = filter.filter(ruleMatter);

        // Then
        assertEquals(RuleLogicCheckTypeVO.ALLOW.getCode(), action.getCode());
        assertNull(action.getData());
    }

    /**
     * Given 黑名单规则值为空，When 执行过滤，Then 直接放行。
     */
    @Test
    void shouldAllowWhenRuleValueIsBlank() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn(" ");

        // When
        RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> action = filter.filter(ruleMatter);

        // Then
        assertEquals(RuleLogicCheckTypeVO.ALLOW.getCode(), action.getCode());
        assertNull(action.getData());
    }

    /**
     * Given 奖品标识格式非法，When 执行过滤，Then 抛出数字格式异常。
     */
    @Test
    void shouldRejectRuleValueWithInvalidAwardId() {
        // Given
        when(repository.queryStrategyRuleValue(STRATEGY_ID, null, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn("invalid:" + USER_ID);

        // When & Then
        assertThrows(NumberFormatException.class, () -> filter.filter(ruleMatter));
    }
}
