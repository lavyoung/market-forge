package com.lavyoung.marketforge.types.model;

/**
 * 统一接口响应载体。
 *
 * @param code 响应码
 * @param message 响应说明
 * @param data 响应数据
 * @param <T> 响应数据类型
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public record Response<T>(
        String code,
        String message,
        T data
) {

    /**
     * 创建包含业务数据的成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Response<T> success(T data) {
        return new Response<>("0000", "success", data);
    }

    /**
     * 创建不包含业务数据的成功响应。
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Response<T> success() {
        return new Response<>("0000", "success", null);
    }

    /**
     * 创建失败响应。
     *
     * @param code 错误码
     * @param message 错误说明
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Response<T> fail(String code, String message) {
        return new Response<>(code, message, null);
    }

}
