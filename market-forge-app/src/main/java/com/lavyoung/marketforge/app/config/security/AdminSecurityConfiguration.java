package com.lavyoung.marketforge.app.config.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/**
 * 运营后台 HTTP 安全过滤链配置。
 *
 * <p>该配置保护所有 {@code /admin/**} 运营接口，同时允许匿名获取 CSRF
 * 令牌和提交登录请求。现有抽奖、积分等用户侧接口暂时维持原访问规则，
 * 避免本章认证改造扩大到其他业务入口。</p>
 *
 * <p>认证成功后的安全上下文保存在 HttpSession 中，再由 Spring Session
 * 将会话持久化至 Redis。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Configuration(proxyBeanMethods = false)
@AllArgsConstructor
public class AdminSecurityConfiguration {

    private final AdminAuthenticationEntryPoint authenticationEntryPoint;
    private final AdminAccessDeniedHandler adminAccessDeniedHandler;

    private final SecurityContextRepository securityContextRepository;

    /**
     * 创建运营后台安全过滤链。
     *
     * <p>过滤链只匹配 {@code /admin/**}，因此 CSRF、Session 和认证规则不会
     * 影响现有抽奖、积分等用户侧接口。路径匹配不包含应用上下文路径，
     * 浏览器访问 {@code /api/v1/admin/auth/login} 时，实际匹配路径仍是
     * {@code /admin/auth/login}。</p>
     *
     * @param http Spring Security HTTP 配置入口
     * @return 仅作用于运营后台接口的安全过滤链
     * @throws Exception 安全过滤器无法构建时抛出
     */
    @Bean
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
        CsrfTokenRequestAttributeHandler csrfTokenRequestAttributeHandler = new CsrfTokenRequestAttributeHandler();
        csrfTokenRequestAttributeHandler.setCsrfRequestAttributeName("_csrf");

        return http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/admin/auth/csrf", "/admin/auth/login").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenRequestAttributeHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true)) // 表示认证成功后必须明确调
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(adminAccessDeniedHandler))
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .build();
    }
}
