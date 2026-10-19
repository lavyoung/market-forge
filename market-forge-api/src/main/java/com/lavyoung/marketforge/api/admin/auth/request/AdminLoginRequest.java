package com.lavyoung.marketforge.api.admin.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 运营后台登录请求。
 *
 * <p>该请求只负责承载登录凭证，不负责认证规则。用户名是否存在、密码是否匹配
 * 以及账号是否可用，由后续接入的 Spring Security 认证组件负责判断。</p>
 *
 * <p>密码属于敏感数据，不得写入日志、异常信息或持久化存储。
 * 本记录重写了 {@link #toString()}，避免日志框架输出明文密码。</p>
 *
 * @param username 运营人员登录名
 * @param password 运营人员原始登录密码
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Schema(description = "运营后台登录请求")
public record AdminLoginRequest(
        @Schema(description = "运营人员登录名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "登录名不能为空")
        @Size(max = 64, message = "登录名长度不能超过 64 个字符")
        String username,

        @Schema(description = "运营人员登录密码", accessMode = Schema.AccessMode.WRITE_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "登录密码不能为空")
        @Size(max = 128, message = "登录密码长度不能超过 128 个字符")
        String password
) implements Serializable {

    /**
     * 返回隐藏密码后的日志文本。
     *
     * @return 不包含明文密码的登录请求描述
     */
    @Override
    public String toString() {
        return "AdminLoginRequest[username=%s, password=******]".formatted(username);
    }
}
