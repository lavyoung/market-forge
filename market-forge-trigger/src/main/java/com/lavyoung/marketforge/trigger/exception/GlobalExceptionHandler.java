package com.lavyoung.marketforge.trigger.exception;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将应用异常转换为统一 HTTP 响应。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 将业务异常转换为统一失败响应。
     *
     * @param exception 业务异常
     * @return 包含业务错误码和错误信息的失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Response<Void> handleBusinessException(
            BusinessException exception) {

        return Response.fail(
                exception.getResponseCode(),
                exception.getMessage()
        );
    }

    /**
     * 处理请求体字段校验失败。
     *
     * @param exception 方法参数校验异常
     * @return 参数校验失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {

        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Parameter validation failed");

        return Response.fail(
                CommonResponseCode.PARAM_INVALID,
                message
        );
    }

    /**
     * 处理路径参数或查询参数约束校验失败。
     *
     * @param exception 约束校验异常
     * @return 参数约束校验失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Response<Void> handleConstraintViolationException(
            ConstraintViolationException exception) {

        return Response.fail(
                CommonResponseCode.PARAM_INVALID,
                exception.getMessage()
        );
    }

    /**
     * 处理请求体缺失或无法反序列化的情况。
     *
     * @param exception 请求体读取异常
     * @return 请求体格式错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {

        return Response.fail(
                CommonResponseCode.REQUEST_BODY_INVALID,
                "Request body is invalid"
        );
    }

    /**
     * 处理未被其他处理器捕获的系统异常。
     *
     * @param exception 未知系统异常
     * @return 隐藏内部细节的系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(
            Exception exception) {

        return Response.fail(
                CommonResponseCode.SYSTEM_ERROR,
                "System error"
        );
    }

}
