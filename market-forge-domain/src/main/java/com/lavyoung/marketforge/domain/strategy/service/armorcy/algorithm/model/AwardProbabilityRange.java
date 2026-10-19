package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;


/**
 * 奖品在累计概率空间中的右侧边界。
 *
 * <p>区间采用左闭右开语义。例如前一个累计上界为 {@code 6}，当前累计上界为
 * {@code 9}，则当前奖品覆盖随机下标 {@code [6, 9)}。</p>
 *
 * <p>所有区间按累计上界升序排列后，可以通过二分查找定位第一个累计上界
 * 大于随机下标的奖品。</p>
 *
 * @param upperBoundExclusive 当前奖品区间的右侧开区间上界，必须大于零
 * @param awardId             奖品标识，必须大于零
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
public record AwardProbabilityRange(
        int upperBoundExclusive,
        long awardId
) {

    /**
     * 校验累计概率区间不变量。
     *
     * @throws IllegalArgumentException 累计上界或奖品标识不合法时抛出
     */
    public AwardProbabilityRange {
        if (upperBoundExclusive <= 0) {
            throw new IllegalArgumentException("upperBoundExclusive must be greater than zero");
        }
        if (awardId <= 0) {
            throw new IllegalArgumentException("awardId must be greater than zero");
        }
    }
}
