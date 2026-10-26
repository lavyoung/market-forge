package com.lavyoung.marketforge.application.strategy.model;

/**
 * 策略就绪检查发现的问题。
 *
 * @param code    稳定的问题编码，供前端识别问题类型
 * @param path    问题对应的策略配置路径
 * @param message 面向运营人员展示的问题说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record StrategyReadinessIssueResult(
        String code,
        String path,
        String message
) {
}
