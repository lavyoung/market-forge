package com.lavyoung.marketforge.domain.activity.model.vo;

import static com.lavyoung.marketforge.types.validation.ModelPreconditions.requireText;

/**
 * 活动发布就绪检查发现的问题。
 *
 * <p>{@code code} 是供 API 和管理端识别的稳定编码；{@code path} 指向发生问题的
 * 配置位置；{@code message} 用于向运营人员解释具体原因。</p>
 *
 * @param code    稳定的问题编码
 * @param path    问题对应的活动配置路径
 * @param message 面向运营人员的问题说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/04
 */
public record ActivityReadinessIssueVO(String code, String path, String message) {

    /**
     * 创建并规范化活动就绪问题。
     *
     * <p>问题编码、配置路径和运营说明均不需要保留首尾空格，因此在公共非空白
     * 校验通过后显式执行规范化。</p>
     *
     * @throws NullPointerException     编码、路径或说明为空时抛出
     * @throws IllegalArgumentException 编码、路径或说明为空白时抛出
     */
    public ActivityReadinessIssueVO {
        code = requireText(code, "code").trim();
        path = requireText(path, "path").trim();
        message = requireText(message, "message").trim();
    }
}
