package com.lavyoung.marketforge.domain.strategy.model.vo;

import java.util.Objects;

/**
 * 策略就绪检查发现的单个问题。
 *
 * <p>{@code code} 是供前端和测试稳定识别的问题编码；{@code path} 指向问题所在的
 * 策略字段，例如 {@code awards[0].awardRate}；{@code message} 是运营人员可以直接
 * 理解的说明。问题属于错误、警告还是建议，由
 * {@link StrategyReadinessReportVO} 中所在的集合决定。</p>
 *
 * @param code    稳定的问题编码
 * @param path    问题对应的策略配置路径
 * @param message 面向运营人员的问题说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyReadinessIssueVO(
        String code,
        String path,
        String message
) {

    /**
     * 创建并规范化策略就绪问题。
     *
     * @throws NullPointerException     问题编码、配置路径或问题说明为空时抛出
     * @throws IllegalArgumentException 问题编码、配置路径或问题说明为空白字符串时抛出
     */
    public StrategyReadinessIssueVO {
        code = requireText(code, "code");
        path = requireText(path, "path");
        message = requireText(message, "message");
    }

    /**
     * 校验并去除文本首尾空格。
     *
     * @param value     待校验文本
     * @param fieldName 字段名称
     * @return 去除首尾空格后的文本
     * @throws NullPointerException     文本为空时抛出
     * @throws IllegalArgumentException 文本为空白字符串时抛出
     */
    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}