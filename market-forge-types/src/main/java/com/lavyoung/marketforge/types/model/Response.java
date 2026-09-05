package com.lavyoung.marketforge.types.model;

/**
 * 统一接口响应载体。
 *
 * @param code    响应码
 * @param message 响应说明
 * @param data    响应数据
 * @param <T>     响应数据类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
public record Response<T>(
        int code,
        String message,
        T data
) {

    /**
     * 创建包含业务数据的成功响应。
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> Response<T> success(T data) {
        return of(CommonResponseCode.SUCCESS, data);
    }

    /**
     * 创建不包含业务数据的成功响应。
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Response<T> success() {
        return of(CommonResponseCode.SUCCESS, null);
    }

    /**
     * 根据指定响应编码创建包含业务数据的成功响应。
     *
     * @param responseCode 响应编码
     * @param data         响应数据
     * @param <T>          响应数据类型
     * @return 成功响应
     */
    public static <T> Response<T> success(IResponseCode responseCode, T data) {
        return of(responseCode, data);
    }

    /**
     * 根据统一响应编码创建失败响应。
     *
     * @param responseCode 失败响应编码
     * @return 失败响应
     */
    public static Response<Void> fail(IResponseCode responseCode) {
        return of(responseCode, null);
    }

    /**
     * 根据统一响应编码和解析后的文案创建失败响应。
     *
     * @param responseCode 失败响应编码
     * @param message      国际化或场景化处理后的响应文案
     * @return 失败响应
     */
    public static Response<Void> fail(IResponseCode responseCode, String message) {
        return new Response<>(responseCode.getCode(), message, null);
    }

    /**
     * 根据统一响应编码创建响应。
     *
     * @param responseCode 响应编码
     * @param data         响应数据
     * @param <T>          响应数据类型
     * @return 统一响应
     */
    private static <T> Response<T> of(IResponseCode responseCode, T data) {
        return new Response<>(
                responseCode.getCode(),
                responseCode.getMsg(),
                data
        );
    }

}
