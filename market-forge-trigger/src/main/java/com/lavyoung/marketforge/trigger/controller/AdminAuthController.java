package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.admin.auth.IAdminAuthApi;
import com.lavyoung.marketforge.api.admin.auth.request.AdminLoginRequest;
import com.lavyoung.marketforge.api.admin.auth.response.AdminCsrfTokenResponse;
import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 运营后台身份认证 HTTP 适配器。
 *
 * <p>负责接收登录凭证、读取 CSRF Token，并将请求委托给 Spring Security
 * 和后台会话管理器。该类不自行比较密码，也不直接操作 Redis。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@RestController
@RequiredArgsConstructor
public class AdminAuthController implements IAdminAuthApi {

    private final AuthenticationManager authenticationManager;
    private final AdminSessionManager sessionManager;

    @Override
    public Response<AdminCsrfTokenResponse> getCsrfToken() {
        HttpServletRequest request = currentHttpExchange().request();
        Object csrfAttribute = request.getAttribute(CsrfToken.class.getName());

        if (!(csrfAttribute instanceof CsrfToken csrfToken)) {
            throw BusinessException.of(CommonResponseCode.CSRF_TOKEN_INVALID);
        }

        AdminCsrfTokenResponse response = new AdminCsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );

        return Response.success(response);
    }

    @Override
    public Response<AdminOperatorResponse> login(AdminLoginRequest request) {
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        Authentication authenticate = authenticationManager.authenticate(authenticationRequest);
        CurrentHttpExchange exchange = currentHttpExchange();
        AdminOperatorResponse operator = sessionManager.establish(authenticate, exchange.request(), exchange.response());

        return Response.success(operator);
    }

    @Override
    public Response<AdminOperatorResponse> getCurrentOperator() {
        return Response.success(sessionManager.currentOperator());
    }

    @Override
    public Response<Void> logout() {
        CurrentHttpExchange exchange = currentHttpExchange();
        sessionManager.logout(exchange.request(), exchange.response());
        return Response.success();
    }

    /**
     * 获取当前 Controller 调用对应的 Servlet 请求和响应。
     *
     * @return 当前 HTTP 请求与响应
     * @throws IllegalStateException 当前线程不在 Servlet 请求处理中时抛出
     */
    private CurrentHttpExchange currentHttpExchange() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new IllegalStateException("Current servlet request is unavailable");
        }
        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            throw new IllegalStateException("Current servlet response is unavailable");
        }
        return new CurrentHttpExchange(attributes.getRequest(), attributes.getResponse());
    }

    /**
     * 当前 Servlet 请求与响应组合。
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     */
    private record CurrentHttpExchange(HttpServletRequest request, HttpServletResponse response) {
    }
}
