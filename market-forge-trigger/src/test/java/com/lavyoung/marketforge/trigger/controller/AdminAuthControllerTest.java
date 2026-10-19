package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.admin.auth.request.AdminLoginRequest;
import com.lavyoung.marketforge.api.admin.auth.response.AdminCsrfTokenResponse;
import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证运营后台认证 Controller 的 HTTP 协议适配和委托行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
class AdminAuthControllerTest {

    private AuthenticationManager authenticationManager;
    private AdminSessionManager sessionManager;
    private AdminAuthController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    /**
     * 创建 Controller 依赖和当前 Servlet 请求上下文。
     */
    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        sessionManager = mock(AdminSessionManager.class);
        controller = new AdminAuthController(authenticationManager, sessionManager);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    /**
     * 清理当前线程绑定的 Servlet 请求上下文。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * Given 安全过滤器已生成 CSRF Token，When 获取令牌，Then 返回完整的回传协议。
     */
    @Test
    void shouldReturnCurrentCsrfToken() {
        // Given
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-token-value");
        request.setAttribute(CsrfToken.class.getName(), csrfToken);

        // When
        Response<AdminCsrfTokenResponse> result = controller.getCsrfToken();

        // Then
        assertAll(
                () -> assertEquals(CommonResponseCode.SUCCESS.getCode(), result.code()),
                () -> assertEquals("X-CSRF-TOKEN", result.data().headerName()),
                () -> assertEquals("_csrf", result.data().parameterName()),
                () -> assertEquals("csrf-token-value", result.data().token())
        );
    }

    /**
     * Given 当前请求没有 CSRF Token，When 获取令牌，Then 返回明确的业务异常。
     */
    @Test
    void shouldRejectMissingCsrfToken() {
        BusinessException exception = assertThrows(BusinessException.class, controller::getCsrfToken);
        assertEquals(CommonResponseCode.CSRF_TOKEN_INVALID, exception.getResponseCode());
    }

    /**
     * Given 合法登录请求，When 登录，Then 委托认证管理器和会话管理器建立会话。
     */
    @Test
    void shouldAuthenticateAndEstablishSession() {
        // Given
        AdminLoginRequest loginRequest = new AdminLoginRequest("admin", "password");
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated("details", null, Set.of());
        AdminOperatorResponse operator = new AdminOperatorResponse("admin", "系统管理员", Set.of("ROLE_ADMIN"));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(sessionManager.establish(authenticated, request, response)).thenReturn(operator);

        // When
        Response<AdminOperatorResponse> result = controller.login(loginRequest);

        // Then
        assertSame(operator, result.data());
        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(sessionManager).establish(authenticated, request, response);
    }

    /**
     * Given 当前会话已认证，When 查询当前操作员，Then 返回会话管理器提供的信息。
     */
    @Test
    void shouldReturnCurrentOperator() {
        // Given
        AdminOperatorResponse operator = new AdminOperatorResponse("admin", "系统管理员", Set.of("ROLE_ADMIN"));
        when(sessionManager.currentOperator()).thenReturn(operator);

        // When
        Response<AdminOperatorResponse> result = controller.getCurrentOperator();

        // Then
        assertSame(operator, result.data());
    }

    /**
     * Given 当前请求已登录，When 退出登录，Then 委托会话管理器注销当前会话。
     */
    @Test
    void shouldLogoutCurrentSession() {
        // When
        Response<Void> result = controller.logout();

        // Then
        assertEquals(CommonResponseCode.SUCCESS.getCode(), result.code());
        verify(sessionManager).logout(request, response);
    }
}
