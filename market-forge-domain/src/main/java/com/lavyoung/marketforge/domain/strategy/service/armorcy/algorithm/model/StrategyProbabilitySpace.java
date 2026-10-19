package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

import java.util.List;
import java.util.Objects;

/**
 * 一次策略算法装配使用的完整整数概率空间。
 * <p>
 * 普通策略的装配键可以是策略标识，例如 {@code 100006}；权重策略的装配键
 * 可以是策略标识与权重档位的组合，例如 {@code 100006_4000}。
 *
 * @param assembleKey  策略装配键
 * @param totalWeight  所有奖品整数权重之和，也是随机数的上界
 * @param awardWeights 参与本次装配的奖品权重，不允许为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
public record StrategyProbabilitySpace(
        String assembleKey,
        int totalWeight,
        List<AwardProbabilityWeight> awardWeights
) {

    /**
     * 建立不可变且完整覆盖的概率空间。
     *
     * @throws NullPointerException     奖品权重集合为空时抛出
     * @throws IllegalArgumentException 装配键、总权重或奖品权重总和不合法时抛出
     */
    public StrategyProbabilitySpace {
        if (assembleKey == null || assembleKey.isBlank()) {
            throw new IllegalArgumentException("assembleKey must not be blank");
        }
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("totalWeight must be greater than zero");
        }

        awardWeights = List.copyOf(Objects.requireNonNull(awardWeights, "awardWeights must not be null"));
        if (awardWeights.isEmpty()) {
            throw new IllegalArgumentException("awardWeights must not be empty");
        }

        long actualTotalWeight = awardWeights.stream().mapToLong(AwardProbabilityWeight::weight).sum();
        if (actualTotalWeight != totalWeight) {
            throw new IllegalArgumentException("totalWeight does not match award weights: expected=" + totalWeight + ", actual=" + actualTotalWeight);
        }
    }
}
