package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

/**
 * 奖品在整数概率空间中的权重。
 * <p>
 * 数据库中的小数概率会在策略装配阶段转换成整数权重。算法实现只处理整数，
 * 避免在填充查找表或生成累计区间时反复执行 BigDecimal 运算。
 *
 * @param awardId 奖品标识，必须大于零
 * @param weight  奖品整数权重，必须大于零
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
public record AwardProbabilityWeight(
        long awardId,
        int weight
) {
    /**
     * 校验奖品权重不变量。
     *
     * @throws IllegalArgumentException 奖品标识或权重不合法时抛出
     */
    public AwardProbabilityWeight {
        if (awardId <= 0) {
            throw new IllegalArgumentException("awardId must be greater than zero");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be greater than zero");
        }
    }
}
