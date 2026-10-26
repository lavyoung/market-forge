package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 策略发布就绪检查响应。
 *
 * @param strategyId         策略业务标识
 * @param ready              是否满足发布条件
 * @param errors             阻止发布的错误
 * @param warnings           不阻止发布的风险提示
 * @param suggestions        配置优化建议
 * @param probabilityPreview 正式抽奖概率空间预览
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Schema(description = "策略发布就绪检查结果")
public record StrategyReadinessResponse(
        @Schema(description = "策略业务标识", example = "900901001", type = "string") String strategyId,
        @Schema(description = "是否满足发布条件", example = "true") boolean ready,
        @Schema(description = "阻止发布的错误") List<StrategyReadinessIssueResponse> errors,
        @Schema(description = "发布风险提示") List<StrategyReadinessIssueResponse> warnings,
        @Schema(description = "配置优化建议") List<StrategyReadinessIssueResponse> suggestions,
        @Schema(description = "概率空间预览") StrategyProbabilityPreviewResponse probabilityPreview
) implements Serializable {

    /**
     * 创建不可变的策略就绪检查响应。
     *
     * @throws NullPointerException 策略标识、问题集合或概率预览为空时抛出
     */
    public StrategyReadinessResponse {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
        suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions must not be null"));
        Objects.requireNonNull(probabilityPreview, "probabilityPreview must not be null");
    }
}
