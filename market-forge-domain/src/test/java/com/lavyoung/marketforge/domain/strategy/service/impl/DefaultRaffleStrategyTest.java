package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleMatterEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.filter.ILogicFilter;
import com.lavyoung.marketforge.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link DefaultRaffleStrategy} 对责任链抽奖与执行中规则的编排行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class DefaultRaffleStrategyTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;

    @Mock
    private IStrategyRepository repository;

    @Mock
    private DefaultChainFactory chainFactory;

    @Mock
    private DefaultLogicFactory logicFactory;

    @Mock
    private ILogicChain logicChain;

    @Mock
    private ILogicFilter<RuleActionEntity.RaffleExecutingEntity> executingFilter;

    private DefaultRaffleStrategy raffleStrategy;

    /**
     * Given 模拟领域依赖，When 初始化默认抽奖策略，Then 使用可控责任链和过滤器执行测试。
     */
    @BeforeEach
    void setUp() {
        raffleStrategy = new DefaultRaffleStrategy(repository, chainFactory, logicFactory);
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
        verifyNoInteractions(repository, chainFactory, logicFactory);
    }

    /**
     * Given 奖品未配置执行中规则，When 执行抽奖，Then 返回责任链选中的奖品。
     */
    @Test
    void shouldReturnChainAwardWhenNoExecutingRuleConfigured() {
        // Given
        stubChainAward();
        when(repository.queryStrategyAwardRuleModels(STRATEGY_ID, AWARD_ID)).thenReturn(null);

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(AWARD_ID, award.awardId());
        verify(logicChain).logic(USER_ID, STRATEGY_ID);
        verifyNoInteractions(logicFactory);
    }

    /**
     * Given 奖品配置的执行中规则全部放行，When 执行抽奖，Then 保留原始奖品结果。
     */
    @Test
    void shouldKeepAwardWhenExecutingRulesAllow() {
        // Given
        stubChainAward();
        when(repository.queryStrategyAwardRuleModels(STRATEGY_ID, AWARD_ID))
                .thenReturn(new StrategyAwardRuleModelVO(RuleModel.LOCK.getCode()));
        when(logicFactory.<RuleActionEntity.RaffleExecutingEntity>openLogicFilter())
                .thenReturn(Map.of(RuleModel.LOCK, executingFilter));
        when(executingFilter.filter(any(RuleMatterEntity.class))).thenReturn(allowAction());

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertEquals(AWARD_ID, award.awardId());
        ArgumentCaptor<RuleMatterEntity> captor = ArgumentCaptor.forClass(RuleMatterEntity.class);
        verify(executingFilter).filter(captor.capture());
        assertEquals(USER_ID, captor.getValue().userId());
        assertEquals(STRATEGY_ID, captor.getValue().strategyId());
        assertEquals(AWARD_ID, captor.getValue().awardId());
        assertEquals(RuleModel.LOCK.getCode(), captor.getValue().ruleModel());
    }

    /**
     * Given 执行中规则接管抽奖结果，When 执行抽奖，Then 返回空奖品标识作为兜底信号。
     */
    @Test
    void shouldReturnFallbackSignalWhenExecutingRuleTakesOver() {
        // Given
        stubChainAward();
        when(repository.queryStrategyAwardRuleModels(STRATEGY_ID, AWARD_ID))
                .thenReturn(new StrategyAwardRuleModelVO(RuleModel.LUCK_AWARD.getCode()));
        when(logicFactory.<RuleActionEntity.RaffleExecutingEntity>openLogicFilter())
                .thenReturn(Map.of(RuleModel.LUCK_AWARD, executingFilter));
        when(executingFilter.filter(any(RuleMatterEntity.class))).thenReturn(takeOverAction());

        // When
        RaffleAwardEntity award = raffleStrategy.performRaffle(validFactor());

        // Then
        assertNull(award.awardId());
    }

    /**
     * Given 责任链可返回固定奖品，When 配置模拟行为，Then 后续场景共享相同前置条件。
     */
    private void stubChainAward() {
        when(chainFactory.openLogicChain(STRATEGY_ID)).thenReturn(logicChain);
        when(logicChain.logic(USER_ID, STRATEGY_ID)).thenReturn(AWARD_ID);
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
     * 创建执行中规则放行动作。
     *
     * @return 放行动作
     */
    private RuleActionEntity<RuleActionEntity.RaffleExecutingEntity> allowAction() {
        return RuleActionEntity.<RuleActionEntity.RaffleExecutingEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .msg(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }

    /**
     * 创建执行中规则接管动作。
     *
     * @return 接管动作
     */
    private RuleActionEntity<RuleActionEntity.RaffleExecutingEntity> takeOverAction() {
        return RuleActionEntity.<RuleActionEntity.RaffleExecutingEntity>builder()
                .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                .msg(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                .ruleModel(RuleModel.LUCK_AWARD.getCode())
                .data(RuleActionEntity.RaffleExecutingEntity.builder().build())
                .build();
    }
}
