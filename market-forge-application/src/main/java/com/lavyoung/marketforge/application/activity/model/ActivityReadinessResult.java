package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 活动运行就绪检查应用结果。
 *
 * <p>该结果同时服务活动开放和展台发布场景，记录本次检查采用的业务时间，
 * 便于运营后台判断检查结果是否已经过期。是否就绪由错误集合实时推导，
 * 不额外保存可能产生矛盾的布尔字段。</p>
 *
 * @param activityId  被检查的活动业务标识
 * @param checkedAt   本次检查采用的业务时间
 * @param errors      阻止活动开放或发布的错误
 * @param warnings    不阻止操作的风险提示
 * @param suggestions 配置优化建议
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
public record ActivityReadinessResult(
        Long activityId,
        LocalDateTime checkedAt,
        List<ActivityReadinessIssueResult> errors,
        List<ActivityReadinessIssueResult> warnings,
        List<ActivityReadinessIssueResult> suggestions
) {

    /**
     * 创建不可变的活动就绪检查结果。
     *
     * @throws NullPointerException     活动标识、检查时间、问题集合或集合元素为空时抛出
     * @throws IllegalArgumentException 活动标识不是正数时抛出
     */
    public ActivityReadinessResult {
        Objects.requireNonNull(activityId, "activityId must not be null");
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");

        if (activityId <= 0) {
            throw new IllegalArgumentException("activityId must be positive");
        }

        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
        suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions must not be null"));
    }

    /**
     * 判断活动配置是否满足开放或展台发布条件。
     *
     * @return 没有阻断错误时返回 {@code true}
     */
    public boolean ready() {
        return errors.isEmpty();
    }
}
