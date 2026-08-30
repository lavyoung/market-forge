package com.lavyoung.marketforge.types.model;

/**
 * 统一接口响应载体。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public record Response<T>(
        String code,
        String message,
        T data
) {

    public static <T> Response<T> success(T data) {
        return new Response<>("0000", "success", data);
    }

    public static <T> Response<T> success() {
        return new Response<>("0000", "success", null);
    }

    public static <T> Response<T> fail(String code, String message) {
        return new Response<>(code, message, null);
    }

}
