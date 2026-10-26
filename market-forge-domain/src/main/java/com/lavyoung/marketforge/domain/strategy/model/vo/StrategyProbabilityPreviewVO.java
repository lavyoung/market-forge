package com.lavyoung.marketforge.domain.strategy.model.vo;

import java.util.List;
import java.util.Objects;

/**
 * 策略整数概率空间预览。
 *
 * <p>配置可以成功转换为概率空间时，{@code available} 为 {@code true}，
 * {@code totalWeight} 表示随机值上界，全部奖品区间必须从零开始连续覆盖
 * {@code [0, totalWeight)}。</p>
 *
 * <p>概率配置不合法时使用 {@link #unavailable()} 表示无法生成预览，
 * 避免在领域模型中使用 {@code null} 或将 {@code Optional} 作为字段。</p>
 *
 * @param available   是否成功生成概率预览
 * @param totalWeight 概率空间总整数权重；不可用时为零
 * @param intervals   按命中区间顺序排列的奖品概率区间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyProbabilityPreviewVO(
        boolean available,
        int totalWeight,
        List<StrategyProbabilityIntervalVO> intervals
) {

    /**
     * 创建并校验完整概率预览。
     *
     * @throws NullPointerException     概率区间集合或集合元素为空时抛出
     * @throws IllegalArgumentException 可用状态、总权重和概率区间不一致，或者区间不连续时抛出
     */
    public StrategyProbabilityPreviewVO {
        intervals = List.copyOf(Objects.requireNonNull(intervals, "intervals must not be null"));

        if (!available) {
            if (totalWeight != 0 || !intervals.isEmpty()) {
                throw new IllegalArgumentException("unavailable preview must not contain probability data");
            }
        } else {
            if (totalWeight <= 0 || intervals.isEmpty()) {
                throw new IllegalArgumentException("available preview must contain positive probability data");
            }

            int expectedStart = 0;
            for (StrategyProbabilityIntervalVO interval : intervals) {
                Objects.requireNonNull(interval, "probability interval must not be null");
                if (interval.startInclusive() != expectedStart) {
                    throw new IllegalArgumentException("probability intervals must be continuous");
                }
                expectedStart = interval.endExclusive();
            }

            if (expectedStart != totalWeight) {
                throw new IllegalArgumentException("probability intervals must cover total weight");
            }
        }
    }

    /**
     * 创建无法生成概率空间时使用的空预览。
     *
     * @return 不包含总权重和奖品区间的不可用预览
     */
    public static StrategyProbabilityPreviewVO unavailable() {
        return new StrategyProbabilityPreviewVO(false, 0, List.of());
    }
}