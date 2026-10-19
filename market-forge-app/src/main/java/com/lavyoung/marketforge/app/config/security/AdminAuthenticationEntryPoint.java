package com.lavyoung.marketforge.app.config.security;

import com.lavyoung.marketforge.types.model.CommonResponseCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 处理运营后台未认证请求。
 *
 * <p>当匿名用户访问受保护的运营接口时返回 HTTP 401 和统一业务响应，
 * 不跳转 Spring Security 默认登录页面。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
@AllArgsConstructor
public class AdminAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityResponseWriter securityResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        securityResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, CommonResponseCode.AUTHENTICATION_REQUIRED);
    }
}
