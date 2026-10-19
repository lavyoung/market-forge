package com.lavyoung.marketforge.app.config.security;

import com.lavyoung.marketforge.trigger.security.AdminOperatorDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证运营后台认证核心组件的账号、密码与权限契约。
 *
 * <p>测试直接组装 Spring Security 的认证提供者，不启动完整应用上下文，
 * 因此不会连接 Redis、数据库、MQ 或 Elasticsearch。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
class AdminAuthenticationConfigurationTest {

    private static final String USERNAME = "admin";
    private static final String RAW_PASSWORD = "Admin-Test-Password-2026";

    private AuthenticationManager authenticationManager;

    /**
     * 创建与生产配置一致的密码编码器、用户查询服务和认证提供者。
     */
    @BeforeEach
    void setUp() {
        AdminAuthenticationConfiguration configuration = new AdminAuthenticationConfiguration();
        PasswordEncoder passwordEncoder = configuration.adminPasswordEncoder();
        AdminAccountProperties properties = new AdminAccountProperties(
                USERNAME,
                passwordEncoder.encode(RAW_PASSWORD),
                "系统管理员",
                Set.of("ROLE_ADMIN", "ACTIVITY_ORDER_READ")
        );
        UserDetailsService userDetailsService = configuration.adminUserDetailsService(properties);
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(passwordEncoder);
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationManager = new ProviderManager(authenticationProvider);
    }

    /**
     * Given 正确的运营账号和密码，When 执行认证，Then 返回已认证身份及配置的全部权限。
     */
    @Test
    void shouldAuthenticateConfiguredAdminAccount() {
        // Given
        Authentication request = UsernamePasswordAuthenticationToken.unauthenticated(USERNAME, RAW_PASSWORD);

        // When
        Authentication result = authenticationManager.authenticate(request);
        AdminOperatorDetails principal = (AdminOperatorDetails) result.getPrincipal();

        // Then
        assertAll(
                () -> assertTrue(result.isAuthenticated()),
                () -> assertEquals(USERNAME, principal.getUsername()),
                () -> assertEquals("系统管理员", principal.displayName()),
                () -> assertEquals(Set.of("ROLE_ADMIN", "ACTIVITY_ORDER_READ"), AuthorityUtils.authorityListToSet(result.getAuthorities())),
                () -> assertNull(result.getCredentials())
        );
    }

    /**
     * Given 已存在账号和错误密码，When 执行认证，Then 拒绝认证且不暴露密码校验细节。
     */
    @Test
    void shouldRejectIncorrectPassword() {
        // Given
        Authentication request = UsernamePasswordAuthenticationToken.unauthenticated(USERNAME, "incorrect-password");

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authenticationManager.authenticate(request));
    }

    /**
     * Given 不存在的账号，When 执行认证，Then 与错误密码一样返回凭证错误，避免账号枚举。
     */
    @Test
    void shouldRejectUnknownAccountWithoutRevealingItsExistence() {
        // Given
        Authentication request = UsernamePasswordAuthenticationToken.unauthenticated("unknown-admin", RAW_PASSWORD);

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authenticationManager.authenticate(request));
    }
}
