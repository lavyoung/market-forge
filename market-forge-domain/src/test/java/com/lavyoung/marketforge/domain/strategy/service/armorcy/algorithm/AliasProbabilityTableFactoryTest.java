package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link AliasProbabilityTableFactory} 的权重配平和概率守恒。
 *
 * <p>测试从装配结果反向计算每个奖品的最终命中概率，避免断言依赖某一种
 * 等价的别名表排列方式。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AliasProbabilityTableFactoryTest {

    private static final double PROBABILITY_TOLERANCE = 1E-12D;

    private AliasProbabilityTableFactory factory;

    /**
     * 为每个测试创建无共享状态的装配器。
     */
    @BeforeEach
    void setUp() {
        factory = new AliasProbabilityTableFactory();
    }

    /**
     * 验证不等权奖品经过配平后仍保持原始概率分布。
     */
    @Test
    void shouldPreserveWeightedProbabilityDistribution() {
        // Given
        StrategyProbabilitySpace probabilitySpace = probabilitySpace(10,
                new AwardProbabilityWeight(101L, 1),
                new AwardProbabilityWeight(102L, 2),
                new AwardProbabilityWeight(103L, 3),
                new AwardProbabilityWeight(104L, 4));

        // When
        AliasProbabilityTable table = factory.create(probabilitySpace);
        Map<Long, Double> actualProbabilities = restoreProbabilities(table);

        // Then
        assertAll(
                () -> assertEquals(0.1D, actualProbabilities.get(101L), PROBABILITY_TOLERANCE),
                () -> assertEquals(0.2D, actualProbabilities.get(102L), PROBABILITY_TOLERANCE),
                () -> assertEquals(0.3D, actualProbabilities.get(103L), PROBABILITY_TOLERANCE),
                () -> assertEquals(0.4D, actualProbabilities.get(104L), PROBABILITY_TOLERANCE));
    }

    /**
     * 验证单奖品概率空间生成一个始终接受且指向自身的列。
     */
    @Test
    void shouldCreateSelfContainedColumnForSingleAward() {
        // Given
        StrategyProbabilitySpace probabilitySpace = probabilitySpace(100, new AwardProbabilityWeight(101L, 100));

        // When
        AliasProbabilityTable table = factory.create(probabilitySpace);

        // Then
        assertAll(
                () -> assertEquals(1, table.size()),
                () -> assertEquals(101L, table.awardIdAt(0)),
                () -> assertEquals(1D, table.acceptanceProbabilityAt(0)),
                () -> assertEquals(0, table.aliasIndexAt(0)));
    }

    /**
     * 验证等权奖品不需要借用其他列的概率空间。
     */
    @Test
    void shouldCreateFullColumnsForEqualWeights() {
        // Given
        StrategyProbabilitySpace probabilitySpace = probabilitySpace(3,
                new AwardProbabilityWeight(101L, 1),
                new AwardProbabilityWeight(102L, 1),
                new AwardProbabilityWeight(103L, 1));

        // When
        AliasProbabilityTable table = factory.create(probabilitySpace);

        // Then
        for (int index = 0; index < table.size(); index++) {
            assertEquals(1D, table.acceptanceProbabilityAt(index));
            assertEquals(index, table.aliasIndexAt(index));
        }
    }

    /**
     * 验证权重高度倾斜时，小概率奖品不会在配平过程中丢失。
     */
    @Test
    void shouldPreserveExtremelySkewedWeights() {
        // Given
        StrategyProbabilitySpace probabilitySpace = probabilitySpace(1_000_000,
                new AwardProbabilityWeight(101L, 1),
                new AwardProbabilityWeight(102L, 999_999));

        // When
        AliasProbabilityTable table = factory.create(probabilitySpace);
        Map<Long, Double> actualProbabilities = restoreProbabilities(table);

        // Then
        assertAll(
                () -> assertEquals(0.000001D, actualProbabilities.get(101L), PROBABILITY_TOLERANCE),
                () -> assertEquals(0.999999D, actualProbabilities.get(102L), PROBABILITY_TOLERANCE));
    }

    /**
     * 验证空概率空间会在装配入口被立即拒绝。
     */
    @Test
    void shouldRejectNullProbabilitySpace() {
        // Given
        AliasProbabilityTableFactory probabilityTableFactory = new AliasProbabilityTableFactory();

        // When
        NullPointerException exception = assertThrows(NullPointerException.class, () -> probabilityTableFactory.create(null));

        // Then
        assertEquals("probabilitySpace must not be null", exception.getMessage());
    }

    private StrategyProbabilitySpace probabilitySpace(int totalWeight, AwardProbabilityWeight... awardWeights) {
        return new StrategyProbabilitySpace("alias-test", totalWeight, List.of(awardWeights));
    }

    /**
     * 根据每列的接受分支和别名分支，反向计算各奖品的最终理论概率。
     *
     * @param table 已完成装配的别名概率表
     * @return 奖品标识与最终理论概率的映射
     */
    private Map<Long, Double> restoreProbabilities(AliasProbabilityTable table) {
        Map<Long, Double> probabilities = new HashMap<>();
        double columnProbability = 1D / table.size();

        for (int column = 0; column < table.size(); column++) {
            double acceptanceProbability = table.acceptanceProbabilityAt(column);
            long originalAwardId = table.awardIdAt(column);
            long aliasAwardId = table.awardIdAt(table.aliasIndexAt(column));

            probabilities.merge(originalAwardId, columnProbability * acceptanceProbability, Double::sum);
            probabilities.merge(aliasAwardId, columnProbability * (1D - acceptanceProbability), Double::sum);
        }

        return Map.copyOf(probabilities);
    }
}
