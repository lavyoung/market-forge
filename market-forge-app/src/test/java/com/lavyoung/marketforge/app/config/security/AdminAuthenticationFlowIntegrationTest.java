package com.lavyoung.marketforge.app.config.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.app.config.WebConfig;
import com.lavyoung.marketforge.trigger.security.AdminOperatorDetails;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证运营后台从 CSRF 获取、登录、会话恢复到注销的完整 HTTP 链路。
 *
 * <p>测试使用内存 HttpSession，不连接 Redis、数据库、MQ 或 Elasticsearch，
 * 重点验证真实 Spring Security 过滤链与认证 Controller 的协作行为。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        AdminSecurityConfiguration.class,
        AdminAuthenticationEntryPoint.class,
        AdminAccessDeniedHandler.class,
        SecurityResponseWriter.class,
        AdminAuthenticationFlowIntegrationTest.TestWebConfiguration.class
})
class AdminAuthenticationFlowIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "integration-test-password";
    private static final String ADMIN_AUTH_PATH = WebConfig.API_PATH_PREFIX + "/admin/auth";

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    /**
     * 使用真实 Spring Security FilterChainProxy 创建 MockMvc。
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
    }

    /**
     * Given 匿名运营客户端，When 依次获取 CSRF、登录、查询自身并注销，Then 会话完整建立并最终失效。
     *
     * @throws Exception MockMvc 请求或 JSON 解析失败时抛出
     */
    @Test
    void shouldCompleteLoginSessionAndLogoutFlow() throws Exception {
        // Given
        CsrfExchange csrf = requestCsrfToken();
        String loginBody = objectMapper.writeValueAsString(Map.of("username", USERNAME, "password", PASSWORD));

        // When: 登录并沿用获取 CSRF Token 时创建的 Session
        MvcResult loginResult = mockMvc.perform(post(ADMIN_AUTH_PATH + "/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andExpect(jsonPath("$.data.displayName").value("系统管理员"))
                .andExpect(jsonPath("$.data.authorities").isArray())
                .andReturn();

        MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(authenticatedSession);

        // Then: 后续请求可以从同一 Session 恢复认证身份
        mockMvc.perform(get(ADMIN_AUTH_PATH + "/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andExpect(jsonPath("$.data.displayName").value("系统管理员"));

        // When: 携带同一 CSRF Token 注销
        mockMvc.perform(post(ADMIN_AUTH_PATH + "/logout")
                        .session(authenticatedSession)
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()));

        // Then: 原 Session 已失效，匿名请求无法再访问当前用户接口
        assertTrue(authenticatedSession.isInvalid());
        mockMvc.perform(get(ADMIN_AUTH_PATH + "/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.AUTHENTICATION_REQUIRED.getCode()));
    }

    /**
     * Given 正确用户名和错误密码，When 登录，Then 返回统一认证失败响应且不建立认证会话。
     *
     * @throws Exception MockMvc 请求或 JSON 解析失败时抛出
     */
    @Test
    void shouldRejectIncorrectPasswordWithoutRevealingDetails() throws Exception {
        // Given
        CsrfExchange csrf = requestCsrfToken();
        String loginBody = objectMapper.writeValueAsString(Map.of("username", USERNAME, "password", "incorrect-password"));

        // When & Then
        mockMvc.perform(post(ADMIN_AUTH_PATH + "/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.AUTHENTICATION_FAILED.getCode()))
                .andExpect(jsonPath("$.message").value(CommonResponseCode.AUTHENTICATION_FAILED.getMsg()));
    }

    /**
     * 获取真实过滤链生成的 CSRF Token 和对应 Session。
     *
     * @return 后续写请求需要携带的 CSRF 交换信息
     * @throws Exception MockMvc 请求或 JSON 解析失败时抛出
     */
    private CsrfExchange requestCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get(ADMIN_AUTH_PATH + "/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headerName").isNotEmpty())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return new CsrfExchange(session, data.path("headerName").asText(), data.path("token").asText());
    }

    /**
     * 一次 CSRF 获取操作产生的 Session、请求头名称和令牌值。
     *
     * @param session    CSRF Token 所属 Session
     * @param headerName 后续请求携带 Token 的请求头名称
     * @param token      CSRF Token
     */
    private record CsrfExchange(MockHttpSession session, String headerName, String token) {
    }

    /**
     * 认证流程集成测试所需的最小 Spring MVC 配置。
     */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @ComponentScan(
            basePackages = "com.lavyoung.marketforge.trigger",
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.lavyoung\\.marketforge\\.trigger\\.(controller\\.AdminAuthController|security\\.(AdminSessionManager|AdminAuthenticationMapper)|exception\\.GlobalExceptionHandler)"
            )
    )
    static class TestWebConfiguration implements WebMvcConfigurer {

        /**
         * 为测试中的触发器 Controller 添加与生产环境一致的 API 版本前缀。
         *
         * @param configurer Spring MVC 路径匹配配置器
         */
        @Override
        public void configurePathMatch(PathMatchConfigurer configurer) {
            configurer.addPathPrefix(
                    WebConfig.API_PATH_PREFIX,
                    HandlerTypePredicate.forBasePackage("com.lavyoung.marketforge.trigger.controller")
            );
        }

        /**
         * 创建统一响应序列化使用的 JSON 映射器。
         *
         * @return 测试上下文使用的 JSON 映射器
         */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /**
         * 创建与生产登录流程一致的认证管理器。
         *
         * @return 使用 BCrypt 校验测试运营账号的认证管理器
         */
        @Bean
        AuthenticationManager authenticationManager() {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
            String passwordHash = passwordEncoder.encode(PASSWORD);
            Set<GrantedAuthority> authorities = Set.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ACTIVITY_ORDER_READ"));
            UserDetailsService userDetailsService = username -> {
                if (!USERNAME.equals(username)) {
                    throw new UsernameNotFoundException("Admin account not found");
                }
                return new AdminOperatorDetails(USERNAME, passwordHash, "系统管理员", authorities);
            };

            DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
            provider.setUserDetailsService(userDetailsService);
            return new ProviderManager(provider);
        }

        /**
         * 创建与生产配置一致的 HttpSession 安全上下文仓储。
         *
         * @return HttpSession 安全上下文仓储
         */
        @Bean
        SecurityContextRepository securityContextRepository() {
            return new HttpSessionSecurityContextRepository();
        }

        /**
         * 创建防止 Session 固定攻击的会话策略。
         *
         * @return 登录后更换 Session ID 的策略
         */
        @Bean
        SessionAuthenticationStrategy sessionAuthenticationStrategy() {
            return new ChangeSessionIdAuthenticationStrategy();
        }
    }
}
