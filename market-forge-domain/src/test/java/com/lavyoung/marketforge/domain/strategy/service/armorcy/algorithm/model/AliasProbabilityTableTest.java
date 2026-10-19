package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link AliasProbabilityTable} 的结构约束和不可变性保护。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AliasProbabilityTableTest {

    /**
     * 验证合法概率表能够按列读取奖品、接受概率和别名下标。
     */
    @Test
    void shouldCreateValidAliasProbabilityTable() {
        // Given
        List<Long> awardIds = List.of(101L, 102L, 103L);
        double[] probabilities = {1D, 0.6D, 0.9D};
        int[] aliases = {0, 0, 1};

        // When
        AliasProbabilityTable table = new AliasProbabilityTable(awardIds, probabilities, aliases);

        // Then
        assertEquals(3, table.size());
        assertEquals(102L, table.awardIdAt(1));
        assertEquals(0.6D, table.acceptanceProbabilityAt(1));
        assertEquals(1, table.aliasIndexAt(2));
    }

    /**
     * 验证构造后修改原始数组不会污染概率表。
     */
    @Test
    void shouldDefensivelyCopyConstructorArrays() {
        // Given
        double[] probabilities = {1D, 0.5D};
        int[] aliases = {0, 0};

        // When
        AliasProbabilityTable table = new AliasProbabilityTable(List.of(101L, 102L), probabilities, aliases);
        probabilities[1] = 0D;
        aliases[1] = 1;

        // Then
        assertEquals(0.5D, table.acceptanceProbabilityAt(1));
        assertEquals(0, table.aliasIndexAt(1));
    }

    /**
     * 验证调用数组访问器后修改返回值不会污染概率表。
     */
    @Test
    void shouldDefensivelyCopyAccessorArrays() {
        // Given
        AliasProbabilityTable table = new AliasProbabilityTable(List.of(101L, 102L), new double[]{1D, 0.5D}, new int[]{0, 0});

        // When
        double[] probabilities = table.acceptanceProbabilities();
        int[] aliases = table.aliasIndexes();
        probabilities[1] = 0D;
        aliases[1] = 1;

        // Then
        assertArrayEquals(new double[]{1D, 0.5D}, table.acceptanceProbabilities());
        assertArrayEquals(new int[]{0, 0}, table.aliasIndexes());
    }

    /**
     * 验证概率表组成部分长度不一致时拒绝创建对象。
     */
    @Test
    void shouldRejectMismatchedComponentSizes() {
        // Given
        List<Long> awardIds = List.of(101L, 102L);

        // When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new AliasProbabilityTable(awardIds, new double[]{1D}, new int[]{0, 1}));

        // Then
        assertEquals("alias table components must have the same size", exception.getMessage());
    }

    /**
     * 验证接受概率超出闭区间时拒绝创建对象。
     */
    @Test
    void shouldRejectAcceptanceProbabilityOutsideUnitInterval() {
        // Given
        double invalidProbability = 1.01D;

        // When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new AliasProbabilityTable(List.of(101L), new double[]{invalidProbability}, new int[]{0}));

        // Then
        assertEquals("acceptance probability must be within [0, 1]: index=0", exception.getMessage());
    }
}
