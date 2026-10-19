package com.lavyoung.marketforge.api.admin.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Objects;

/**
 * 运营后台 CSRF 令牌响应。
 *
 * <p>前端在执行登录、退出等会修改服务端状态的请求前，需要先获取该令牌，
 * 再按照 {@code headerName} 指定的请求头名称传回 {@code token}。</p>
 *
 * <p>CSRF 令牌用于防止浏览器在用户不知情的情况下携带会话 Cookie 发起伪造请求，
 * 它不能替代登录认证，也不应被当作用户身份凭证。</p>
 *
 * @param headerName    客户端回传令牌时使用的 HTTP 请求头名称
 * @param parameterName 使用表单参数回传令牌时的参数名称
 * @param token         当前会话对应的 CSRF 令牌
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Schema(description = "运营后台 CSRF 令牌")
public record AdminCsrfTokenResponse(
        @Schema(description = "CSRF 请求头名称", example = "X-CSRF-TOKEN")
        String headerName,

        @Schema(description = "CSRF 表单参数名称", example = "_csrf")
        String parameterName,

        @Schema(description = "当前会话的 CSRF 令牌")
        String token
) implements Serializable {

    /**
     * 校验 CSRF 令牌响应的必要字段。
     *
     * @throws NullPointerException 任一令牌字段为空时抛出
     */
    public AdminCsrfTokenResponse {
        Objects.requireNonNull(headerName, "headerName must not be null");
        Objects.requireNonNull(parameterName, "parameterName must not be null");
        Objects.requireNonNull(token, "token must not be null");
    }
}
