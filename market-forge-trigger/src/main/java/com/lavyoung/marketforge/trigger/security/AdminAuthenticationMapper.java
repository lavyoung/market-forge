package com.lavyoung.marketforge.trigger.security;

import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运营后台认证对象转换器。
 *
 * <p>该组件负责隔离认证期间使用的敏感身份和登录后保存的安全会话身份，
 * 同时负责把 Spring Security 认证对象转换成稳定的后台 API 响应。</p>
 *
 * <p>它只做安全模型与 HTTP 响应模型之间的转换，不校验密码、不操作
 * HttpSession，也不执行业务权限判断。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
public class AdminAuthenticationMapper {

    /**
     * 将认证管理器返回的身份转换为可安全持久化的会话认证对象。
     *
     * <p>返回对象不携带明文密码或 BCrypt 密码摘要，但保留原认证结果中的
     * 权限集合。只有已经通过认证且 principal 类型正确的对象才能转换。</p>
     *
     * @param authentication 认证管理器返回的认证结果
     * @return 不包含密码信息的会话认证对象
     * @throws NullPointerException     authentication 为空时抛出
     * @throws IllegalArgumentException authentication 尚未认证时抛出
     * @throws IllegalStateException    principal 不是后台操作员认证身份时抛出
     */
    public Authentication createSessionAuthentication(Authentication authentication) {
        Objects.requireNonNull(authentication, "authentication must not be null");

        if (!authentication.isAuthenticated()) {
            throw new IllegalArgumentException("authentication must be authenticated");
        }

        if (!(authentication.getPrincipal() instanceof AdminOperatorDetails details)) {
            throw new IllegalStateException("Unsupported admin authentication principal");
        }

        AdminOperatorPrincipal principal = new AdminOperatorPrincipal(details.username(), details.displayName());

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authentication.getAuthorities());
    }

    /**
     * 将会话认证对象转换为运营人员响应。
     *
     * <p>该方法同时服务于登录成功响应和 {@code /me} 当前用户查询，
     * 保证两个接口返回完全一致的身份与权限结构。</p>
     *
     * @param authentication 当前会话中的认证对象
     * @return 不包含认证凭证的运营人员响应
     * @throws NullPointerException     authentication 为空时抛出
     * @throws IllegalArgumentException authentication 尚未认证时抛出
     * @throws IllegalStateException    principal 不是后台会话身份时抛出
     */
    public AdminOperatorResponse toResponse(Authentication authentication) {
        AdminOperatorPrincipal principal = requirePrincipal(authentication);
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return new AdminOperatorResponse(principal.username(), principal.displayName(), authorities);
    }

    /**
     * 校验认证状态并读取后台会话身份。
     *
     * @param authentication 当前认证对象
     * @return 后台会话身份
     * @throws NullPointerException     authentication 为空时抛出
     * @throws IllegalArgumentException authentication 尚未认证时抛出
     * @throws IllegalStateException    principal 类型不正确时抛出
     */
    private AdminOperatorPrincipal requirePrincipal(Authentication authentication) {
        Objects.requireNonNull(authentication, "authentication must not be null");

        if (!authentication.isAuthenticated()) {
            throw new IllegalArgumentException("authentication must be authenticated");
        }
        if (!(authentication.getPrincipal() instanceof AdminOperatorPrincipal principal)) {
            throw new IllegalStateException("Unsupported admin session principal");
        }

        return principal;
    }
}
