package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证奖品整数权重和策略概率空间的领域不变量。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyProbabilitySpaceTest {

    /**
     * Given 合法的奖品权重，When 创建概率空间，Then 保存完整概率范围并复制输入集合。
     */
    @Test
    void shouldCreateImmutableProbabilitySpace() {
        // Given
        List<AwardProbabilityWeight> source = new ArrayList<>(List.of(
                new AwardProbabilityWeight(101L, 7),
                new AwardProbabilityWeight(102L, 3)
        ));

        // When
        StrategyProbabilitySpace probabilitySpace = new StrategyProbabilitySpace("100006", 10, source);
        source.clear();

        // Then
        assertEquals("100006", probabilitySpace.assembleKey());
        assertEquals(10, probabilitySpace.totalWeight());
        assertEquals(2, probabilitySpace.awardWeights().size());
        assertThrows(UnsupportedOperationException.class,
                () -> probabilitySpace.awardWeights().add(new AwardProbabilityWeight(103L, 1)));
    }

    /**
     * Given 非法奖品标识或非正权重，When 创建奖品权重，Then 拒绝进入概率空间。
     */
    @Test
    void shouldRejectInvalidAwardWeight() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () -> new AwardProbabilityWeight(0L, 1));
        assertThrows(IllegalArgumentException.class, () -> new AwardProbabilityWeight(101L, 0));
        assertThrows(IllegalArgumentException.class, () -> new AwardProbabilityWeight(101L, -1));
    }

    /**
     * Given 空白装配键、无效总权重或空奖品集合，When 创建概率空间，Then 明确拒绝非法输入。
     */
    @Test
    void shouldRejectIncompleteProbabilitySpace() {
        // Given
        List<AwardProbabilityWeight> awardWeights = List.of(new AwardProbabilityWeight(101L, 1));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> new StrategyProbabilitySpace(" ", 1, awardWeights));
        assertThrows(IllegalArgumentException.class, () -> new StrategyProbabilitySpace("100006", 0, awardWeights));
        assertThrows(NullPointerException.class, () -> new StrategyProbabilitySpace("100006", 1, null));
        assertThrows(IllegalArgumentException.class, () -> new StrategyProbabilitySpace("100006", 1, List.of()));
    }

    /**
     * Given 声明总权重与奖品权重之和不同，When 创建概率空间，Then 拒绝形成有空洞的随机区间。
     */
    @Test
    void shouldRejectMismatchedTotalWeight() {
        // Given
        List<AwardProbabilityWeight> awardWeights = List.of(
                new AwardProbabilityWeight(101L, 7),
                new AwardProbabilityWeight(102L, 3)
        );

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> new StrategyProbabilitySpace("100006", 11, awardWeights));
    }
}
