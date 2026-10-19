package com.lavyoung.marketforge.app.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.app.config.WebConfig;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 验证运营后台安全过滤链的路径边界、认证规则和 CSRF 行为。
 *
 * <p>测试使用最小 Spring MVC 上下文，不加载数据库、Redis、MQ 和搜索组件，
 * 只验证 {@link AdminSecurityConfiguration} 声明的 HTTP 安全契约。</p>
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
        AdminSecurityConfigurationTest.TestWebConfiguration.class
})
class AdminSecurityConfigurationTest {

    private static final String ADMIN_AUTH_PATH = WebConfig.API_PATH_PREFIX + "/admin/auth";
    private static final String ADMIN_ACTIVITY_ORDER_PATH = WebConfig.API_PATH_PREFIX + "/admin/activity/orders";

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    /**
     * 使用真实 Spring Security FilterChainProxy 创建 MockMvc。
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
    }

    /**
     * Given 匿名运营客户端，When 获取 CSRF 令牌，Then 请求被安全过滤链允许。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldAllowAnonymousCsrfTokenRequest() throws Exception {
        mockMvc.perform(get(ADMIN_AUTH_PATH + "/csrf")).andExpect(status().isOk());
    }

    /**
     * Given 未携带 CSRF 令牌的匿名客户端，When 提交登录，Then 安全过滤链拒绝请求。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(ADMIN_AUTH_PATH + "/login"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(CommonResponseCode.CSRF_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(CommonResponseCode.CSRF_TOKEN_INVALID.getMsg()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * Given 携带有效 CSRF 令牌的匿名客户端，When 提交登录，Then 请求可以到达登录控制器。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldAllowLoginWithCsrfToken() throws Exception {
        mockMvc.perform(post(ADMIN_AUTH_PATH + "/login").with(csrf())).andExpect(status().isOk());
    }

    /**
     * Given 未登录客户端，When 查询受保护的运营资源，Then 请求被拒绝且不跳转 HTML 登录页。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectProtectedAdminRequestWithoutLogin() throws Exception {
        mockMvc.perform(get(ADMIN_ACTIVITY_ORDER_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(CommonResponseCode.AUTHENTICATION_REQUIRED.getCode()))
                .andExpect(jsonPath("$.message").value(CommonResponseCode.AUTHENTICATION_REQUIRED.getMsg()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * Given 已认证运营用户，When 查询受保护资源，Then 请求通过认证校验。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldAllowProtectedAdminRequestForAuthenticatedUser() throws Exception {
        mockMvc.perform(get(ADMIN_ACTIVITY_ORDER_PATH).with(user("admin"))).andExpect(status().isOk());
    }

    /**
     * Given 非运营后台写接口，When 未携带 CSRF 令牌提交请求，Then 不受 Admin 安全链影响。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldNotApplyAdminCsrfRulesToPublicPostRequest() throws Exception {
        mockMvc.perform(post("/public/action")).andExpect(status().isOk());
    }

    /**
     * 安全过滤链测试所需的最小 MVC 配置。
     */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    static class TestWebConfiguration {

        /**
         * 创建安全异常响应序列化使用的 JSON 映射器。
         *
         * @return 测试上下文使用的 JSON 映射器
         */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /**
         * 创建与生产配置一致的 HttpSession 安全上下文仓储。
         *
         * @return 测试过滤链使用的安全上下文仓储
         */
        @Bean
        SecurityContextRepository securityContextRepository() {
            return new HttpSessionSecurityContextRepository();
        }

        /**
         * 创建测试控制器。
         *
         * @return 提供公开和受保护测试端点的控制器
         */
        @Bean
        TestEndpointController testEndpointController() {
            return new TestEndpointController();
        }
    }

    /**
     * 提供安全过滤链行为测试使用的最小 HTTP 端点。
     */
    @RestController
    static class TestEndpointController {

        @GetMapping(WebConfig.API_PATH_PREFIX + "/admin/auth/csrf")
        String csrf() {
            return "csrf";
        }

        @PostMapping(WebConfig.API_PATH_PREFIX + "/admin/auth/login")
        String login() {
            return "login";
        }

        @GetMapping(WebConfig.API_PATH_PREFIX + "/admin/activity/orders")
        String adminOrders() {
            return "orders";
        }

        @PostMapping("/public/action")
        String publicAction() {
            return "public";
        }
    }
}
