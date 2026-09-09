package com.lavyoung.marketforge.app.strategy;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.*;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.impl.DefaultRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl.BlackListLogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl.DefaultRuleChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl.WeightLogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * 使用真实责任链节点、规则过滤器和模拟外部端口验证抽奖策略的编排与调度行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/05
 */
@ExtendWith(MockitoExtension.class)
class RaffleStrategyRunnerTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long DEFAULT_AWARD_ID = 100_011L;
    private static final Long BLACKLIST_AWARD_ID = 100_012L;
    private static final Long LUCK_AWARD_ID = 100_013L;
    private static final String WEIGHT_KEY = "4000";

    @Mock
    private IStrategyRepository repository;

    @Mock
    private IRuleTreeRepository ruleTreeRepository;

    @Mock
    private IStrategyDispatch strategyDispatch;

    @Mock
    private DefaultTreeFactory defaultTreeFactory;

    private IRaffleStrategy raffleStrategy;

    /**
     * Given 模拟外部端口，When 初始化真实责任链，Then 使用固定用户分值执行每个场景。
     */
    @BeforeEach
    void setUp() {
        DefaultChainFactory chainFactory = new DefaultChainFactory(
                Map.of(
                        RuleModel.RULE_BLACKLIST, new BlackListLogicChain(repository),
                        RuleModel.WEIGHT, new WeightLogicChain(repository, strategyDispatch),
                        RuleModel.DEFAULT, new DefaultRuleChain(strategyDispatch)
                ),
                repository
        );
        raffleStrategy = new DefaultRaffleStrategy(
                repository,
                ruleTreeRepository,
                strategyDispatch,
                chainFactory,
                defaultTreeFactory
        );
    }

    /**
     * Given 策略没有前置规则，When 执行抽奖，Then 从默认概率表返回奖品。
     */
    @Test
    @DisplayName("无规则接管时执行默认抽奖")
    void shouldPerformDefaultRaffle() {
        // Given
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules(null));
        when(strategyDispatch.getRandomAwardId(STRATEGY_ID)).thenReturn(DEFAULT_AWARD_ID);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(DEFAULT_AWARD_ID, award.awardId());
        verify(strategyDispatch).getRandomAwardId(STRATEGY_ID);
        verify(strategyDispatch, never()).getRandomAwardIdAndWeight(STRATEGY_ID, WEIGHT_KEY);
    }

    /**
     * Given 当前用户命中黑名单，When 执行抽奖，Then 黑名单规则接管并返回指定奖品。
     */
    @Test
    @DisplayName("黑名单用户获得规则指定奖品")
    void shouldReturnConfiguredAwardForBlacklistedUser() {
        // Given
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules(RuleModel.RULE_BLACKLIST.getCode()));
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.RULE_BLACKLIST.getCode()))
                .thenReturn(BLACKLIST_AWARD_ID + ":user-002/" + USER_ID);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(BLACKLIST_AWARD_ID, award.awardId());
        verifyNoInteractions(strategyDispatch);
    }

    /**
     * Given 用户分值达到权重门槛，When 执行抽奖，Then 使用对应权重概率表返回奖品。
     */
    @Test
    @DisplayName("达到积分门槛时执行权重抽奖")
    void shouldPerformWeightedRaffleForEligibleUser() {
        // Given
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID))
                .thenReturn(strategyWithRules(RuleModel.WEIGHT.getCode()));
        when(repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.WEIGHT.getCode()))
                .thenReturn("4000:100011/100012;5000:100011/100012/100013");
        when(strategyDispatch.getRandomAwardIdAndWeight(STRATEGY_ID, WEIGHT_KEY))
                .thenReturn(DEFAULT_AWARD_ID);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(DEFAULT_AWARD_ID, award.awardId());
        verify(strategyDispatch).getRandomAwardIdAndWeight(STRATEGY_ID, WEIGHT_KEY);
        verify(strategyDispatch, never()).getRandomAwardId(STRATEGY_ID);
    }


    /**
     * Given 解锁节点放行且库存节点接管，When 执行完整规则树，Then 按配置依次访问三个节点并返回幸运奖结果。
     */
    @Test
    @DisplayName("规则树按节点结果完成流转并返回最终奖品")
    public void test_tree_rule() {
        // Given
        ILogicTreeNode lockNode = mock(ILogicTreeNode.class);
        ILogicTreeNode stockNode = mock(ILogicTreeNode.class);
        ILogicTreeNode luckAwardNode = mock(ILogicTreeNode.class);
        when(lockNode.logic(USER_ID, STRATEGY_ID, DEFAULT_AWARD_ID, null))
                .thenReturn(treeAction(RuleLogicCheckTypeVO.ALLOW, DEFAULT_AWARD_ID, RuleModel.LOCK, null));
        when(stockNode.logic(USER_ID, STRATEGY_ID, DEFAULT_AWARD_ID, null))
                .thenReturn(treeAction(RuleLogicCheckTypeVO.TAKE_OVER, DEFAULT_AWARD_ID, RuleModel.LOCK, null));
        when(luckAwardNode.logic(USER_ID, STRATEGY_ID, DEFAULT_AWARD_ID, null))
                .thenReturn(treeAction(RuleLogicCheckTypeVO.ALLOW, LUCK_AWARD_ID, RuleModel.LUCK_AWARD, "1/100"));
        DefaultTreeFactory treeFactory = new DefaultTreeFactory(Map.of(
                RuleModel.LOCK.getCode(), lockNode,
                "rule_stock", stockNode,
                RuleModel.LUCK_AWARD.getCode(), luckAwardNode
        ));

        // When
        IDecisionTreeEngine treeEngine = treeFactory.openLogicTree(ruleTree());
        DefaultTreeFactory.StrategyAwardVO result = treeEngine.process(USER_ID, STRATEGY_ID, DEFAULT_AWARD_ID);

        // Then
        assertAll(
                () -> assertEquals(LUCK_AWARD_ID, result.awardId()),
                () -> assertEquals(RuleModel.LUCK_AWARD, result.ruleModel()),
                () -> assertEquals("1/100", result.awardRuleValue())
        );
        InOrder executionOrder = inOrder(lockNode, stockNode, luckAwardNode);
        executionOrder.verify(lockNode).logic(USER_ID, STRATEGY_ID, DEFAULT_AWARD_ID, null);
        executionOrder.verify(stockNode).logic(USER_ID, STRATEGY_ID, DEFAULT_AWARD_ID, null);
        executionOrder.verify(luckAwardNode).logic(USER_ID, STRATEGY_ID, DEFAULT_AWARD_ID, null);
    }

    /**
     * 构建“解锁放行—库存接管—幸运奖兜底”的规则树。
     *
     * @return 用于验证节点流转顺序的规则树
     */
    private RuleTreeVO ruleTree() {
        RuleTreeNodeVO lockNode = treeNode(
                RuleModel.LOCK.getCode(),
                List.of(
                        treeLine(RuleModel.LOCK.getCode(), RuleModel.LUCK_AWARD.getCode(), RuleLogicCheckTypeVO.TAKE_OVER),
                        treeLine(RuleModel.LOCK.getCode(), "rule_stock", RuleLogicCheckTypeVO.ALLOW)
                )
        );
        RuleTreeNodeVO stockNode = treeNode(
                "rule_stock",
                List.of(treeLine("rule_stock", RuleModel.LUCK_AWARD.getCode(), RuleLogicCheckTypeVO.TAKE_OVER))
        );
        RuleTreeNodeVO luckAwardNode = treeNode(RuleModel.LUCK_AWARD.getCode(), List.of());
        return new RuleTreeVO(
                100_000_001,
                "抽奖规则树",
                "验证规则树节点流转",
                RuleModel.LOCK.getCode(),
                Map.of(
                        RuleModel.LOCK.getCode(), lockNode,
                        "rule_stock", stockNode,
                        RuleModel.LUCK_AWARD.getCode(), luckAwardNode
                )
        );
    }

    /**
     * 创建规则树节点。
     *
     * @param ruleKey 节点规则标识
     * @param lines   节点的候选连线
     * @return 规则树节点
     */
    private RuleTreeNodeVO treeNode(String ruleKey, List<RuleTreeNodeLineVo> lines) {
        return RuleTreeNodeVO.builder()
                .treeId(100_000_001)
                .ruleKey(ruleKey)
                .ruleDesc(ruleKey)
                .ruleTreeNodeLineVoList(lines)
                .build();
    }

    /**
     * 创建使用相等条件匹配节点结果的连线。
     *
     * @param from      来源节点规则标识
     * @param to        目标节点规则标识
     * @param checkType 触发连线的节点结果
     * @return 规则树节点连线
     */
    private RuleTreeNodeLineVo treeLine(String from, String to, RuleLogicCheckTypeVO checkType) {
        return RuleTreeNodeLineVo.builder()
                .treeId(100_000_001)
                .ruleNodeFrom(from)
                .ruleNodeTo(to)
                .ruleLimitTypeVO(RuleLimitTypeVO.EQ)
                .ruleLimitValue(checkType)
                .build();
    }

    /**
     * 创建规则树节点执行结果。
     *
     * @param checkType 节点判断结果
     * @param awardId   奖品标识
     * @param ruleModel 命中的规则模型
     * @param ruleValue 奖品规则配置
     * @return 节点执行结果
     */
    private DefaultTreeFactory.TreeActionEntity treeAction(RuleLogicCheckTypeVO checkType, Long awardId,
                                                           RuleModel ruleModel, String ruleValue) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckTypeVO(checkType)
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(awardId)
                        .ruleModel(ruleModel)
                        .awardRuleValue(ruleValue)
                        .build())
                .build();
    }


    /**
     * 创建有效抽奖因子。
     *
     * @return 包含固定用户和策略标识的抽奖因子
     */
    private RaffleFactorEntity validFactor() {
        return RaffleFactorEntity.builder()
                .userId(USER_ID)
                .strategyId(STRATEGY_ID)
                .build();
    }

    /**
     * 创建指定规则配置的策略实体。
     *
     * @param ruleModels 逗号分隔的规则模型编码
     * @return 策略实体
     */
    private StrategyEntity strategyWithRules(String ruleModels) {
        return StrategyEntity.builder()
                .strategyId(STRATEGY_ID)
                .ruleModels(ruleModels)
                .build();
    }


}
