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
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotPointer;
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
    private static final long SNAPSHOT_VERSION = 7L;

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
     * Given 未配置权重规则的普通策略，When 执行装配，Then 初始化库存并在算法数据完成后发布快照指针。
     */
    @Test
    void shouldReserveGenerationBeforeReadingConfigurationAndPublishPointerLast() {
        // Given
        List<StrategyAwardEntity> awards = strategyAwards();
        StrategyProbabilitySpace probabilitySpace = defaultProbabilitySpace();
        when(repository.queryStrategyAwardList(STRATEGY_ID)).thenReturn(awards);
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID)).thenReturn(StrategyEntity.builder().strategyId(STRATEGY_ID).build());
        when(probabilitySpaceFactory.create(ASSEMBLE_KEY, awards)).thenReturn(probabilitySpace);
        when(probabilityAlgorithmRegistry.select(probabilitySpace)).thenReturn(directLookupAlgorithm);
        when(directLookupAlgorithm.type()).thenReturn(ProbabilityAlgorithmType.DIRECT_LOOKUP);
        when(repository.nextProbabilitySnapshotVersion(ASSEMBLE_KEY)).thenReturn(SNAPSHOT_VERSION);
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(ASSEMBLE_KEY, SNAPSHOT_VERSION);
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                ASSEMBLE_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, SNAPSHOT_VERSION);
        when(repository.publishProbabilitySnapshotPointer(pointer)).thenReturn(true);

        // When
        boolean assembled = strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID);

        // Then
        assertTrue(assembled);
        for (StrategyAwardEntity award : awards) {
            verify(repository).cacheStrategyAwardStock(award.strategyId(), award.awardId(), award.awardCountSurplus());
        }
        InOrder generationOrder = inOrder(repository);
        generationOrder.verify(repository).nextProbabilitySnapshotVersion(ASSEMBLE_KEY);
        generationOrder.verify(repository).queryStrategyAwardList(STRATEGY_ID);
        InOrder assemblyOrder = inOrder(directLookupAlgorithm, repository);
        assemblyOrder.verify(directLookupAlgorithm).assemble(probabilitySpace, snapshotKey);
        assemblyOrder.verify(repository).publishProbabilitySnapshotPointer(pointer);
        verify(repository, never()).storeStrategyProbabilityAlgorithm(anyString(), any());
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
        when(repository.nextProbabilitySnapshotVersion(ASSEMBLE_KEY)).thenReturn(SNAPSHOT_VERSION);
        ProbabilitySnapshotKey defaultSnapshotKey = new ProbabilitySnapshotKey(ASSEMBLE_KEY, 7L);
        ProbabilitySnapshotKey weightSnapshotKey = new ProbabilitySnapshotKey(WEIGHT_ASSEMBLE_KEY, 7L);
        ProbabilitySnapshotPointer defaultPointer = new ProbabilitySnapshotPointer(
                ASSEMBLE_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, 7L);
        ProbabilitySnapshotPointer weightPointer = new ProbabilitySnapshotPointer(
                WEIGHT_ASSEMBLE_KEY, ProbabilityAlgorithmType.BINARY_SEARCH, 7L);
        when(repository.publishProbabilitySnapshotPointer(defaultPointer)).thenReturn(true);
        when(repository.publishProbabilitySnapshotPointer(weightPointer)).thenReturn(true);

        // When
        boolean assembled = strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID);

        // Then
        assertTrue(assembled);
        verify(directLookupAlgorithm).assemble(defaultSpace, defaultSnapshotKey);
        verify(binarySearchAlgorithm).assemble(weightSpace, weightSnapshotKey);
        verify(repository).publishProbabilitySnapshotPointer(defaultPointer);
        verify(repository).publishProbabilitySnapshotPointer(weightPointer);
        verify(probabilitySpaceFactory).create(WEIGHT_ASSEMBLE_KEY, eligibleAwards);
        verify(repository, times(1)).nextProbabilitySnapshotVersion(ASSEMBLE_KEY);
        verify(repository, never()).nextProbabilitySnapshotVersion(WEIGHT_ASSEMBLE_KEY);
    }

    /**
     * Given Redis 无法分配新的策略代次，When 执行策略装配，Then 在读取数据库配置和改写库存前立即失败。
     */
    @Test
    void shouldStopBeforeReadingConfigurationWhenGenerationAllocationFails() {
        // Given
        RuntimeException allocationFailure = new RuntimeException("redis unavailable");
        when(repository.nextProbabilitySnapshotVersion(ASSEMBLE_KEY)).thenThrow(allocationFailure);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID));

        // Then
        assertSame(allocationFailure, exception);
        verify(repository, never()).queryStrategyAwardList(anyLong());
        verify(repository, never()).cacheStrategyAwardStock(anyLong(), anyLong(), anyInt());
        verifyNoInteractions(probabilitySpaceFactory, probabilityAlgorithmRegistry);
    }

    /**
     * Given 普通策略存在二分算法快照指针，When 获取随机奖品，Then 使用指针中的固定版本派发。
     */
    @Test
    void shouldDispatchDefaultStrategyByActiveSnapshotPointer() {
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                ASSEMBLE_KEY, ProbabilityAlgorithmType.BINARY_SEARCH, SNAPSHOT_VERSION);
        when(repository.getProbabilitySnapshotPointer(ASSEMBLE_KEY)).thenReturn(Optional.of(pointer));
        when(probabilityAlgorithmRegistry.getRequired(ProbabilityAlgorithmType.BINARY_SEARCH)).thenReturn(binarySearchAlgorithm);
        when(binarySearchAlgorithm.dispatch(pointer.snapshotKey())).thenReturn(EXPECTED_AWARD_ID);

        long awardId = strategyArmoryDispatch.getRandomAwardId(STRATEGY_ID);

        assertEquals(EXPECTED_AWARD_ID, awardId);
        verify(binarySearchAlgorithm).dispatch(pointer.snapshotKey());
    }

    /**
     * Given 权重策略存在直接查表快照指针，When 按权重档位抽奖，Then 使用组合装配键和固定版本派发。
     */
    @Test
    void shouldDispatchWeightStrategyByActiveSnapshotPointer() {
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                WEIGHT_ASSEMBLE_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, SNAPSHOT_VERSION);
        when(repository.getProbabilitySnapshotPointer(WEIGHT_ASSEMBLE_KEY)).thenReturn(Optional.of(pointer));
        when(probabilityAlgorithmRegistry.getRequired(ProbabilityAlgorithmType.DIRECT_LOOKUP)).thenReturn(directLookupAlgorithm);
        when(directLookupAlgorithm.dispatch(pointer.snapshotKey())).thenReturn(EXPECTED_AWARD_ID);

        long awardId = strategyArmoryDispatch.getRandomAwardIdAndWeight(STRATEGY_ID, RULE_WEIGHT_VALUE);

        assertEquals(EXPECTED_AWARD_ID, awardId);
        verify(directLookupAlgorithm).dispatch(pointer.snapshotKey());
    }

    /**
     * Given 策略没有生效快照指针，When 获取随机奖品，Then 返回策略尚未装配业务错误且不调用任何算法。
     */
    @Test
    void shouldRejectDispatchWhenSnapshotPointerIsMissing() {
        when(repository.getProbabilitySnapshotPointer(ASSEMBLE_KEY)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> strategyArmoryDispatch.getRandomAwardId(STRATEGY_ID));

        assertSame(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, exception.getResponseCode());
        verifyNoInteractions(directLookupAlgorithm, binarySearchAlgorithm);
    }

    /**
     * Given 算法写入新快照时失败，When 执行策略装配，Then 退役候选快照且不发布不完整版本。
     */
    @Test
    void shouldNotPublishPointerWhenSnapshotAssemblyFails() {
        // Given
        List<StrategyAwardEntity> awards = strategyAwards();
        StrategyProbabilitySpace probabilitySpace = defaultProbabilitySpace();
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(ASSEMBLE_KEY, SNAPSHOT_VERSION);
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                ASSEMBLE_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, SNAPSHOT_VERSION);
        RuntimeException failure = new RuntimeException("snapshot write failed");
        when(repository.queryStrategyAwardList(STRATEGY_ID)).thenReturn(awards);
        when(probabilitySpaceFactory.create(ASSEMBLE_KEY, awards)).thenReturn(probabilitySpace);
        when(probabilityAlgorithmRegistry.select(probabilitySpace)).thenReturn(directLookupAlgorithm);
        when(directLookupAlgorithm.type()).thenReturn(ProbabilityAlgorithmType.DIRECT_LOOKUP);
        when(repository.nextProbabilitySnapshotVersion(ASSEMBLE_KEY)).thenReturn(SNAPSHOT_VERSION);
        doThrow(failure).when(directLookupAlgorithm).assemble(probabilitySpace, snapshotKey);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID));

        // Then
        assertSame(failure, exception);
        verify(repository).retireProbabilitySnapshot(pointer);
        verify(repository, never()).publishProbabilitySnapshotPointer(any());
    }

    /**
     * Given 算法装配和候选快照退役都失败，When 执行策略装配，Then 保留并抛出最初的装配异常。
     */
    @Test
    void shouldPreserveAssemblyFailureWhenCandidateRetirementFails() {
        // Given
        List<StrategyAwardEntity> awards = strategyAwards();
        StrategyProbabilitySpace probabilitySpace = defaultProbabilitySpace();
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(ASSEMBLE_KEY, SNAPSHOT_VERSION);
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                ASSEMBLE_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, SNAPSHOT_VERSION);
        RuntimeException assemblyFailure = new RuntimeException("snapshot write failed");
        RuntimeException retirementFailure = new RuntimeException("redis unavailable");
        when(repository.queryStrategyAwardList(STRATEGY_ID)).thenReturn(awards);
        when(probabilitySpaceFactory.create(ASSEMBLE_KEY, awards)).thenReturn(probabilitySpace);
        when(probabilityAlgorithmRegistry.select(probabilitySpace)).thenReturn(directLookupAlgorithm);
        when(directLookupAlgorithm.type()).thenReturn(ProbabilityAlgorithmType.DIRECT_LOOKUP);
        when(repository.nextProbabilitySnapshotVersion(ASSEMBLE_KEY)).thenReturn(SNAPSHOT_VERSION);
        doThrow(assemblyFailure).when(directLookupAlgorithm).assemble(probabilitySpace, snapshotKey);
        doThrow(retirementFailure).when(repository).retireProbabilitySnapshot(pointer);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID));

        // Then
        assertSame(assemblyFailure, exception);
        verify(repository).retireProbabilitySnapshot(pointer);
        verify(repository, never()).publishProbabilitySnapshotPointer(any());
    }

    /**
     * Given 更高版本已经生效，When 当前装配任务发布旧版本被拒绝，Then 不覆盖指针且装配流程正常结束。
     */
    @Test
    void shouldIgnoreStaleSnapshotPublication() {
        // Given
        List<StrategyAwardEntity> awards = strategyAwards();
        StrategyProbabilitySpace probabilitySpace = defaultProbabilitySpace();
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(ASSEMBLE_KEY, SNAPSHOT_VERSION);
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                ASSEMBLE_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, SNAPSHOT_VERSION);
        when(repository.queryStrategyAwardList(STRATEGY_ID)).thenReturn(awards);
        when(repository.queryStrategyEntityByStrategyId(STRATEGY_ID)).thenReturn(
                StrategyEntity.builder().strategyId(STRATEGY_ID).build());
        when(probabilitySpaceFactory.create(ASSEMBLE_KEY, awards)).thenReturn(probabilitySpace);
        when(probabilityAlgorithmRegistry.select(probabilitySpace)).thenReturn(directLookupAlgorithm);
        when(directLookupAlgorithm.type()).thenReturn(ProbabilityAlgorithmType.DIRECT_LOOKUP);
        when(repository.nextProbabilitySnapshotVersion(ASSEMBLE_KEY)).thenReturn(SNAPSHOT_VERSION);
        when(repository.publishProbabilitySnapshotPointer(pointer)).thenReturn(false);

        // When
        boolean assembled = strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID);

        // Then
        assertTrue(assembled);
        verify(directLookupAlgorithm).assemble(probabilitySpace, snapshotKey);
        verify(repository).publishProbabilitySnapshotPointer(pointer);
        verify(repository).retireProbabilitySnapshot(pointer);
    }

    /**
     * Given 快照数据写入成功但发布操作抛出异常，When 执行策略装配，Then 保留候选数据并传播发布异常。
     */
    @Test
    void shouldNotRetireCandidateWhenSnapshotPublicationResultIsUnknown() {
        // Given
        List<StrategyAwardEntity> awards = strategyAwards();
        StrategyProbabilitySpace probabilitySpace = defaultProbabilitySpace();
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(ASSEMBLE_KEY, SNAPSHOT_VERSION);
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                ASSEMBLE_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, SNAPSHOT_VERSION);
        RuntimeException publicationFailure = new RuntimeException("publication result unknown");
        when(repository.queryStrategyAwardList(STRATEGY_ID)).thenReturn(awards);
        when(probabilitySpaceFactory.create(ASSEMBLE_KEY, awards)).thenReturn(probabilitySpace);
        when(probabilityAlgorithmRegistry.select(probabilitySpace)).thenReturn(directLookupAlgorithm);
        when(directLookupAlgorithm.type()).thenReturn(ProbabilityAlgorithmType.DIRECT_LOOKUP);
        when(repository.nextProbabilitySnapshotVersion(ASSEMBLE_KEY)).thenReturn(SNAPSHOT_VERSION);
        when(repository.publishProbabilitySnapshotPointer(pointer)).thenThrow(publicationFailure);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID));

        // Then
        assertSame(publicationFailure, exception);
        verify(directLookupAlgorithm).assemble(probabilitySpace, snapshotKey);
        verify(repository, never()).retireProbabilitySnapshot(any());
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
