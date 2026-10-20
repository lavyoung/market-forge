package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link ProbabilityAlgorithmRegistry} 的注册约束、选择优先级和按类型恢复行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class ProbabilityAlgorithmRegistryTest {

    private static final StrategyProbabilitySpace PROBABILITY_SPACE = new StrategyProbabilitySpace(
            "100006", 10, List.of(new AwardProbabilityWeight(101L, 10)));

    /**
     * Given 注入顺序中二分算法位于直接查表算法之前，When 两种算法都支持概率空间，Then 仍按枚举优先级选择直接查表算法。
     */
    @Test
    void shouldSelectDirectLookupByDeclaredTypePriority() {
        // Given
        IProbabilityAlgorithm directLookup = algorithm(ProbabilityAlgorithmType.DIRECT_LOOKUP, true);
        IProbabilityAlgorithm binarySearch = algorithm(ProbabilityAlgorithmType.BINARY_SEARCH, true);
        ProbabilityAlgorithmRegistry registry = new ProbabilityAlgorithmRegistry(List.of(binarySearch, directLookup));

        // When
        IProbabilityAlgorithm selected = registry.select(PROBABILITY_SPACE);

        // Then
        assertSame(directLookup, selected);
        verify(directLookup).supports(PROBABILITY_SPACE);
        verify(binarySearch, never()).supports(PROBABILITY_SPACE);
    }

    /**
     * Given 直接查表不支持且 Alias 支持大型候选奖池，When 选择装配算法，Then 在二分算法之前选择 Alias。
     */
    @Test
    void shouldSelectAliasWhenDirectLookupIsUnsupported() {
        // Given
        IProbabilityAlgorithm directLookup = algorithm(ProbabilityAlgorithmType.DIRECT_LOOKUP, false);
        IProbabilityAlgorithm aliasMethod = algorithm(ProbabilityAlgorithmType.ALIAS_METHOD, true);
        IProbabilityAlgorithm binarySearch = algorithm(ProbabilityAlgorithmType.BINARY_SEARCH, true);
        ProbabilityAlgorithmRegistry registry = new ProbabilityAlgorithmRegistry(List.of(binarySearch, aliasMethod, directLookup));

        // When
        IProbabilityAlgorithm selected = registry.select(PROBABILITY_SPACE);

        // Then
        assertSame(aliasMethod, selected);
        verify(directLookup).supports(PROBABILITY_SPACE);
        verify(aliasMethod).supports(PROBABILITY_SPACE);
        verify(binarySearch, never()).supports(PROBABILITY_SPACE);
    }

    /**
     * Given 直接查表和 Alias 都不支持当前空间，When 选择装配算法，Then 使用二分算法通用兜底。
     */
    @Test
    void shouldFallbackToBinarySearchWhenEarlierAlgorithmsAreUnsupported() {
        // Given
        IProbabilityAlgorithm directLookup = algorithm(ProbabilityAlgorithmType.DIRECT_LOOKUP, false);
        IProbabilityAlgorithm aliasMethod = algorithm(ProbabilityAlgorithmType.ALIAS_METHOD, false);
        IProbabilityAlgorithm binarySearch = algorithm(ProbabilityAlgorithmType.BINARY_SEARCH, true);
        ProbabilityAlgorithmRegistry registry = new ProbabilityAlgorithmRegistry(List.of(aliasMethod, binarySearch, directLookup));

        // When
        IProbabilityAlgorithm selected = registry.select(PROBABILITY_SPACE);

        // Then
        assertSame(binarySearch, selected);
        verify(directLookup).supports(PROBABILITY_SPACE);
        verify(aliasMethod).supports(PROBABILITY_SPACE);
        verify(binarySearch).supports(PROBABILITY_SPACE);
    }

    /**
     * Given 已注册二分算法，When 根据装配元数据恢复实现，Then 返回同一个算法实例。
     */
    @Test
    void shouldGetRequiredAlgorithmByType() {
        IProbabilityAlgorithm binarySearch = algorithm(ProbabilityAlgorithmType.BINARY_SEARCH, true);
        ProbabilityAlgorithmRegistry registry = new ProbabilityAlgorithmRegistry(List.of(binarySearch));

        assertSame(binarySearch, registry.getRequired(ProbabilityAlgorithmType.BINARY_SEARCH));
    }

    /**
     * Given 同一类型存在两个算法实现，When 建立注册表，Then 拒绝存在歧义的启动配置。
     */
    @Test
    void shouldRejectDuplicateAlgorithmTypes() {
        IProbabilityAlgorithm first = algorithm(ProbabilityAlgorithmType.DIRECT_LOOKUP, true);
        IProbabilityAlgorithm second = algorithm(ProbabilityAlgorithmType.DIRECT_LOOKUP, true);

        assertThrows(IllegalStateException.class, () -> new ProbabilityAlgorithmRegistry(List.of(first, second)));
    }

    /**
     * Given 没有算法支持概率空间，When 选择装配算法，Then 返回概率总范围无效业务错误。
     */
    @Test
    void shouldRejectProbabilitySpaceWithoutSupportedAlgorithm() {
        IProbabilityAlgorithm directLookup = algorithm(ProbabilityAlgorithmType.DIRECT_LOOKUP, false);
        ProbabilityAlgorithmRegistry registry = new ProbabilityAlgorithmRegistry(List.of(directLookup));

        BusinessException exception = assertThrows(BusinessException.class, () -> registry.select(PROBABILITY_SPACE));

        assertSame(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID, exception.getResponseCode());
    }

    /**
     * Given 装配元数据指向未注册算法，When 恢复算法实现，Then 返回装配失败业务错误。
     */
    @Test
    void shouldRejectMissingRequiredAlgorithm() {
        ProbabilityAlgorithmRegistry registry = new ProbabilityAlgorithmRegistry(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> registry.getRequired(ProbabilityAlgorithmType.BINARY_SEARCH));

        assertSame(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, exception.getResponseCode());
    }

    /**
     * 创建指定类型和支持结果的算法测试替身。
     *
     * @param type      算法类型
     * @param supported 是否支持测试概率空间
     * @return 配置完成的算法替身
     */
    private IProbabilityAlgorithm algorithm(ProbabilityAlgorithmType type, boolean supported) {
        IProbabilityAlgorithm algorithm = mock(IProbabilityAlgorithm.class);
        when(algorithm.type()).thenReturn(type);
        when(algorithm.supports(PROBABILITY_SPACE)).thenReturn(supported);
        return algorithm;
    }
}
