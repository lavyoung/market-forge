package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

import java.util.List;
import java.util.Objects;

/**
 * Alias Method 装配完成后的不可变概率表。
 *
 * <p>每个下标代表一列。抽奖时先等概率选择一列，再根据该列的接受概率决定
 * 返回原奖品，还是跳转到别名列对应的奖品。因此单次抽奖只需要常数次访问，
 * 时间复杂度为 O(1)，空间复杂度为 O(n)。</p>
 *
 * <p>数组属于可变对象，构造器和访问器都会创建副本，防止装配完成后的概率表
 * 被外部代码篡改。高频抽样应使用 {@link #acceptanceProbabilityAt(int)} 和
 * {@link #aliasIndexAt(int)}，避免通过数组访问器反复复制完整数组。</p>
 *
 * @param awardIds                每一列对应的奖品标识
 * @param acceptanceProbabilities 每一列保留原奖品的接受概率，取值范围为 [0, 1]
 * @param aliasIndexes            每一列未被接受时跳转的别名列下标
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
public record AliasProbabilityTable(
        List<Long> awardIds,
        double[] acceptanceProbabilities,
        int[] aliasIndexes
) {

    /**
     * 创建结构完整、不会被外部修改的别名概率表。
     *
     * @throws NullPointerException     任一组成部分为空时抛出
     * @throws IllegalArgumentException 概率表为空、数组长度不一致、概率越界、
     *                                  奖品标识非法或别名下标越界时抛出
     */
    public AliasProbabilityTable {
        awardIds = List.copyOf(Objects.requireNonNull(awardIds, "awardIds must not be null"));
        acceptanceProbabilities = Objects.requireNonNull(acceptanceProbabilities, "acceptanceProbabilities must not be null").clone();
        aliasIndexes = Objects.requireNonNull(aliasIndexes, "aliasIndexes must not be null").clone();

        if (awardIds.isEmpty()) {
            throw new IllegalArgumentException("awardIds must not be empty");
        }

        if (awardIds.size() != acceptanceProbabilities.length || awardIds.size() != aliasIndexes.length) {
            throw new IllegalArgumentException("alias table components must have the same size");
        }

        for (int index = 0; index < awardIds.size(); index++) {
            Long awardId = awardIds.get(index);
            double probability = acceptanceProbabilities[index];
            int aliasIndex = aliasIndexes[index];

            if (awardId == null || awardId <= 0) {
                throw new IllegalArgumentException("awardId must be greater than zero: index=" + index);
            }

            if (!Double.isFinite(probability) || probability < 0 || probability > 1) {
                throw new IllegalArgumentException("acceptance probability must be within [0, 1]: index=" + index);
            }

            if (aliasIndex < 0 || aliasIndex >= awardIds.size()) {
                throw new IllegalArgumentException("alias index is out of range: index=" + index);
            }
        }
    }

    @Override
    public double[] acceptanceProbabilities() {
        return acceptanceProbabilities.clone();
    }

    @Override
    public int[] aliasIndexes() {
        return aliasIndexes.clone();
    }

    /**
     * 获取概率表列数。
     *
     * @return 参与 Alias Method 抽奖的奖品数量
     */
    public int size() {
        return awardIds.size();
    }

    /**
     * 获取指定列保留原奖品的接受概率。
     *
     * @param index 概率表列下标
     * @return 取值范围为 [0, 1] 的接受概率
     * @throws IndexOutOfBoundsException 下标越界时抛出
     */
    public double acceptanceProbabilityAt(int index) {
        return acceptanceProbabilities[index];
    }

    /**
     * 获取指定列对应的别名列下标。
     *
     * @param index 概率表列下标
     * @return 未接受原奖品时使用的候补列
     * @throws IndexOutOfBoundsException 下标越界时抛出
     */
    public int aliasIndexAt(int index) {
        return aliasIndexes[index];
    }

    /**
     * 获取指定列对应的奖品标识。
     *
     * @param index 概率表列下标
     * @return 奖品标识
     * @throws IndexOutOfBoundsException 下标越界时抛出
     */
    public long awardIdAt(int index) {
        return awardIds.get(index);
    }
}
