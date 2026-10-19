package com.lavyoung.marketforge.domain.strategy.service.armorcy;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.IProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmRegistry;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilitySpaceFactory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.impl.StrategyArmoryDispatch;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link StrategyArmoryDispatch} 的概率算法装配编排、元数据保存和派发路由。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategyArmoryDispatchTest {

    private static final Long STRATEGY_ID = 100_001L;
    private static final String ASSEMBLE_KEY = "100001";
    private static final String RULE_WEIGHT_VALUE = "4000";
    private static final String WEIGHT_ASSEMBLE_KEY = "100001_4000";
    private static final long EXPECTED_AWARD_ID = 100_011L;

    @Mock
    private IStrategyRepository repository;

    @Mock
    private ProbabilitySpaceFactory probabilitySpaceFactory;

    @Mock
    private ProbabilityAlgorithmRegistry probabilityAlgorithmRegistry;

    @Mock
    private IProbabilityAlgorithm directLookupAlgorithm;

    @Mock
    private IProbabilityAlgorithm binarySearchAlgorithm;

    private StrategyArmoryDispatch strategyArmoryDispatch;

    /**
     * Given 可控仓储、概率工厂和算法注册表，When 初始化服务，Then 每个用例使用独立领域服务实例。
     */
    @BeforeEach
    void setUp() {
        strategyArmoryDispatch = new StrategyArmoryDispatch(repository, probabilitySpaceFactory, probabilityAlgorithmRegistry);
    }

    /**
     * Given 未配置权重规则的普通策略，When 执行装配，Then 初始化库存并在算法数据完成后保存算法类型。
     */
    @Test
    void shouldAssembleDefaultProbabilitySpaceAndStoreAlgorithmMetadataLast() {
        // Given
        List<StrategyAwardEntity> awards = strategyAwards();
        StrategyProbabilitySpace probabilitySpace = defaultProbabilitySpace();
        when(repository.queryStrategyAwardList(STRATEGY_ID)).thenReturn(awards);
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID)).thenReturn(StrategyEntity.builder().strategyId(STRATEGY_ID).build());
        when(probabilitySpaceFactory.create(ASSEMBLE_KEY, awards)).thenReturn(probabilitySpace);
        when(probabilityAlgorithmRegistry.select(probabilitySpace)).thenReturn(directLookupAlgorithm);
        when(directLookupAlgorithm.type()).thenReturn(ProbabilityAlgorithmType.DIRECT_LOOKUP);

        // When
        boolean assembled = strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID);

        // Then
        assertTrue(assembled);
        for (StrategyAwardEntity award : awards) {
            verify(repository).cacheStrategyAwardStock(award.strategyId(), award.awardId(), award.awardCountSurplus());
        }
        InOrder assemblyOrder = inOrder(directLookupAlgorithm, repository);
        assemblyOrder.verify(directLookupAlgorithm).assemble(probabilitySpace);
        assemblyOrder.verify(repository).storeStrategyProbabilityAlgorithm(ASSEMBLE_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP);
    }

    /**
     * Given 策略配置一个权重档位，When 执行装配，Then 只使用档位声明的奖品建立独立概率空间和算法元数据。
     */
    @Test
    void shouldAssembleIndependentWeightProbabilitySpace() {
        // Given
        List<StrategyAwardEntity> awards = strategyAwards();
        List<StrategyAwardEntity> eligibleAwards = List.of(awards.get(0), awards.get(1));
        StrategyProbabilitySpace defaultSpace = defaultProbabilitySpace();
        StrategyProbabilitySpace weightSpace = new StrategyProbabilitySpace(WEIGHT_ASSEMBLE_KEY, 3, List.of(
                new AwardProbabilityWeight(100_011L, 2),
                new AwardProbabilityWeight(100_012L, 1)
        ));
        StrategyEntity strategy = StrategyEntity.builder().strategyId(STRATEGY_ID).ruleModels(RuleModel.WEIGHT.getCode()).build();
        StrategyRuleEntity weightRule = StrategyRuleEntity.builder().strategyId(STRATEGY_ID).ruleModel(RuleModel.WEIGHT.getCode())
                .ruleValue("4000:100011/100012").build();
        when(repository.queryStrategyAwardList(STRATEGY_ID)).thenReturn(awards);
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID)).thenReturn(strategy);
        when(repository.getStrategyRule(STRATEGY_ID, RuleModel.WEIGHT)).thenReturn(weightRule);
        when(probabilitySpaceFactory.create(ASSEMBLE_KEY, awards)).thenReturn(defaultSpace);
        when(probabilitySpaceFactory.create(WEIGHT_ASSEMBLE_KEY, eligibleAwards)).thenReturn(weightSpace);
        when(probabilityAlgorithmRegistry.select(defaultSpace)).thenReturn(directLookupAlgorithm);
        when(probabilityAlgorithmRegistry.select(weightSpace)).thenReturn(binarySearchAlgorithm);
        when(directLookupAlgorithm.type()).thenReturn(ProbabilityAlgorithmType.DIRECT_LOOKUP);
        when(binarySearchAlgorithm.type()).thenReturn(ProbabilityAlgorithmType.BINARY_SEARCH);

        // When
        boolean assembled = strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID);

        // Then
        assertTrue(assembled);
        verify(binarySearchAlgorithm).assemble(weightSpace);
        verify(repository).storeStrategyProbabilityAlgorithm(WEIGHT_ASSEMBLE_KEY, ProbabilityAlgorithmType.BINARY_SEARCH);
        verify(probabilitySpaceFactory).create(WEIGHT_ASSEMBLE_KEY, eligibleAwards);
    }

    /**
     * Given 普通策略保存了二分算法元数据，When 获取随机奖品，Then 路由到对应算法并返回结果。
     */
    @Test
    void shouldDispatchDefaultStrategyByStoredAlgorithmType() {
        when(repository.getStrategyProbabilityAlgorithm(ASSEMBLE_KEY)).thenReturn(Optional.of(ProbabilityAlgorithmType.BINARY_SEARCH));
        when(probabilityAlgorithmRegistry.getRequired(ProbabilityAlgorithmType.BINARY_SEARCH)).thenReturn(binarySearchAlgorithm);
        when(binarySearchAlgorithm.dispatch(ASSEMBLE_KEY)).thenReturn(EXPECTED_AWARD_ID);

        long awardId = strategyArmoryDispatch.getRandomAwardId(STRATEGY_ID);

        assertEquals(EXPECTED_AWARD_ID, awardId);
        verify(binarySearchAlgorithm).dispatch(ASSEMBLE_KEY);
    }

    /**
     * Given 权重策略保存了直接查表算法元数据，When 按权重档位抽奖，Then 使用组合装配键路由算法。
     */
    @Test
    void shouldDispatchWeightStrategyByStoredAlgorithmType() {
        when(repository.getStrategyProbabilityAlgorithm(WEIGHT_ASSEMBLE_KEY)).thenReturn(Optional.of(ProbabilityAlgorithmType.DIRECT_LOOKUP));
        when(probabilityAlgorithmRegistry.getRequired(ProbabilityAlgorithmType.DIRECT_LOOKUP)).thenReturn(directLookupAlgorithm);
        when(directLookupAlgorithm.dispatch(WEIGHT_ASSEMBLE_KEY)).thenReturn(EXPECTED_AWARD_ID);

        long awardId = strategyArmoryDispatch.getRandomAwardIdAndWeight(STRATEGY_ID, RULE_WEIGHT_VALUE);

        assertEquals(EXPECTED_AWARD_ID, awardId);
        verify(directLookupAlgorithm).dispatch(WEIGHT_ASSEMBLE_KEY);
    }

    /**
     * Given 策略没有算法元数据，When 获取随机奖品，Then 返回策略尚未装配业务错误且不调用任何算法。
     */
    @Test
    void shouldRejectDispatchWhenAlgorithmMetadataIsMissing() {
        when(repository.getStrategyProbabilityAlgorithm(ASSEMBLE_KEY)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> strategyArmoryDispatch.getRandomAwardId(STRATEGY_ID));

        assertSame(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, exception.getResponseCode());
        verifyNoInteractions(directLookupAlgorithm, binarySearchAlgorithm);
    }

    /**
     * Given 权重档位为空白，When 按权重策略抽奖，Then 在访问仓储前拒绝请求。
     */
    @Test
    void shouldRejectBlankWeightValue() {
        assertThrows(IllegalArgumentException.class, () -> strategyArmoryDispatch.getRandomAwardIdAndWeight(STRATEGY_ID, " "));
        verifyNoInteractions(repository, probabilityAlgorithmRegistry);
    }

    /**
     * Given 奖品库存充足，When 委托领域服务扣减库存，Then 按一份库存和活动结束时间调用仓储。
     */
    @Test
    void shouldDelegateAwardStockSubtraction() {
        LocalDateTime endDateTime = LocalDateTime.now().plusDays(1);
        when(repository.subtractAwardStock(STRATEGY_ID, EXPECTED_AWARD_ID, 1, endDateTime)).thenReturn(true);

        boolean subtracted = strategyArmoryDispatch.subtractAwardStock(STRATEGY_ID, EXPECTED_AWARD_ID, endDateTime);

        assertTrue(subtracted);
        verify(repository).subtractAwardStock(STRATEGY_ID, EXPECTED_AWARD_ID, 1, endDateTime);
    }

    /**
     * 创建三个奖品的策略配置。
     *
     * @return 用于普通策略和权重策略装配测试的奖品列表
     */
    private List<StrategyAwardEntity> strategyAwards() {
        return List.of(
                strategyAward(100_011L, "0.6", 60),
                strategyAward(100_012L, "0.3", 30),
                strategyAward(100_013L, "0.1", 10)
        );
    }

    /**
     * 创建普通策略对应的整数概率空间。
     *
     * @return 权重为 6、3、1 的概率空间
     */
    private StrategyProbabilitySpace defaultProbabilitySpace() {
        return new StrategyProbabilitySpace(ASSEMBLE_KEY, 10, List.of(
                new AwardProbabilityWeight(100_011L, 6),
                new AwardProbabilityWeight(100_012L, 3),
                new AwardProbabilityWeight(100_013L, 1)
        ));
    }

    /**
     * 创建单个策略奖品。
     *
     * @param awardId      奖品标识
     * @param awardRate    奖品概率
     * @param stockSurplus 剩余库存
     * @return 策略奖品实体
     */
    private StrategyAwardEntity strategyAward(long awardId, String awardRate, int stockSurplus) {
        return StrategyAwardEntity.builder().strategyId(STRATEGY_ID).awardId(awardId).awardCount(100)
                .awardCountSurplus(stockSurplus).awardRate(new BigDecimal(awardRate)).build();
    }
}
