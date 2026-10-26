package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 策略就绪检查问题响应。
 *
 * @param code    稳定的问题编码
 * @param path    问题对应的策略配置路径
 * @param message 面向运营人员的问题说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Schema(description = "策略就绪检查问题")
public record StrategyReadinessIssueResponse(
        @Schema(description = "问题编码", example = "strategy.rate.invalid") String code,
        @Schema(description = "配置字段路径", example = "awards[*].awardRate") String path,
        @Schema(description = "问题说明", example = "抽奖策略概率配置无效") String message
) implements Serializable {
}
