package com.lavyoung.marketforge.domain.activity.model.vo;

import java.util.List;
import java.util.Objects;

/**
 * 活动发布就绪检查报告。
 *
 * <p>报告将检查结果划分为三类：</p>
 *
 * <ul>
 *     <li>错误：阻止活动开放，例如策略未生效、没有可用 SKU；</li>
 *     <li>警告：允许开放，但可能存在运营风险；</li>
 *     <li>建议：不影响正确性，用于改善活动配置。</li>
 * </ul>
 *
 * <p>是否就绪由错误集合实时推导，不额外保存容易产生不一致的布尔字段。</p>
 *
 * @param activityId  被检查的活动业务标识
 * @param errors      阻止活动开放的错误
 * @param warnings    不阻止开放的风险提示
 * @param suggestions 配置优化建议
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/04
 */
public record ActivityReadinessReportVO(
        Long activityId,
        List<ActivityReadinessIssueVO> errors,
        List<ActivityReadinessIssueVO> warnings,
        List<ActivityReadinessIssueVO> suggestions
) {

    /**
     * 创建不可变的活动就绪检查报告。
     *
     * @throws NullPointerException     活动标识、问题集合或集合元素为空时抛出
     * @throws IllegalArgumentException 活动标识不是正数时抛出
     */
    public ActivityReadinessReportVO {
        Objects.requireNonNull(activityId, "activityId must not be null");
        if (activityId <= 0) {
            throw new IllegalArgumentException("activityId must be positive");
        }

        errors = immutableIssues(errors, "errors");
        warnings = immutableIssues(warnings, "warnings");
        suggestions = immutableIssues(suggestions, "suggestions");
    }

    /**
     * 判断活动是否满足开放条件。
     *
     * @return 没有阻断错误时返回 {@code true}
     */
    public boolean ready() {
        return errors.isEmpty();
    }

    /**
     * 复制并校验问题集合，防止调用方在报告创建后修改检查结果。
     *
     * @param issues    待复制的问题集合
     * @param fieldName 集合字段名称
     * @return 不可修改的问题集合
     * @throws NullPointerException 集合或集合元素为空时抛出
     */
    private static List<ActivityReadinessIssueVO> immutableIssues(
            List<ActivityReadinessIssueVO> issues,
            String fieldName
    ) {
        List<ActivityReadinessIssueVO> copied =
                List.copyOf(Objects.requireNonNull(issues, fieldName + " must not be null"));
        copied.forEach(issue -> Objects.requireNonNull(issue, fieldName + " must not contain null"));
        return copied;
    }
}