package com.lavyoung.marketforge.trigger.security;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * 后台操作员的 Spring Security 登录身份。
 *
 * <p>该对象由应用装配层在加载后台账号时创建，并作为认证主体保存到
 * {@link org.springframework.security.core.Authentication} 中。Trigger 层可以据此
 * 获取当前操作员的用户名、显示名称和权限，并转换为后台认证接口的响应对象。</p>
 *
 * <p>该类型属于安全协议适配模型，不是领域实体，也不应该参与营销领域规则计算。</p>
 *
 * @param username     后台登录用户名
 * @param passwordHash BCrypt 密码摘要，只用于认证匹配
 * @param displayName  后台页面展示的操作员名称
 * @param authorities  操作员拥有的权限集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
public record AdminOperatorDetails(
        String username,
        String passwordHash,
        String displayName,
        Set<GrantedAuthority> authorities
) implements UserDetails {

    /**
     * 创建后台操作员登录身份，并对关键属性进行防御性校验。
     *
     * @param username     后台登录用户名
     * @param passwordHash BCrypt 密码摘要
     * @param displayName  后台展示名称
     * @param authorities  权限集合
     * @throws NullPointerException 当任意关键属性为空时抛出
     */
    public AdminOperatorDetails {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        authorities = Set.copyOf(Objects.requireNonNull(authorities, "authorities must not be null"));
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * 输出不包含密码摘要的调试信息，避免认证信息通过日志泄露。
     *
     * @return 已脱敏的操作员身份描述
     */
    @Override
    public String toString() {
        return "AdminOperatorDetails[username=%s, displayName=%s, authorities=%s]"
                .formatted(username, displayName, authorities);
    }

}
