package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 策略整数概率空间预览响应。
 *
 * @param available   是否成功生成概率预览
 * @param totalWeight 概率空间总整数权重
 * @param intervals   按实际命中顺序排列的奖品区间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Schema(description = "策略整数概率空间预览")
public record StrategyProbabilityPreviewResponse(
        @Schema(description = "概率预览是否可用", example = "true") boolean available,
        @Schema(description = "概率空间总整数权重", example = "200") int totalWeight,
        @Schema(description = "奖品概率命中区间") List<StrategyProbabilityIntervalResponse> intervals
) implements Serializable {

    /**
     * 创建不可变的概率预览响应。
     *
     * @throws NullPointerException 概率区间集合为空时抛出
     */
    public StrategyProbabilityPreviewResponse {
        intervals = List.copyOf(Objects.requireNonNull(intervals, "intervals must not be null"));
    }
}
