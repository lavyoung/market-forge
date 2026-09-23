package com.lavyoung.marketforge.types.exception;

import com.lavyoung.marketforge.types.model.IResponseCode;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.Arrays;
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
     * 业务异常文案格式化参数。
     */
    private final Object[] args;

    /**
     * 根据统一响应编码创建业务异常。
     *
     * @param responseCode 业务响应编码
     * @throws NullPointerException 响应编码为空时抛出
     */
    public BusinessException(IResponseCode responseCode) {
        this(responseCode, requireResponseCode(responseCode).getMsg(), null, new Object[0]);
    }

    /**
     * 根据统一响应编码和自定义文案创建业务异常。
     *
     * @param responseCode 业务响应编码
     * @param message      当前业务场景的异常文案
     * @throws NullPointerException 响应编码为空时抛出
     */
    public BusinessException(IResponseCode responseCode, String message) {
        this(responseCode, message, null, new Object[0]);
    }

    /**
     * 根据统一响应编码和底层原因创建业务异常。
     *
     * @param responseCode 业务响应编码
     * @param cause        导致当前业务异常的底层原因
     * @throws NullPointerException 响应编码为空时抛出
     */
    public BusinessException(IResponseCode responseCode, Throwable cause) {
        this(responseCode, requireResponseCode(responseCode).getMsg(), cause, new Object[0]);
    }

    /**
     * 根据统一响应编码和默认文案参数创建业务异常。
     * <p>
     * 响应编码的默认文案可使用 {@link MessageFormat} 占位符，例如 {@code "活动 SKU {0} 不存在"}。
     * 当默认文案未配置占位符时，参数会作为排障上下文追加到默认文案后，避免动态信息丢失。
     *
     * @param responseCode 业务响应编码
     * @param args         默认文案格式化参数
     * @return 带格式化文案和参数快照的业务异常
     * @throws NullPointerException 响应编码为空时抛出
     */
    public static BusinessException of(IResponseCode responseCode, Object... args) {
        IResponseCode requiredResponseCode = requireResponseCode(responseCode);
        Object[] safeArgs = safeArgs(args);
        return new BusinessException(
                requiredResponseCode,
                formatMessage(requiredResponseCode.getMsg(), safeArgs),
                null,
                safeArgs
        );
    }

    /**
     * 根据统一响应编码、底层原因和默认文案参数创建业务异常。
     *
     * @param responseCode 业务响应编码
     * @param cause        导致当前业务异常的底层原因
     * @param args         默认文案格式化参数
     * @return 带格式化文案、原因和参数快照的业务异常
     * @throws NullPointerException 响应编码为空时抛出
     */
    public static BusinessException of(IResponseCode responseCode, Throwable cause, Object... args) {
        IResponseCode requiredResponseCode = requireResponseCode(responseCode);
        Object[] safeArgs = safeArgs(args);
        return new BusinessException(
                requiredResponseCode,
                formatMessage(requiredResponseCode.getMsg(), safeArgs),
                cause,
                safeArgs
        );
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
     * 获取业务异常文案格式化参数快照。
     *
     * @return 格式化参数副本
     */
    public Object[] getArgs() {
        return args.clone();
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

    /**
     * 复制格式化参数，避免外部数组修改影响异常对象。
     *
     * @param args 原始参数
     * @return 参数副本
     */
    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args.clone();
    }

    /**
     * 生成业务异常文案。
     *
     * @param message 默认文案
     * @param args    格式化参数
     * @return 格式化后的业务异常文案
     */
    private static String formatMessage(String message, Object[] args) {
        if (args.length == 0) {
            return message;
        }
        if (message.contains("{0")) {
            return MessageFormat.format(message, args);
        }
        return message + "，参数：" + Arrays.toString(args);
    }

    private BusinessException(IResponseCode responseCode, String message, Throwable cause, Object[] args) {
        super(message, cause);
        this.responseCode = requireResponseCode(responseCode);
        this.args = safeArgs(args);
    }

}
