/**
 * 运营后台登录凭证。
 */
export interface AdminLoginRequest {
    /** 运营人员登录名。 */
    username: string;
    /** 运营人员原始密码。 */
    password: string;
}

/**
 * 当前登录运营人员。
 */
export interface AdminOperator {
    /** 登录名。 */
    username: string;
    /** 页面展示名称。 */
    displayName: string;
    /** 当前账号拥有的权限标识。 */
    authorities: string[];
}

/**
 * 服务端生成的 CSRF Token。
 */
export interface AdminCsrfToken {
    /** 写请求回传 Token 时使用的请求头。 */
    headerName: string;
    /** 表单参数名称。 */
    parameterName: string;
    /** Token 内容。 */
    token: string;
}
