package com.lavyoung.marketforge.trigger.exception;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.concurrent.RejectedExecutionException;

/**
 * 将应用异常转换为统一 HTTP 响应。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 将业务异常转换为统一失败响应。
     *
     * @param exception 业务异常
     * @return 包含业务错误码和错误信息的失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response<Void>> handleBusinessException(
            BusinessException exception) {

        return ResponseEntity.unprocessableEntity().body(Response.fail(
                exception.getResponseCode(), exception.getMessage()));
    }

    /**
     * 处理请求体字段校验失败。
     *
     * @param exception 方法参数校验异常
     * @return 参数校验失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {

        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Parameter validation failed");

        return ResponseEntity.badRequest().body(Response.fail(CommonResponseCode.PARAM_INVALID, message));
    }

    /**
     * 处理路径参数或查询参数约束校验失败。
     *
     * @param exception 约束校验异常
     * @return 参数约束校验失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {

        return ResponseEntity.badRequest().body(Response.fail(
                CommonResponseCode.PARAM_INVALID, exception.getMessage()));
    }

    /**
     * 处理 Spring MVC 方法参数或返回值约束校验失败。
     *
     * @param exception 方法级校验异常
     * @return HTTP 400 参数校验失败响应
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Response<Void>> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception) {
        return ResponseEntity.badRequest().body(Response.fail(
                CommonResponseCode.PARAM_INVALID, "Parameter validation failed"));
    }

    /**
     * 处理请求体缺失或无法反序列化的情况。
     *
     * @param exception 请求体读取异常
     * @return 请求体格式错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {

        return ResponseEntity.badRequest().body(Response.fail(
                CommonResponseCode.REQUEST_BODY_INVALID, "Request body is invalid"));
    }

    /**
     * 处理异步执行器容量耗尽产生的任务拒绝异常。
     *
     * <p>线程池拒绝任务表示服务端当前没有足够处理容量，属于暂时性过载，
     * 因此返回 HTTP 503。响应同时携带 {@code Retry-After}，提示调用方至少
     * 等待一秒后再重试，避免立即重放请求进一步加剧队列压力。</p>
     *
     * <p>该异常属于预期的容量保护结果，只记录简洁警告日志，不输出完整异常
     * 堆栈，避免高负载期间大量堆栈日志继续消耗 CPU 和磁盘资源。</p>
     *
     * @param exception 线程池拒绝执行异常
     * @return HTTP 503、重试提示头和统一服务不可用响应
     */
    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<Response<Void>> handleRejectedExecutionException(RejectedExecutionException exception) {
        log.warn("异步任务执行器拒绝任务 message={}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(Response.fail(CommonResponseCode.SERVICE_UNAVAILABLE));
    }

    /**
     * 处理未被其他处理器捕获的系统异常。
     *
     * @param exception 未知系统异常
     * @return 隐藏内部细节的系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(
            Exception exception) {

        log.error("未处理的系统异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.fail(
                CommonResponseCode.SYSTEM_ERROR, "System error"));
    }

    /**
     * 处理请求路径未匹配到控制器或静态资源的情况。
     *
     * <p>Spring MVC 在启用静态资源处理后，会将没有匹配到控制器的请求继续交给
     * 静态资源处理器。静态资源同样不存在时，将抛出
     * {@link NoResourceFoundException}。该异常属于客户端请求错误，不应返回
     * HTTP 500。</p>
     *
     * @param exception 资源不存在异常
     * @return HTTP 404 和统一资源不存在响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Response<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.fail(
                CommonResponseCode.RESOURCE_NOT_FOUND,
                CommonResponseCode.RESOURCE_NOT_FOUND.getMsg()
        ));
    }

    /**
     * 将身份认证失败转换为统一的 HTTP 401 响应。
     *
     * <p>无论用户名不存在、密码错误还是账号状态异常，都返回相同错误信息，
     * 防止调用方根据响应差异枚举后台账号。</p>
     *
     * @param exception Spring Security 认证异常
     * @return 不暴露认证失败细节的统一响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Response<Void>> handleAuthenticationException(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Response.fail(CommonResponseCode.AUTHENTICATION_FAILED));
    }

}
