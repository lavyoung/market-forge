package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link DefaultRaffleStrategy} 对责任链抽奖与规则树决策的编排行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class DefaultRaffleStrategyTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;
    private static final Long TREE_AWARD_ID = 100_012L;

    @Mock
    private IStrategyRepository repository;

    @Mock
    private IRuleTreeRepository ruleTreeRepository;

    @Mock
    private IStrategyDispatch strategyDispatch;

    @Mock
    private DefaultChainFactory chainFactory;

    @Mock
    private DefaultTreeFactory treeFactory;

    @Mock
    private ILogicChain logicChain;

    @Mock
    private IDecisionTreeEngine treeEngine;

    private DefaultRaffleStrategy raffleStrategy;

    /**
     * Given 模拟领域依赖，When 初始化默认抽奖策略，Then 使用可控责任链和规则树执行测试。
     */
    @BeforeEach
    void setUp() {
        raffleStrategy = new DefaultRaffleStrategy(
                repository,
                ruleTreeRepository,
                strategyDispatch,
                chainFactory,
                treeFactory
        );
    }

    /**
     * Given 抽奖参数缺少策略标识，When 执行抽奖，Then 抛出业务异常且不访问领域依赖。
     */
    @Test
    void shouldRejectRaffleFactorWithoutStrategyId() {
        // Given
        RaffleFactorEntity factor = RaffleFactorEntity.builder().userId(USER_ID).build();

        // When & Then
        assertThrows(BusinessException.class, () -> raffleStrategy.performRaffle(factor));
        verifyNoInteractions(repository, ruleTreeRepository, strategyDispatch, chainFactory, treeFactory);
    }

    /**
     * Given 前置规则接管责任链，When 执行抽奖，Then 直接返回规则奖品且不执行规则树。
     */
    @Test
    void shouldReturnChainAwardWhenNonDefaultRuleTakesOver() {
        // Given
        stubChainAward(RuleModel.RULE_BLACKLIST);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(AWARD_ID, award.awardId());
        verifyNoInteractions(ruleTreeRepository, treeFactory);
    }

    /**
     * Given 默认责任链奖品未配置执行阶段规则，When 执行抽奖，Then 保留原始奖品。
     */
    @Test
    void shouldKeepDefaultAwardWhenNoTreeRuleConfigured() {
        // Given
        stubChainAward(RuleModel.DEFAULT);
        when(repository.queryStrategyAwardRuleModels(STRATEGY_ID, AWARD_ID)).thenReturn(null);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(AWARD_ID, award.awardId());
        verifyNoInteractions(ruleTreeRepository, treeFactory);
    }

    /**
     * Given 默认奖品配置了可执行规则树，When 执行抽奖，Then 返回规则树改写后的奖品和配置。
     */
    @Test
    void shouldReturnTreeDecisionForDefaultAward() {
        // Given
        stubChainAward(RuleModel.DEFAULT);
        StrategyAwardRuleModelVO ruleModels = new StrategyAwardRuleModelVO(RuleModel.LOCK.getCode());
        RuleTreeVO ruleTree = ruleTree();
        DefaultTreeFactory.StrategyAwardVO treeAward = DefaultTreeFactory.StrategyAwardVO.builder()
                .awardId(TREE_AWARD_ID)
                .ruleModel(RuleModel.LUCK_AWARD)
                .awardRuleValue("1/100")
                .build();
        when(repository.queryStrategyAwardRuleModels(STRATEGY_ID, AWARD_ID)).thenReturn(ruleModels);
        when(ruleTreeRepository.queryRuleTreeVOByTreeId(List.of(RuleModel.LOCK))).thenReturn(Optional.of(ruleTree));
        when(treeFactory.openLogicTree(ruleTree)).thenReturn(treeEngine);
        when(treeEngine.process(USER_ID, STRATEGY_ID, AWARD_ID)).thenReturn(treeAward);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertAll(
                () -> assertEquals(TREE_AWARD_ID, award.awardId()),
                () -> assertEquals("1/100", award.awardConfig())
        );
        verify(treeEngine).process(USER_ID, STRATEGY_ID, AWARD_ID);
    }

    /**
     * Given 奖品规则模型没有对应规则树，When 执行抽奖，Then 抛出策略未装配异常。
     */
    @Test
    void shouldRejectMissingRuleTreeConfiguration() {
        // Given
        stubChainAward(RuleModel.DEFAULT);
        StrategyAwardRuleModelVO ruleModels = new StrategyAwardRuleModelVO(RuleModel.LOCK.getCode());
        when(repository.queryStrategyAwardRuleModels(STRATEGY_ID, AWARD_ID)).thenReturn(ruleModels);
        when(ruleTreeRepository.queryRuleTreeVOByTreeId(List.of(RuleModel.LOCK))).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> raffleStrategy.performRaffle(validFactor())
        );

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED.getCode(), exception.getCode());
        verifyNoInteractions(treeFactory);
    }

    /**
     * 配置责任链返回固定奖品及指定规则模型。
     *
     * @param ruleModel 责任链命中的规则模型
     */
    private void stubChainAward(RuleModel ruleModel) {
        when(chainFactory.openLogicChain(STRATEGY_ID)).thenReturn(logicChain);
        when(logicChain.logic(USER_ID, STRATEGY_ID)).thenReturn(
                DefaultChainFactory.StrategyAwardVO.builder()
                        .awardId(AWARD_ID)
                        .ruleModel(ruleModel)
                        .build()
        );
    }

    /**
     * 创建有效抽奖因子。
     *
     * @return 包含固定用户和策略标识的抽奖因子
     */
    private RaffleFactorEntity validFactor() {
        return RaffleFactorEntity.builder().userId(USER_ID).strategyId(STRATEGY_ID).build();
    }

    /**
     * 创建用于规则树编排测试的最小配置。
     *
     * @return 仅包含根节点信息的规则树
     */
    private RuleTreeVO ruleTree() {
        return new RuleTreeVO(
                100_000_001,
                "测试规则树",
                "测试规则树",
                RuleModel.LOCK.getCode(),
                Map.of()
        );
    }
}
