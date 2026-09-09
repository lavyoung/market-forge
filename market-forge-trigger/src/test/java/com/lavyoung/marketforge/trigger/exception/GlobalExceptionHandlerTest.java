package com.lavyoung.marketforge.trigger.exception;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}
