package com.lavyoung.marketforge.application.strategy.model;

import java.util.List;
import java.util.Objects;

/**
 * 策略整数概率空间预览结果。
 *
 * @param available   是否成功生成概率预览
 * @param totalWeight 概率空间总整数权重
 * @param intervals   按实际命中顺序排列的奖品区间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record StrategyProbabilityPreviewResult(
        boolean available,
        int totalWeight,
        List<StrategyProbabilityIntervalResult> intervals
) {
    /**
     * 创建不可变的概率预览结果。
     *
     * @throws NullPointerException 概率区间集合为空时抛出
     */
    public StrategyProbabilityPreviewResult {
        intervals = List.copyOf(Objects.requireNonNull(intervals, "intervals must not be null"));
    }
}
