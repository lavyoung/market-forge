package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link BinarySearchProbabilityAlgorithm} 的累计区间装配、二分边界和异常数据保护。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class BinarySearchProbabilityAlgorithmTest {

    private static final String ASSEMBLE_KEY = "100006";

    @Mock
    private IStrategyRepository repository;

    @Mock
    private RandomGenerator randomGenerator;

    private BinarySearchProbabilityAlgorithm algorithm;

    /**
     * Given 可控仓储和随机数生成器，When 初始化算法，Then 每个测试都使用独立算法实例。
     */
    @BeforeEach
    void setUp() {
        algorithm = new BinarySearchProbabilityAlgorithm(repository, randomGenerator);
    }

    /**
     * Given 任意合法概率空间，When 查询算法能力，Then 返回二分类型并作为通用兜底算法接受该空间。
     */
    @Test
    void shouldExposeBinarySearchTypeAndSupportValidSpace() {
        StrategyProbabilitySpace probabilitySpace = probabilitySpace();

        assertSame(ProbabilityAlgorithmType.BINARY_SEARCH, algorithm.type());
        assertTrue(algorithm.supports(probabilitySpace));
        assertThrows(NullPointerException.class, () -> algorithm.supports(null));
    }

    /**
     * Given 权重为 6、3、1 的概率空间，When 装配二分算法，Then 保存累计上界 6、9、10。
     */
    @Test
    void shouldAssembleCumulativeProbabilityRanges() {
        // Given
        StrategyProbabilitySpace probabilitySpace = probabilitySpace();

        // When
        algorithm.assemble(probabilitySpace);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AwardProbabilityRange>> rangesCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).storeStrategyAwardProbabilityRanges(eq(ASSEMBLE_KEY), eq(10), rangesCaptor.capture());
        assertEquals(List.of(
                new AwardProbabilityRange(6, 101L),
                new AwardProbabilityRange(9, 102L),
                new AwardProbabilityRange(10, 103L)
        ), rangesCaptor.getValue());
    }

    /**
     * Given 三段累计概率区间，When 随机值命中各区间首尾边界，Then 二分查找返回对应奖品。
     */
    @Test
    void shouldDispatchAwardsAtEveryIntervalBoundary() {
        // Given
        List<AwardProbabilityRange> ranges = List.of(
                new AwardProbabilityRange(6, 101L),
                new AwardProbabilityRange(9, 102L),
                new AwardProbabilityRange(10, 103L)
        );
        when(repository.getRateRange(ASSEMBLE_KEY)).thenReturn(10);
        when(repository.getStrategyAwardProbabilityRanges(ASSEMBLE_KEY)).thenReturn(ranges);
        when(randomGenerator.nextInt(10)).thenReturn(0, 5, 6, 8, 9);

        // When
        List<Long> results = List.of(
                algorithm.dispatch(ASSEMBLE_KEY),
                algorithm.dispatch(ASSEMBLE_KEY),
                algorithm.dispatch(ASSEMBLE_KEY),
                algorithm.dispatch(ASSEMBLE_KEY),
                algorithm.dispatch(ASSEMBLE_KEY)
        );

        // Then
        assertEquals(List.of(101L, 101L, 102L, 102L, 103L), results);
    }

    /**
     * Given 空白装配键，When 派发奖品，Then 在访问仓储前拒绝请求。
     */
    @Test
    void shouldRejectBlankAssembleKey() {
        assertThrows(IllegalArgumentException.class, () -> algorithm.dispatch(" "));
        verifyNoInteractions(repository, randomGenerator);
    }

    /**
     * Given Redis 中没有总权重，When 派发奖品，Then 返回策略尚未装配业务错误。
     */
    @Test
    void shouldRejectDispatchWhenStrategyIsNotAssembled() {
        when(repository.getRateRange(ASSEMBLE_KEY)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> algorithm.dispatch(ASSEMBLE_KEY));

        assertSame(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, exception.getResponseCode());
        verify(repository, never()).getStrategyAwardProbabilityRanges(ASSEMBLE_KEY);
        verifyNoInteractions(randomGenerator);
    }

    /**
     * Given 总权重存在但累计区间为空，When 派发奖品，Then 返回装配数据损坏业务错误。
     */
    @Test
    void shouldRejectEmptyProbabilityRanges() {
        when(repository.getRateRange(ASSEMBLE_KEY)).thenReturn(10);
        when(repository.getStrategyAwardProbabilityRanges(ASSEMBLE_KEY)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> algorithm.dispatch(ASSEMBLE_KEY));

        assertSame(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, exception.getResponseCode());
        verifyNoInteractions(randomGenerator);
    }

    /**
     * Given 最后累计上界与总权重不一致，When 派发奖品，Then 拒绝使用不完整概率空间。
     */
    @Test
    void shouldRejectIncompleteProbabilityCoverage() {
        when(repository.getRateRange(ASSEMBLE_KEY)).thenReturn(10);
        when(repository.getStrategyAwardProbabilityRanges(ASSEMBLE_KEY)).thenReturn(List.of(new AwardProbabilityRange(9, 101L)));

        BusinessException exception = assertThrows(BusinessException.class, () -> algorithm.dispatch(ASSEMBLE_KEY));

        assertSame(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, exception.getResponseCode());
        verifyNoInteractions(randomGenerator);
    }

    /**
     * 创建权重为 6、3、1 的合法概率空间。
     *
     * @return 用于装配和边界查找测试的概率空间
     */
    private StrategyProbabilitySpace probabilitySpace() {
        return new StrategyProbabilitySpace(ASSEMBLE_KEY, 10, List.of(
                new AwardProbabilityWeight(101L, 6),
                new AwardProbabilityWeight(102L, 3),
                new AwardProbabilityWeight(103L, 1)
        ));
    }
}
