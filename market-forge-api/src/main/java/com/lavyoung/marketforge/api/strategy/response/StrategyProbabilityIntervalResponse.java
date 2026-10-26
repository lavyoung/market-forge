package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 策略奖品概率命中区间响应。
 *
 * @param awardId        奖品业务标识
 * @param weight         约简后的整数权重
 * @param startInclusive 左闭命中区间起点
 * @param endExclusive   右开命中区间终点
 * @param probability    奖品理论命中概率
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Schema(description = "策略奖品概率命中区间")
public record StrategyProbabilityIntervalResponse(
        @Schema(description = "奖品业务标识", example = "900901011", type = "string") String awardId,
        @Schema(description = "整数权重", example = "30") int weight,
        @Schema(description = "区间起点，包含该值", example = "0") int startInclusive,
        @Schema(description = "区间终点，不包含该值", example = "30") int endExclusive,
        @Schema(description = "理论命中概率", example = "0.150000000000") BigDecimal probability
) implements Serializable {
}
