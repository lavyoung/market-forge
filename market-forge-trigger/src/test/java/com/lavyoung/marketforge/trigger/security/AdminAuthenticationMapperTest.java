package com.lavyoung.marketforge.trigger.security;

import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证后台认证对象向安全会话身份及接口响应转换的完整契约。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
class AdminAuthenticationMapperTest {

    private static final String PASSWORD_HASH = "$2a$12$test-password-hash";

    private AdminAuthenticationMapper mapper;

    /**
     * 创建无外部依赖的认证转换器。
     */
    @BeforeEach
    void setUp() {
        mapper = new AdminAuthenticationMapper();
    }

    /**
     * Given 已通过认证且包含密码摘要的临时身份，When 创建会话认证，Then 会话只保留非敏感身份和权限。
     */
    @Test
    void shouldCreateCredentialFreeSessionAuthentication() {
        // Given
        Set<SimpleGrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ACTIVITY_ORDER_READ"));
        AdminOperatorDetails details = new AdminOperatorDetails("admin", PASSWORD_HASH, "系统管理员", Set.copyOf(authorities));
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(details, "raw-password", authorities);

        // When
        Authentication sessionAuthentication = mapper.createSessionAuthentication(authenticated);
        AdminOperatorPrincipal principal = assertInstanceOf(AdminOperatorPrincipal.class, sessionAuthentication.getPrincipal());

        // Then
        Set<String> principalComponents = Arrays.stream(AdminOperatorPrincipal.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        assertAll(
                () -> assertTrue(sessionAuthentication.isAuthenticated()),
                () -> assertNull(sessionAuthentication.getCredentials()),
                () -> assertEquals("admin", principal.username()),
                () -> assertEquals("admin", principal.getName()),
                () -> assertEquals("系统管理员", principal.displayName()),
                () -> assertEquals(authorities, Set.copyOf(sessionAuthentication.getAuthorities())),
                () -> assertEquals(Set.of("username", "displayName"), principalComponents)
        );
    }

    /**
     * Given 已建立的后台会话认证，When 转换接口响应，Then 返回用户名、展示名称和全部权限。
     */
    @Test
    void shouldMapSessionAuthenticationToOperatorResponse() {
        // Given
        AdminOperatorPrincipal principal = new AdminOperatorPrincipal("admin", "系统管理员");
        Set<SimpleGrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ACTIVITY_ORDER_READ"));
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);

        // When
        AdminOperatorResponse response = mapper.toResponse(authentication);

        // Then
        assertAll(
                () -> assertEquals("admin", response.username()),
                () -> assertEquals("系统管理员", response.displayName()),
                () -> assertEquals(Set.of("ROLE_ADMIN", "ACTIVITY_ORDER_READ"), response.authorities())
        );
    }

    /**
     * Given 尚未完成认证的请求令牌，When 创建会话身份，Then 拒绝写入未认证身份。
     */
    @Test
    void shouldRejectUnauthenticatedToken() {
        // Given
        Authentication authentication = UsernamePasswordAuthenticationToken.unauthenticated("admin", "password");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> mapper.createSessionAuthentication(authentication));
    }

    /**
     * Given principal 类型不是后台操作员身份，When 创建会话认证，Then 拒绝不受支持的认证来源。
     */
    @Test
    void shouldRejectUnsupportedAuthenticationPrincipal() {
        // Given
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // When & Then
        assertThrows(IllegalStateException.class, () -> mapper.createSessionAuthentication(authentication));
    }

    /**
     * Given 会话认证中不是后台会话身份，When 生成接口响应，Then 拒绝错误的上下文数据。
     */
    @Test
    void shouldRejectUnsupportedSessionPrincipal() {
        // Given
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // When & Then
        assertThrows(IllegalStateException.class, () -> mapper.toResponse(authentication));
    }
}
