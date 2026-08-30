package com.lavyoung.marketforge.trigger.exception;

import com.lavyoung.marketforge.types.exception.BusinessException;
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
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Response<Void> handleBusinessException(
            BusinessException exception) {

        return Response.fail(
                exception.getCode(),
                exception.getMessage()
        );
    }

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
                "PARAM_001",
                message
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Response<Void> handleConstraintViolationException(
            ConstraintViolationException exception) {

        return Response.fail(
                "PARAM_002",
                exception.getMessage()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {

        return Response.fail(
                "PARAM_003",
                "Request body is invalid"
        );
    }

    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(
            Exception exception) {

        return Response.fail(
                "SYSTEM_001",
                "System error"
        );
    }

}
