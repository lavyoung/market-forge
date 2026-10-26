package com.lavyoung.marketforge.domain.strategy.model.vo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 策略奖品在整数概率空间中的命中区间。
 *
 * <p>区间采用左闭右开形式 {@code [startInclusive, endExclusive)}。例如总权重为
 * 100，某奖品区间为 {@code [20, 50)}，表示随机值落在 20 至 49 时命中该奖品，
 * 对应整数权重为 30。</p>
 *
 * @param awardId        奖品业务标识
 * @param weight         奖品约简后的整数权重
 * @param startInclusive 命中区间起点，包含该值
 * @param endExclusive   命中区间终点，不包含该值
 * @param probability    奖品理论命中概率，取值范围为 {@code (0, 1]}
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyProbabilityIntervalVO(
        Long awardId,
        int weight,
        int startInclusive,
        int endExclusive,
        BigDecimal probability
) {

    /**
     * 创建并校验奖品概率区间。
     *
     * @throws NullPointerException     奖品标识或理论概率为空时抛出
     * @throws IllegalArgumentException 奖品标识、权重、区间或理论概率不合法时抛出
     */
    public StrategyProbabilityIntervalVO {
        Objects.requireNonNull(awardId, "awardId must not be null");
        Objects.requireNonNull(probability, "probability must not be null");

        if (awardId <= 0) {
            throw new IllegalArgumentException("awardId must be positive");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
        if (startInclusive < 0 || endExclusive <= startInclusive) {
            throw new IllegalArgumentException("probability interval must be increasing");
        }
        if (endExclusive - startInclusive != weight) {
            throw new IllegalArgumentException("probability interval length must equal weight");
        }
        if (probability.compareTo(BigDecimal.ZERO) <= 0 || probability.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("probability must be greater than zero and no greater than one");
        }
    }
}