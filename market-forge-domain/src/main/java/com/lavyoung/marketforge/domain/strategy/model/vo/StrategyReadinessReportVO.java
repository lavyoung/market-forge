package com.lavyoung.marketforge.domain.strategy.model.vo;

import java.util.List;
import java.util.Objects;

/**
 * 策略发布就绪检查报告。
 *
 * <p>报告将检查结果分为三类：</p>
 *
 * <ul>
 *     <li>错误：阻止策略发布，例如奖品不存在、概率非法或规则引用错误；</li>
 *     <li>警告：允许发布，但可能存在运营风险；</li>
 *     <li>建议：不影响正确性，只用于改进策略配置。</li>
 * </ul>
 *
 * <p>是否就绪由错误集合是否为空实时推导，不额外保存可产生不一致的
 * {@code ready} 字段。</p>
 *
 * @param strategyId         被检查的策略业务标识
 * @param errors             阻止发布的错误集合
 * @param warnings           不阻止发布的风险提示集合
 * @param suggestions        配置优化建议集合
 * @param probabilityPreview 当前策略的概率空间预览
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyReadinessReportVO(
        Long strategyId,
        List<StrategyReadinessIssueVO> errors,
        List<StrategyReadinessIssueVO> warnings,
        List<StrategyReadinessIssueVO> suggestions,
        StrategyProbabilityPreviewVO probabilityPreview
) {

    /**
     * 创建不可变的策略就绪检查报告。
     *
     * @throws NullPointerException     策略标识、问题集合、集合元素或概率预览为空时抛出
     * @throws IllegalArgumentException 策略标识不是正数时抛出
     */
    public StrategyReadinessReportVO {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        if (strategyId <= 0) {
            throw new IllegalArgumentException("strategyId must be positive");
        }

        errors = immutableIssues(errors, "errors");
        warnings = immutableIssues(warnings, "warnings");
        suggestions = immutableIssues(suggestions, "suggestions");
        Objects.requireNonNull(probabilityPreview, "probabilityPreview must not be null");
    }

    /**
     * 判断当前策略是否满足发布条件。
     *
     * @return 没有任何阻断错误时返回 {@code true}
     */
    public boolean ready() {
        return errors.isEmpty();
    }

    /**
     * 复制并校验问题集合。
     *
     * @param issues    待复制的问题集合
     * @param fieldName 集合字段名称
     * @return 不可修改的问题集合
     * @throws NullPointerException 集合或集合元素为空时抛出
     */
    private static List<StrategyReadinessIssueVO> immutableIssues(
            List<StrategyReadinessIssueVO> issues,
            String fieldName
    ) {
        List<StrategyReadinessIssueVO> copied = List.copyOf(
                Objects.requireNonNull(issues, fieldName + " must not be null")
        );
        copied.forEach(issue -> Objects.requireNonNull(issue, fieldName + " must not contain null"));
        return copied;
    }
}