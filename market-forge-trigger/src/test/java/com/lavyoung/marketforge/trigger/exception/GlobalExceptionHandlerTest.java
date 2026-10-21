package com.lavyoung.marketforge.trigger.exception;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证全局异常处理器同时保留业务错误码与正确 HTTP 状态语义。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * Given 可预期业务异常，When 转换为 HTTP 响应，Then 返回 422 和原业务错误码。
     */
    @Test
    void shouldMapBusinessExceptionToUnprocessableEntity() {
        // When
        ResponseEntity<Response<Void>> result = handler.handleBusinessException(
                new BusinessException(BusinessResponseCode.STRATEGY_NOT_FOUND));

        // Then
        assertAll(
                () -> assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode()),
                () -> assertEquals(BusinessResponseCode.STRATEGY_NOT_FOUND.getCode(), result.getBody().code())
        );
    }

    /**
     * Given 参数约束异常，When 转换为 HTTP 响应，Then 返回 400 和统一参数错误码。
     */
    @Test
    void shouldMapConstraintViolationToBadRequest() {
        // When
        ResponseEntity<Response<Void>> result = handler.handleConstraintViolationException(
                new ConstraintViolationException("invalid parameter", java.util.Set.of()));

        // Then
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode()),
                () -> assertEquals(CommonResponseCode.PARAM_INVALID.getCode(), result.getBody().code())
        );
    }

    /**
     * Given 未知系统异常，When 转换为 HTTP 响应，Then 返回 500 且不暴露异常细节。
     */
    @Test
    void shouldHideInternalExceptionDetails() {
        // When
        ResponseEntity<Response<Void>> result = handler.handleException(new IllegalStateException("secret"));

        // Then
        assertAll(
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode()),
                () -> assertEquals(CommonResponseCode.SYSTEM_ERROR.getCode(), result.getBody().code()),
                () -> assertEquals("System error", result.getBody().message())
        );
    }

    /**
     * Given 异步执行器拒绝任务，When 转换为 HTTP 响应，Then 返回 503、统一容量错误码和重试提示。
     */
    @Test
    void shouldMapRejectedExecutionToServiceUnavailable() {
        // Given
        RejectedExecutionException exception = new RejectedExecutionException("strategy-refresh queue full");

        // When
        ResponseEntity<Response<Void>> result = handler.handleRejectedExecutionException(exception);

        // Then
        assertAll(
                () -> assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode()),
                () -> assertEquals(CommonResponseCode.SERVICE_UNAVAILABLE.getCode(), result.getBody().code()),
                () -> assertEquals(CommonResponseCode.SERVICE_UNAVAILABLE.getMsg(), result.getBody().message()),
                () -> assertEquals("1", result.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
        );
    }

    /**
     * Given 请求路径没有匹配到控制器或静态资源，When 转换为 HTTP 响应，
     * Then 返回 404 和统一资源不存在错误码，而不是将客户端错误误报为系统异常。
     */
    @Test
    void shouldMapMissingResourceToNotFound() {
        // Given
        NoResourceFoundException exception = new NoResourceFoundException(
                HttpMethod.GET,
                "/award/records"
        );

        // When
        ResponseEntity<Response<Void>> result = handler.handleNoResourceFoundException(exception);

        // Then
        assertAll(
                () -> assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode()),
                () -> assertEquals(CommonResponseCode.RESOURCE_NOT_FOUND.getCode(), result.getBody().code()),
                () -> assertEquals(CommonResponseCode.RESOURCE_NOT_FOUND.getMsg(), result.getBody().message())
        );
    }
}
