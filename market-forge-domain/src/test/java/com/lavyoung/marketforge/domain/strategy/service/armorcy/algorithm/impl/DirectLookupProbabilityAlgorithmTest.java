package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link DirectLookupProbabilityAlgorithm} 的容量保护、查找表装配和随机派发行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DirectLookupProbabilityAlgorithmTest {

    private static final String ASSEMBLE_KEY = "100006";
    private static final int MAXIMUM_TABLE_SIZE = 10;

    @Mock
    private IStrategyRepository repository;

    private DirectLookupProbabilityAlgorithm algorithm;

    /**
     * Given 模拟策略仓储，When 初始化算法，Then 使用容量为 10 的独立算法实例。
     */
    @BeforeEach
    void setUp() {
        algorithm = new DirectLookupProbabilityAlgorithm(repository, MAXIMUM_TABLE_SIZE);
    }

    /**
     * Given 直接查表算法，When 查询算法类型和容量支持，Then 返回稳定类型并遵守容量边界。
     */
    @Test
    void shouldExposeTypeAndCapacityBoundary() {
        // Given
        StrategyProbabilitySpace supportedSpace = probabilitySpace(10);
        StrategyProbabilitySpace unsupportedSpace = probabilitySpace(11);

        // When & Then
        assertSame(ProbabilityAlgorithmType.DIRECT_LOOKUP, algorithm.type());
        assertTrue(algorithm.supports(supportedSpace));
        assertFalse(algorithm.supports(unsupportedSpace));
    }

    /**
     * Given 权重为 6、3、1 的概率空间，When 装配直接查找表，Then 保存 10 个完整且比例正确的奖品位置。
     */
    @Test
    void shouldAssembleCompleteLookupTable() {
        // Given
        StrategyProbabilitySpace probabilitySpace = new StrategyProbabilitySpace(ASSEMBLE_KEY, 10, List.of(
                new AwardProbabilityWeight(101L, 6),
                new AwardProbabilityWeight(102L, 3),
                new AwardProbabilityWeight(103L, 1)
        ));

        // When
        algorithm.assemble(probabilitySpace);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, Long>> tableCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).storeStrategyAwardSearchTables(eq(ASSEMBLE_KEY), eq(10), tableCaptor.capture());

        Map<Integer, Long> lookupTable = tableCaptor.getValue();
        assertEquals(10, lookupTable.size());
        assertEquals(6, lookupTable.values().stream().filter(awardId -> Long.valueOf(101L).equals(awardId)).count());
        assertEquals(3, lookupTable.values().stream().filter(awardId -> Long.valueOf(102L).equals(awardId)).count());
        assertEquals(1, lookupTable.values().stream().filter(awardId -> Long.valueOf(103L).equals(awardId)).count());
        assertTrue(lookupTable.keySet().containsAll(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)));
    }

    /**
     * Given 超过直接查表容量的概率空间，When 执行装配，Then 拒绝写入仓储以保护内存。
     */
    @Test
    void shouldRejectProbabilitySpaceOverCapacity() {
        // Given
        StrategyProbabilitySpace probabilitySpace = probabilitySpace(11);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> algorithm.assemble(probabilitySpace));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID, exception.getResponseCode());
        verify(repository, never()).storeStrategyAwardSearchTables(eq(ASSEMBLE_KEY), anyInt(), anyMap());
    }

    /**
     * Given 已装配的查找表，When 执行随机派发，Then 使用合法下标并返回仓储中的奖品标识。
     */
    @Test
    void shouldDispatchAwardFromValidRandomIndex() {
        // Given
        when(repository.getRateRange(ASSEMBLE_KEY)).thenReturn(10);
        when(repository.getStrategyAwardAssemble(eq(ASSEMBLE_KEY), anyInt())).thenReturn(102L);

        // When
        long awardId = algorithm.dispatch(ASSEMBLE_KEY);

        // Then
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(repository).getStrategyAwardAssemble(eq(ASSEMBLE_KEY), indexCaptor.capture());
        assertEquals(102L, awardId);
        assertTrue(indexCaptor.getValue() >= 0);
        assertTrue(indexCaptor.getValue() < 10);
    }

    /**
     * Given 空白装配键，When 执行随机派发，Then 按算法接口契约抛出参数异常。
     */
    @Test
    void shouldRejectBlankAssembleKey() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () -> algorithm.dispatch(" "));
        verify(repository, never()).getRateRange(ASSEMBLE_KEY);
    }

    /**
     * Given 策略没有有效概率范围，When 执行随机派发，Then 返回尚未装配业务错误。
     */
    @Test
    void shouldRejectDispatchWhenStrategyIsNotAssembled() {
        // Given
        when(repository.getRateRange(ASSEMBLE_KEY)).thenReturn(0);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> algorithm.dispatch(ASSEMBLE_KEY));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, exception.getResponseCode());
        verify(repository, never()).getStrategyAwardAssemble(eq(ASSEMBLE_KEY), anyInt());
    }

    /**
     * Given 概率范围存在但随机下标没有奖品，When 执行派发，Then 暴露装配数据损坏错误。
     */
    @Test
    void shouldRejectDispatchWhenLookupTableIsIncomplete() {
        // Given
        when(repository.getRateRange(ASSEMBLE_KEY)).thenReturn(10);
        when(repository.getStrategyAwardAssemble(eq(ASSEMBLE_KEY), anyInt())).thenReturn(0L);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> algorithm.dispatch(ASSEMBLE_KEY));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, exception.getResponseCode());
    }

    /**
     * 创建只有一个奖品的概率空间，用于验证容量边界。
     *
     * @param totalWeight 概率空间总权重
     * @return 指定权重的概率空间
     */
    private StrategyProbabilitySpace probabilitySpace(int totalWeight) {
        return new StrategyProbabilitySpace(ASSEMBLE_KEY, totalWeight, List.of(new AwardProbabilityWeight(101L, totalWeight)));
    }
}
