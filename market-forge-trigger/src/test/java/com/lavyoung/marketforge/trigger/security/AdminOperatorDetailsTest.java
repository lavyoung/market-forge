package com.lavyoung.marketforge.trigger.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证后台操作员安全身份的字段、防御性复制和敏感信息脱敏契约。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
class AdminOperatorDetailsTest {

    private static final String PASSWORD_HASH = "$2a$12$test-password-hash";

    /**
     * Given 可变权限集合，When 创建操作员身份后修改来源集合，Then 身份内部权限保持不可变。
     */
    @Test
    void shouldDefensivelyCopyAuthorities() {
        // Given
        Set<GrantedAuthority> sourceAuthorities = new HashSet<>();
        sourceAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        // When
        AdminOperatorDetails details = new AdminOperatorDetails("admin", PASSWORD_HASH, "系统管理员", sourceAuthorities);
        sourceAuthorities.add(new SimpleGrantedAuthority("ACTIVITY_ORDER_WRITE"));

        // Then
        assertAll(
                () -> assertEquals("admin", details.getUsername()),
                () -> assertEquals(PASSWORD_HASH, details.getPassword()),
                () -> assertEquals("系统管理员", details.displayName()),
                () -> assertEquals(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")), details.authorities()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> details.authorities().add(new SimpleGrantedAuthority("ACTIVITY_ORDER_READ"))),
                () -> assertTrue(details.isAccountNonExpired()),
                () -> assertTrue(details.isAccountNonLocked()),
                () -> assertTrue(details.isCredentialsNonExpired()),
                () -> assertTrue(details.isEnabled())
        );
    }

    /**
     * Given 任一关键属性为空，When 创建操作员身份，Then 立即拒绝不完整的认证主体。
     */
    @Test
    void shouldRejectNullIdentityComponents() {
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new AdminOperatorDetails(null, PASSWORD_HASH, "系统管理员", authorities)),
                () -> assertThrows(NullPointerException.class,
                        () -> new AdminOperatorDetails("admin", null, "系统管理员", authorities)),
                () -> assertThrows(NullPointerException.class,
                        () -> new AdminOperatorDetails("admin", PASSWORD_HASH, null, authorities)),
                () -> assertThrows(NullPointerException.class,
                        () -> new AdminOperatorDetails("admin", PASSWORD_HASH, "系统管理员", null))
        );
    }

    /**
     * Given 包含密码摘要的操作员身份，When 输出调试文本，Then 不泄露密码摘要。
     */
    @Test
    void shouldRedactPasswordHashFromDebugText() {
        // Given
        AdminOperatorDetails details = new AdminOperatorDetails(
                "admin", PASSWORD_HASH, "系统管理员", Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // When
        String debugText = details.toString();

        // Then
        assertAll(
                () -> assertTrue(debugText.contains("admin")),
                () -> assertTrue(debugText.contains("系统管理员")),
                () -> assertFalse(debugText.contains(PASSWORD_HASH))
        );
    }
}
