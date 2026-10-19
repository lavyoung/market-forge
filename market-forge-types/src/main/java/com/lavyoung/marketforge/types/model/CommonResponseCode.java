package com.lavyoung.marketforge.types.model;

/**
 * 可供不同项目共同使用的基础响应编码。
 * <p>
 * 这里只定义与具体业务领域无关的结果；各项目应通过实现 {@link IResponseCode}
 * 定义自己的业务错误码。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
public enum CommonResponseCode implements IResponseCode {

    /**
     * 请求处理成功。
     */
    SUCCESS(0, "common.success", "成功"),

    /**
     * 请求参数不符合接口约束。
     */
    PARAM_INVALID(1_001, "common.param.invalid", "请求参数无效"),

    /**
     * 请求缺少必要参数。
     */
    PARAM_MISSING(1_002, "common.param.missing", "缺少必要的请求参数"),

    /**
     * 请求体缺失或无法解析。
     */
    REQUEST_BODY_INVALID(1_003, "common.request.body.invalid", "请求体格式无效"),

    /**
     * 请求路径没有匹配到控制器或静态资源。
     */
    RESOURCE_NOT_FOUND(1_004, "common.resource.not-found", "请求的资源不存在"),

    /**
     * 当前请求没有有效的登录身份。
     *
     * <p>用户未登录和服务端会话已经失效都使用该编码，避免前端为两种
     * 本质相同的状态维护不同处理分支。</p>
     */
    AUTHENTICATION_REQUIRED(1_005, "common.authentication.required", "请先登录"),

    /**
     * 登录凭证校验失败。
     *
     * <p>不区分用户名不存在和密码错误，防止攻击者根据错误信息枚举
     * 系统中已经存在的运营账号。</p>
     */
    AUTHENTICATION_FAILED(1_006, "common.authentication.failed", "用户名或密码错误"),

    /**
     * 当前登录用户没有访问目标资源所需的权限。
     */
    ACCESS_DENIED(1_007, "common.access.denied", "无权访问该资源"),

    /**
     * 请求携带的 CSRF 令牌缺失、失效或与当前会话不匹配。
     */
    CSRF_TOKEN_INVALID(1_008, "common.csrf-token.invalid", "请求安全校验失败"),

    /**
     * 未被业务规则识别的系统异常。
     */
    SYSTEM_ERROR(900_000_001, "system.error", "系统异常，请稍后重试");

    private final int code;
    private final String i18nKey;
    private final String msg;

    /**
     * 创建基础响应编码。
     *
     * @param code    业务编码
     * @param i18nKey 国际化资源键
     * @param msg     默认响应文案
     */
    CommonResponseCode(int code, String i18nKey, String msg) {
        this.code = code;
        this.i18nKey = i18nKey;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getI18nKey() {
        return i18nKey;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
