package com.lavyoung.marketforge.app.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.types.model.IResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 将 Spring Security 异常写为项目统一 JSON 响应。
 *
 * <p>安全异常发生在 MVC Controller 之前，无法交给全局异常处理器转换。
 * 该组件统一处理响应状态、编码和 JSON 序列化，避免认证与授权处理器分别
 * 维护不同的响应格式。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
@AllArgsConstructor
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * 写出统一安全失败响应。
     *
     * @param servletResponse Servlet HTTP 响应
     * @param httpStatus      HTTP 状态码
     * @param responseCode    项目统一响应编码
     * @throws IOException JSON 无法写入响应流时抛出
     */
    public void write(HttpServletResponse servletResponse, int httpStatus, IResponseCode responseCode) throws IOException {
        servletResponse.setStatus(httpStatus);
        servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(servletResponse.getOutputStream(), Response.fail(responseCode));
    }

}
