package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证次数锁、幸运奖及默认终止节点对正常配置和异常配置的处理。
 */
class RuleTreeNodeTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;

    /**
     * Given 奖品要求三次抽奖后解锁，When 当前开发基准次数为两次，Then 次数锁接管流程。
     */
    @Test
    void shouldTakeOverWhenAwardIsStillLocked() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, AWARD_ID, RuleModel.LOCK.getCode())).thenReturn("3");

        // When
        DefaultTreeFactory.TreeActionEntity result = new RuleLockLogicTreeNode(repository)
                .logic(USER_ID, STRATEGY_ID, AWARD_ID, "strategy_rule");

        // Then
        assertAll(
                () -> assertEquals(RuleLogicCheckTypeVO.TAKE_OVER, result.ruleLogicCheckTypeVO()),
                () -> assertNull(result.strategyAwardVO().awardId()),
                () -> assertEquals(RuleModel.LOCK, result.strategyAwardVO().ruleModel())
        );
    }

    /**
     * Given 次数锁配置不是整数，When 执行节点，Then 抛出规则值业务异常。
     */
    @Test
    void shouldRejectInvalidLockRuleValue() {
        // Given
        IStrategyRepository repository = mock(IStrategyRepository.class);
        when(repository.queryStrategyRuleValue(STRATEGY_ID, AWARD_ID, RuleModel.LOCK.getCode())).thenReturn("invalid");

        // When & Then
        assertThrows(BusinessException.class, () -> new RuleLockLogicTreeNode(repository)
                .logic(USER_ID, STRATEGY_ID, AWARD_ID, "strategy_rule"));
    }

    /**
     * Given 幸运奖配置合法，When 执行节点，Then 返回配置的奖品和数量范围。
     */
    @Test
    void shouldParseLuckAwardConfiguration() {
        // Given
        RuleLuckAwardLogicTreeNode node = new RuleLuckAwardLogicTreeNode();

        // When
        DefaultTreeFactory.TreeActionEntity result =
                node.logic(USER_ID, STRATEGY_ID, AWARD_ID, "900901011:1/100");

        // Then
        assertAll(
                () -> assertEquals(900_901_011L, result.strategyAwardVO().awardId()),
                () -> assertEquals("1/100", result.strategyAwardVO().awardRuleValue()),
                () -> assertEquals(RuleLogicCheckTypeVO.TAKE_OVER, result.ruleLogicCheckTypeVO())
        );
    }

    /**
     * Given 幸运奖ID无法转换为数字，When 执行节点，Then 抛出规则值业务异常。
     */
    @Test
    void shouldRejectInvalidLuckAwardId() {
        // Given
        RuleLuckAwardLogicTreeNode node = new RuleLuckAwardLogicTreeNode();

        // When & Then
        assertThrows(BusinessException.class,
                () -> node.logic(USER_ID, STRATEGY_ID, AWARD_ID, "award-x:1/100"));
    }

    /**
     * Given 前置节点全部放行，When 到达默认叶子节点，Then 保留原候选奖品。
     */
    @Test
    void shouldKeepAwardAtDefaultLeaf() {
        // Given
        DefaultLogicTreeNode node = new DefaultLogicTreeNode();

        // When
        DefaultTreeFactory.TreeActionEntity result = node.logic(USER_ID, STRATEGY_ID, AWARD_ID, "");

        // Then
        assertAll(
                () -> assertEquals(AWARD_ID, result.strategyAwardVO().awardId()),
                () -> assertEquals(RuleModel.DEFAULT, result.strategyAwardVO().ruleModel()),
                () -> assertEquals(RuleLogicCheckTypeVO.ALLOW, result.ruleLogicCheckTypeVO())
        );
    }
}
