package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.AliasProbabilityTableFactory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link AliasMethodProbabilityAlgorithm} 的适用阈值、装配交互、常数时间抽样和异常边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AliasMethodProbabilityAlgorithmTest {

    private static final String ASSEMBLE_KEY = "100006";
    private static final int MINIMUM_AWARD_COUNT = 3;

    @Mock
    private IStrategyRepository repository;

    @Mock
    private AliasProbabilityTableFactory probabilityTableFactory;

    @Mock
    private RandomGenerator randomGenerator;

    private AliasMethodProbabilityAlgorithm algorithm;

    /**
     * 为每个测试创建具有确定候选数量阈值和可控随机源的算法实例。
     */
    @BeforeEach
    void setUp() {
        AliasMethodProbabilityAlgorithm.RuntimeOptions options =
                new AliasMethodProbabilityAlgorithm.RuntimeOptions(randomGenerator, MINIMUM_AWARD_COUNT);
        algorithm = new AliasMethodProbabilityAlgorithm(repository, probabilityTableFactory, options);
    }

    /**
     * Given 候选奖品数分别位于阈值两侧，When 判断适用性，Then 只接受达到阈值的概率空间。
     */
    @Test
    void shouldExposeAliasTypeAndApplyAwardCountThreshold() {
        // Given
        StrategyProbabilitySpace belowThreshold = probabilitySpace(2);
        StrategyProbabilitySpace atThreshold = probabilitySpace(3);

        // When & Then
        assertSame(ProbabilityAlgorithmType.ALIAS_METHOD, algorithm.type());
        assertFalse(algorithm.supports(belowThreshold));
        assertTrue(algorithm.supports(atThreshold));
        assertThrows(NullPointerException.class, () -> algorithm.supports(null));
    }

    /**
     * Given 达到候选数量阈值的概率空间，When 装配 Alias 算法，Then 保存工厂生成的完整概率快照。
     */
    @Test
    void shouldAssembleAndStoreAliasProbabilityTable() {
        // Given
        StrategyProbabilitySpace probabilitySpace = probabilitySpace(3);
        AliasProbabilityTable aliasTable = aliasProbabilityTable();
        when(probabilityTableFactory.create(probabilitySpace)).thenReturn(aliasTable);

        // When
        algorithm.assemble(probabilitySpace);

        // Then
        verify(probabilityTableFactory).create(probabilitySpace);
        verify(repository).storeStrategyAwardAliasTable(ASSEMBLE_KEY, aliasTable);
    }

    /**
     * Given 候选奖品数低于阈值，When 强制使用 Alias 算法装配，Then 拒绝写入任何概率数据。
     */
    @Test
    void shouldRejectAssemblyBelowAwardCountThreshold() {
        // Given
        StrategyProbabilitySpace probabilitySpace = probabilitySpace(2);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> algorithm.assemble(probabilitySpace));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID, exception.getResponseCode());
        verifyNoInteractions(probabilityTableFactory, repository);
    }

    /**
     * Given 随机接受值小于所选列接受概率，When 执行抽样，Then 返回所选列的原奖品。
     */
    @Test
    void shouldDispatchOriginalAwardWhenColumnIsAccepted() {
        // Given
        AliasProbabilityTable aliasTable = aliasProbabilityTable();
        when(repository.getStrategyAwardAliasTable(ASSEMBLE_KEY)).thenReturn(Optional.of(aliasTable));
        when(randomGenerator.nextInt(aliasTable.size())).thenReturn(0);
        when(randomGenerator.nextDouble()).thenReturn(0.2D);

        // When
        long awardId = algorithm.dispatch(ASSEMBLE_KEY);

        // Then
        assertEquals(101L, awardId);
    }

    /**
     * Given 随机接受值不小于所选列接受概率，When 执行抽样，Then 返回别名列对应的奖品。
     */
    @Test
    void shouldDispatchAliasAwardWhenColumnIsRejected() {
        // Given
        AliasProbabilityTable aliasTable = aliasProbabilityTable();
        when(repository.getStrategyAwardAliasTable(ASSEMBLE_KEY)).thenReturn(Optional.of(aliasTable));
        when(randomGenerator.nextInt(aliasTable.size())).thenReturn(0);
        when(randomGenerator.nextDouble()).thenReturn(0.5D);

        // When
        long awardId = algorithm.dispatch(ASSEMBLE_KEY);

        // Then
        assertEquals(102L, awardId);
    }

    /**
     * Given 空白装配键，When 派发奖品，Then 在访问仓储和随机源前拒绝请求。
     */
    @Test
    void shouldRejectBlankAssembleKey() {
        // Given & When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> algorithm.dispatch(" "));

        // Then
        assertEquals("assembleKey must not be blank", exception.getMessage());
        verifyNoInteractions(repository, randomGenerator);
    }

    /**
     * Given 仓储中不存在 Alias 概率表，When 派发奖品，Then 返回策略尚未装配业务错误。
     */
    @Test
    void shouldRejectDispatchWhenAliasTableIsMissing() {
        // Given
        when(repository.getStrategyAwardAliasTable(ASSEMBLE_KEY)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> algorithm.dispatch(ASSEMBLE_KEY));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, exception.getResponseCode());
        verify(randomGenerator, never()).nextInt(org.mockito.ArgumentMatchers.anyInt());
        verify(randomGenerator, never()).nextDouble();
    }

    /**
     * Given 非法候选数量阈值，When 创建运行参数，Then 立即拒绝错误配置。
     */
    @Test
    void shouldRejectNonPositiveMinimumAwardCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new AliasMethodProbabilityAlgorithm.RuntimeOptions(randomGenerator, 0));
    }

    private StrategyProbabilitySpace probabilitySpace(int awardCount) {
        List<AwardProbabilityWeight> awardWeights = java.util.stream.IntStream.range(0, awardCount)
                .mapToObj(index -> new AwardProbabilityWeight(index + 101L, 1))
                .toList();
        return new StrategyProbabilitySpace(ASSEMBLE_KEY, awardCount, awardWeights);
    }

    private AliasProbabilityTable aliasProbabilityTable() {
        return new AliasProbabilityTable(List.of(101L, 102L), new double[]{0.25D, 1D}, new int[]{1, 1});
    }
}
