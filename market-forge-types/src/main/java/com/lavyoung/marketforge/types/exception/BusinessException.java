package com.lavyoung.marketforge.types.exception;

import lombok.Getter;

/**
 * 表示可预期的业务异常。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码。
     * -- GETTER --
     * 获取业务错误码。
     *
     */
    private final String code;

    /**
     * 创建业务异常。
     *
     * @param code    业务错误码
     * @param message 可向调用方说明的错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = String.valueOf(code);
    }

}
