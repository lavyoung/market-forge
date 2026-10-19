package com.lavyoung.marketforge.trigger.security;

import javax.security.auth.Subject;
import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

/**
 * 运营后台已经完成认证的会话身份。
 *
 * <p>该对象只保存页面展示和身份识别所需的非敏感信息。认证成功后，
 * Controller 将临时的 {@link AdminOperatorDetails} 转换为该对象，
 * 再将其保存到 Spring SecurityContext 和 Redis Session。</p>
 *
 * <p>权限信息不在该对象中重复保存，而是由 Spring Security 的
 * {@link org.springframework.security.core.Authentication} 统一持有。</p>
 *
 * @param username    运营人员登录名
 * @param displayName 运营人员展示名称
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
public record AdminOperatorPrincipal(
        String username,
        String displayName
) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建不包含认证凭证的会话身份。
     *
     * @throws NullPointerException 用户名或展示名称为空时抛出
     */
    public AdminOperatorPrincipal {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
    }

    /**
     * 返回 Java Security Principal 约定的身份名称。
     *
     * @return 运营人员登录名
     */
    @Override
    public String getName() {
        return username;
    }

    @Override
    public boolean implies(Subject subject) {
        return Principal.super.implies(subject);
    }
}
