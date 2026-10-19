package com.lavyoung.marketforge.api.admin.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * 当前登录运营人员信息。
 *
 * <p>该响应由服务端根据 Spring Security 中的认证信息构造，供前端初始化用户状态、
 * 展示登录人信息以及执行页面级权限判断。</p>
 *
 * @param username    运营人员登录名
 * @param displayName 运营人员展示名称
 * @param authorities 当前账号拥有的权限标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Schema(description = "当前登录运营人员信息")
public record AdminOperatorResponse(
        @Schema(description = "运营人员登录名", example = "admin")
        String username,

        @Schema(description = "运营人员展示名称", example = "系统管理员")
        String displayName,

        @Schema(description = "权限标识集合", example = "[\"ROLE_ADMIN\", \"ACTIVITY_ORDER_READ\"]")
        Set<String> authorities
) implements Serializable {

    /**
     * 创建不可变的运营人员响应。
     *
     * @throws NullPointerException 用户名、展示名称或权限集合为空时抛出
     */
    public AdminOperatorResponse {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        authorities = Set.copyOf(Objects.requireNonNull(authorities, "authorities must not be null"));
    }
}
