package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 活动开放就绪检查响应。
 *
 * <p>业务标识以字符串输出，避免超过 JavaScript 安全整数范围后发生精度丢失。</p>
 *
 * @param activityId  被检查的活动业务标识
 * @param checkedAt   本次检查采用的业务时间
 * @param ready       是否满足活动开放条件
 * @param errors      阻止活动开放的错误
 * @param warnings    不阻止开放的风险提示
 * @param suggestions 配置优化建议
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
@Schema(description = "活动开放就绪检查结果")
public record ActivityReadinessResponse(
        @Schema(description = "活动业务标识", example = "100301") String activityId,
        @Schema(description = "检查时间") LocalDateTime checkedAt,
        @Schema(description = "是否满足开放条件") boolean ready,
        @Schema(description = "阻止活动开放的错误") List<ActivityReadinessIssueResponse> errors,
        @Schema(description = "不阻止开放的风险提示") List<ActivityReadinessIssueResponse> warnings,
        @Schema(description = "配置优化建议") List<ActivityReadinessIssueResponse> suggestions
) {

    /**
     * 创建不可变的活动开放就绪响应。
     *
     * @throws NullPointerException 活动标识、检查时间或问题集合为空时抛出
     */
    public ActivityReadinessResponse {
        Objects.requireNonNull(activityId, "activityId must not be null");
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
        suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions must not be null"));
    }
}
