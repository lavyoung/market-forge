package com.lavyoung.marketforge.app.config.security;

import com.lavyoung.marketforge.trigger.security.AdminOperatorDetails;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运营后台身份认证核心组件配置。
 *
 * <p>该配置将外部化运营账号转换为 Spring Security 可以识别的
 * {@link UserDetails}，并提供密码编码器和认证管理器。</p>
 *
 * <p>MVP 阶段通过外部化配置加载单个运营账号。这里的账号来源与登录会话
 * 存储彼此独立；认证成功后的 HttpSession 仍由 Spring Session 保存到 Redis。</p>
 *
 * <p>后续接入数据库账号体系时，只需替换 {@link UserDetailsService} 实现，
 * HTTP 登录协议和认证管理器无需改变。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminAccountProperties.class)
public class AdminAuthenticationConfiguration {

    /**
     * 创建 BCrypt 密码编码器。
     *
     * <p>强度参数 {@code 12} 用于后续生成或升级密码哈希。校验已有哈希时，
     * BCrypt 会从哈希内容中读取其实际强度和随机盐。</p>
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder adminPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 根据外部化配置创建运营账号查询服务。
     *
     * <p>每次查询都创建新的 {@link AdminOperatorDetails}，确保认证结果保留
     * 显示名称和权限等后台身份信息。传入的 {@code passwordHash} 已经是
     * BCrypt 摘要，不能再次编码。</p>
     *
     * <p>未知用户名统一抛出 {@link UsernameNotFoundException}，最终由
     * Spring Security 转换为通用凭证错误，避免向调用方暴露账号是否存在。</p>
     *
     * @param properties 运营后台账号配置
     * @return 根据用户名加载后台操作员身份的服务
     * @throws UsernameNotFoundException 查询的后台用户名不存在时抛出
     */
    @Bean
    public UserDetailsService adminUserDetailsService(AdminAccountProperties properties) {
        Set<GrantedAuthority> authorities = properties.authorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());

        return username -> {
            // 未知用户名统一抛出
            if (!properties.username().equals(username)) {
                throw new UsernameNotFoundException("Admin account not found");
            }
            return new AdminOperatorDetails(
                    properties.username(),
                    properties.passwordHash(),
                    properties.displayName(),
                    authorities
            );
        };
    }

    /**
     * 暴露 Spring Security 构建完成的认证管理器。
     *
     * <p>后续登录 Controller 使用该管理器校验用户名和密码。认证规则仍由
     * Spring Security 的认证提供者执行，Controller 不应自行比较密码哈希。</p>
     *
     * @param authenticationConfiguration Spring Security 认证装配入口
     * @return 已装配用户查询服务和密码编码器的认证管理器
     * @throws Exception Spring Security 无法构建认证管理器时抛出
     */
    @Bean
    public AuthenticationManager adminAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 创建运营后台安全上下文仓储。
     *
     * <p>登录 Controller 使用该仓储把认证结果保存到 HttpSession；
     * 后续请求的安全过滤链再从同一个仓储恢复认证身份。</p>
     *
     * @return 基于 HttpSession 的安全上下文仓储
     */
    @Bean
    public SecurityContextRepository adminSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * 创建登录成功后的 Session 固定攻击防护策略。
     *
     * <p>认证成功后更换 Session ID，同时保留登录前创建的 CSRF Token 等
     * Session 属性，防止攻击者预先固定受害者的 Session ID。</p>
     *
     * @return 更换 Session ID 的认证会话策略
     */
    @Bean
    public SessionAuthenticationStrategy adminSessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }
}
