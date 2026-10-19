package com.lavyoung.marketforge.trigger.security;

import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * 运营后台登录会话管理器。
 *
 * <p>负责建立、读取和销毁运营后台登录会话。认证密码校验由
 * AuthenticationManager 完成，本组件只接收已经通过认证的结果。</p>
 *
 * <p>建立会话前会把包含密码摘要的临时认证身份转换为安全会话身份，
 * 然后更换 Session ID，并将 SecurityContext 持久化到 Spring Session。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
@RequiredArgsConstructor
public class AdminSessionManager {

    private final AdminAuthenticationMapper authenticationMapper;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    /**
     * 根据认证成功的结果建立运营后台登录会话。
     *
     * @param authentication 已通过密码校验的认证结果
     * @param request        当前 HTTP 请求
     * @param response       当前 HTTP 响应
     * @return 已登录运营人员信息
     * @throws IllegalArgumentException 认证结果尚未通过认证时抛出
     * @throws IllegalStateException    认证主体类型不正确时抛出
     */
    public AdminOperatorResponse establish(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        Authentication sessionAuthentication = authenticationMapper.createSessionAuthentication(authentication);

        sessionAuthenticationStrategy.onAuthentication(sessionAuthentication, request, response);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(sessionAuthentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);

        return authenticationMapper.toResponse(sessionAuthentication);
    }

    /**
     * 查询当前安全上下文中的运营人员。
     *
     * @return 当前登录运营人员信息
     * @throws AuthenticationCredentialsNotFoundException 当前线程没有认证身份时抛出
     */
    public AdminOperatorResponse currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Admin authentication is required");
        }

        return authenticationMapper.toResponse(authentication);
    }

    /**
     * 注销当前运营后台会话。
     *
     * <p>注销处理器会使当前 HttpSession 失效、清理认证对象并清空
     * SecurityContext。Spring Session 随后同步删除 Redis 中的会话。</p>
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logoutHandler.logout(request, response, authentication);
    }
}
