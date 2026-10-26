package com.lavyoung.marketforge.application.strategy.model;

import java.util.List;
import java.util.Objects;

/**
 * 策略发布就绪检查应用结果。
 *
 * <p>是否可以发布由错误集合是否为空推导，避免同时保存 {@code ready}
 * 字段后出现“没有错误但不可发布”的矛盾状态。</p>
 *
 * @param strategyId         策略业务标识
 * @param errors             阻止策略发布的错误
 * @param warnings           不阻止发布的风险提示
 * @param suggestions        配置优化建议
 * @param probabilityPreview 正式抽奖概率空间预览
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record StrategyReadinessResult(
        Long strategyId,
        List<StrategyReadinessIssueResult> errors,
        List<StrategyReadinessIssueResult> warnings,
        List<StrategyReadinessIssueResult> suggestions,
        StrategyProbabilityPreviewResult probabilityPreview
) {
    /**
     * 创建不可变的策略就绪检查结果。
     *
     * @throws NullPointerException 策略标识、问题集合或概率预览为空时抛出
     */
    public StrategyReadinessResult {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
        suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions must not be null"));
        Objects.requireNonNull(probabilityPreview, "probabilityPreview must not be null");
    }

    /**
     * 判断策略是否满足发布条件。
     *
     * @return 没有阻断错误时返回 {@code true}
     */
    public boolean ready() {
        return errors.isEmpty();
    }
}
