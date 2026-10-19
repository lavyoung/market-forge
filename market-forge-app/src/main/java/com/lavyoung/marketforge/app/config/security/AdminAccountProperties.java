package com.lavyoung.marketforge.app.config.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * 运营后台单账号认证配置。
 *
 * <p>MVP 阶段通过外部化配置提供一个运营账号。该类型只描述账号信息，
 * 不负责密码校验、身份认证或 Session 创建。后续接入数据库账号体系时，
 * 可以替换账号查询实现而不改变 HTTP 登录协议。</p>
 *
 * <p>{@code passwordHash} 必须是 BCrypt 哈希值，禁止配置明文密码。
 * {@link #toString()} 不输出哈希内容，避免配置对象进入日志时泄露凭证信息。</p>
 *
 * @param username     运营人员登录名
 * @param passwordHash BCrypt 密码哈希
 * @param displayName  运营人员展示名称
 * @param authorities  运营人员权限标识集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Validated
@ConfigurationProperties(prefix = "market-forge.security.admin")
public record AdminAccountProperties(
        @NotBlank(message = "运营后台登录名不能为空")
        @Size(max = 64, message = "运营后台登录名长度不能超过 64 个字符")
        String username,

        @NotBlank(message = "运营后台密码哈希不能为空")
        @Pattern(regexp = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$", message = "运营后台密码必须使用 BCrypt 哈希")
        String passwordHash,

        @NotBlank(message = "运营人员展示名称不能为空")
        @Size(max = 64, message = "运营人员展示名称不能超过 64 个字符")
        String displayName,

        @NotEmpty(message = "运营人员至少需要一个权限")
        Set<@NotBlank(message = "运营权限标识不能为空") String> authorities
) {

    /**
     * 创建运营账号配置并将权限集合转换为不可变集合。
     *
     * @throws NullPointerException 权限集合包含空元素时抛出
     */
    public AdminAccountProperties {
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }

    /**
     * 返回隐藏密码哈希后的配置描述。
     *
     * @return 不包含密码哈希的账号配置文本
     */
    @Override
    public String toString() {
        return "AdminAccountProperties[username=%s, passwordHash=******, displayName=%s, authorities=%s]"
                .formatted(username, displayName, authorities);
    }
}
