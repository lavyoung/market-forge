package com.lavyoung.marketforge.types.exception;

/**
 * 表示可预期的业务异常。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public class BusinessException extends RuntimeException {

    /** 业务错误码。 */
    private final String code;

    /**
     * 创建业务异常。
     *
     * @param code 业务错误码
     * @param message 可向调用方说明的错误信息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码
     */
    public String getCode() {
        return code;
    }

}
