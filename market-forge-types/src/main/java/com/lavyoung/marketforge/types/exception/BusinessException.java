package com.lavyoung.marketforge.types.exception;

import com.lavyoung.marketforge.types.model.IResponseCode;
import lombok.Getter;

import java.util.Objects;

/**
 * 表示可预期的业务异常。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务异常对应的统一响应编码定义。
     */
    private final IResponseCode responseCode;

    /**
     * 根据统一响应编码创建业务异常。
     *
     * @param responseCode 业务响应编码
     * @throws NullPointerException 响应编码为空时抛出
     */
    public BusinessException(IResponseCode responseCode) {
        this(responseCode, requireResponseCode(responseCode).getMsg());
    }

    /**
     * 根据统一响应编码和自定义文案创建业务异常。
     *
     * @param responseCode 业务响应编码
     * @param message      当前业务场景的异常文案
     * @throws NullPointerException 响应编码为空时抛出
     */
    public BusinessException(IResponseCode responseCode, String message) {
        super(message);
        this.responseCode = requireResponseCode(responseCode);
    }

    /**
     * 根据统一响应编码和底层原因创建业务异常。
     *
     * @param responseCode 业务响应编码
     * @param cause        导致当前业务异常的底层原因
     * @throws NullPointerException 响应编码为空时抛出
     */
    public BusinessException(IResponseCode responseCode, Throwable cause) {
        super(requireResponseCode(responseCode).getMsg(), cause);
        this.responseCode = responseCode;
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码
     */
    public int getCode() {
        return responseCode.getCode();
    }

    /**
     * 获取国际化资源键。
     *
     * @return 国际化资源键
     */
    public String getI18nKey() {
        return responseCode.getI18nKey();
    }

    /**
     * 校验并返回响应编码。
     *
     * @param responseCode 待校验的响应编码
     * @return 非空响应编码
     * @throws NullPointerException 响应编码为空时抛出
     */
    private static IResponseCode requireResponseCode(IResponseCode responseCode) {
        return Objects.requireNonNull(responseCode, "responseCode must not be null");
    }

}
