package com.lavyoung.marketforge.app.config.security;

import com.lavyoung.marketforge.types.model.CommonResponseCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 处理运营后台权限不足和 CSRF 校验失败。
 *
 * <p>普通授权失败使用 {@link CommonResponseCode#ACCESS_DENIED}；
 * CSRF 令牌缺失、过期或不匹配使用
 * {@link CommonResponseCode#CSRF_TOKEN_INVALID}，便于前端重新获取令牌。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
@AllArgsConstructor
public class AdminAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityResponseWriter responseWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException, ServletException {
        CommonResponseCode responseCode = exception instanceof CsrfException ? CommonResponseCode.CSRF_TOKEN_INVALID : CommonResponseCode.ACCESS_DENIED;
        responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, responseCode);
    }
}
