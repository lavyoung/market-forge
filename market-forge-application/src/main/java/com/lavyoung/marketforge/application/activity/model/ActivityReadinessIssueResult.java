package com.lavyoung.marketforge.application.activity.model;

/**
 * 活动就绪检查发现的问题。
 *
 * <p>该结果由 Application 层向外提供，不直接暴露领域层的
 * {@code ActivityReadinessIssueVO}。</p>
 *
 * @param code    供前端稳定识别的问题编码
 * @param path    问题对应的活动配置路径
 * @param message 面向运营人员展示的问题说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
public record ActivityReadinessIssueResult(String code, String path, String message) {
}
