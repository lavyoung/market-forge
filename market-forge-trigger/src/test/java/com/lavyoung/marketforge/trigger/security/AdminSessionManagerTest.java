package com.lavyoung.marketforge.trigger.security;

import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证运营后台会话的建立、读取和注销行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
class AdminSessionManagerTest {

    private AdminAuthenticationMapper authenticationMapper;
    private SecurityContextRepository securityContextRepository;
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private AdminSessionManager sessionManager;

    /**
     * 创建隔离外部会话设施的测试对象。
     */
    @BeforeEach
    void setUp() {
        authenticationMapper = mock(AdminAuthenticationMapper.class);
        securityContextRepository = mock(SecurityContextRepository.class);
        sessionAuthenticationStrategy = mock(SessionAuthenticationStrategy.class);
        sessionManager = new AdminSessionManager(authenticationMapper, securityContextRepository, sessionAuthenticationStrategy);
    }

    /**
     * 清理线程持有的安全上下文，防止测试之间共享认证状态。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Given 密码认证已经成功，When 建立会话，Then 保存脱敏认证并用它生成响应。
     */
    @Test
    void shouldEstablishCredentialFreeAdminSession() {
        // Given
        Authentication credentialAuthentication = UsernamePasswordAuthenticationToken.authenticated("details", null, Set.of());
        Authentication sessionAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                new AdminOperatorPrincipal("admin", "系统管理员"), null, Set.of());
        AdminOperatorResponse expected = new AdminOperatorResponse("admin", "系统管理员", Set.of());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authenticationMapper.createSessionAuthentication(credentialAuthentication)).thenReturn(sessionAuthentication);
        when(authenticationMapper.toResponse(sessionAuthentication)).thenReturn(expected);

        // When
        AdminOperatorResponse actual = sessionManager.establish(credentialAuthentication, request, response);

        // Then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertSame(sessionAuthentication, SecurityContextHolder.getContext().getAuthentication())
        );
        verify(sessionAuthenticationStrategy).onAuthentication(sessionAuthentication, request, response);
        verify(securityContextRepository).saveContext(SecurityContextHolder.getContext(), request, response);
        verify(authenticationMapper).toResponse(sessionAuthentication);
    }

    /**
     * Given 当前线程存在已认证会话，When 查询当前操作员，Then 返回 Mapper 转换结果。
     */
    @Test
    void shouldReturnCurrentOperator() {
        // Given
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                new AdminOperatorPrincipal("admin", "系统管理员"), null, Set.of());
        AdminOperatorResponse expected = new AdminOperatorResponse("admin", "系统管理员", Set.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authenticationMapper.toResponse(authentication)).thenReturn(expected);

        // When
        AdminOperatorResponse actual = sessionManager.currentOperator();

        // Then
        assertSame(expected, actual);
    }

    /**
     * Given 当前线程没有认证身份，When 查询当前操作员，Then 返回未认证异常。
     */
    @Test
    void shouldRejectCurrentOperatorQueryWithoutAuthentication() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, sessionManager::currentOperator);
    }

    /**
     * Given 当前请求已经建立 Session，When 注销，Then Session 和安全上下文均被清理。
     */
    @Test
    void shouldInvalidateSessionAndClearSecurityContextOnLogout() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession(true).setAttribute("state", "authenticated");
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                new AdminOperatorPrincipal("admin", "系统管理员"), null, Set.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        sessionManager.logout(request, response);

        // Then
        assertAll(
                () -> assertNull(request.getSession(false)),
                () -> assertNull(SecurityContextHolder.getContext().getAuthentication()),
                () -> assertEquals("", response.getContentAsString())
        );
    }
}
