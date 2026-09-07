package com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.*;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link DecisionTreeEngine} 的叶子终止与节点连线匹配行为。
 */
class DecisionTreeEngineTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;
    private static final String ROOT_RULE = "rule_root";

    /**
     * Given 根节点没有后继连线且节点注册表不可接受空键，When 执行规则树，Then 正常返回叶子节点结果。
     */
    @Test
    void shouldReturnLeafDecisionWithoutLookingUpNullNode() {
        // Given
        ILogicTreeNode rootNode = mock(ILogicTreeNode.class);
        DefaultTreeFactory.StrategyAwardVO expected = DefaultTreeFactory.StrategyAwardVO.builder()
                .awardId(AWARD_ID)
                .ruleModel(RuleModel.DEFAULT)
                .build();
        when(rootNode.logic(USER_ID, STRATEGY_ID, AWARD_ID)).thenReturn(
                DefaultTreeFactory.TreeActionEntity.builder()
                        .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.ALLOW)
                        .strategyAwardVO(expected)
                        .build()
        );
        DecisionTreeEngine engine = new DecisionTreeEngine(
                Map.of(ROOT_RULE, rootNode),
                ruleTree(List.of())
        );

        // When
        DefaultTreeFactory.StrategyAwardVO result = engine.process(USER_ID, STRATEGY_ID, AWARD_ID);

        // Then
        assertSame(expected, result);
    }

    /**
     * Given 节点结果不满足任何候选连线，When 执行规则树，Then 抛出规则值配置异常。
     */
    @Test
    void shouldRejectNodeResultWithoutMatchingLine() {
        // Given
        ILogicTreeNode rootNode = mock(ILogicTreeNode.class);
        when(rootNode.logic(USER_ID, STRATEGY_ID, AWARD_ID)).thenReturn(
                DefaultTreeFactory.TreeActionEntity.builder()
                        .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.ALLOW)
                        .build()
        );
        RuleTreeNodeLineVo unmatchedLine = RuleTreeNodeLineVo.builder()
                .treeId(100_000_001)
                .ruleNodeFrom(ROOT_RULE)
                .ruleNodeTo("rule_next")
                .ruleLimitTypeVO(RuleLimitTypeVO.EQ)
                .ruleLimitValue(RuleLogicCheckTypeVO.TAKE_OVER)
                .build();
        DecisionTreeEngine engine = new DecisionTreeEngine(
                Map.of(ROOT_RULE, rootNode),
                ruleTree(List.of(unmatchedLine))
        );

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> engine.process(USER_ID, STRATEGY_ID, AWARD_ID)
        );

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID.getCode(), exception.getCode());
    }

    /**
     * 创建只有根节点的规则树。
     *
     * @param lines 根节点的候选连线
     * @return 规则树配置
     */
    private RuleTreeVO ruleTree(List<RuleTreeNodeLineVo> lines) {
        RuleTreeNodeVO rootNode = RuleTreeNodeVO.builder()
                .treeId(100_000_001)
                .ruleKey(ROOT_RULE)
                .ruleDesc("测试根节点")
                .ruleTreeNodeLineVoList(lines)
                .build();
        return new RuleTreeVO(
                100_000_001,
                "测试规则树",
                "测试规则树",
                ROOT_RULE,
                Map.of(ROOT_RULE, rootNode)
        );
    }
}
