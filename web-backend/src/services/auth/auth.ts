import {request} from '@umijs/max';
import type {ApiResponse} from '../types';
import {requireResponseData} from '../types';
import type {AdminCsrfToken, AdminLoginRequest, AdminOperator} from './types';

const ADMIN_AUTH_PATH = '/api/admin/auth';

let currentCsrfToken: AdminCsrfToken | undefined;

/**
 * 获取当前浏览器会话对应的 CSRF Token。
 *
 * Token 只保存在运行时内存，不写入 localStorage。页面刷新后重新向
 * 服务端获取，避免长期保存已经失效的 Session Token。
 *
 * @param forceRefresh 是否强制重新获取
 * @returns 当前 Session 对应的 CSRF Token
 */
export async function getAdminCsrfToken(forceRefresh = false): Promise<AdminCsrfToken> {
    if (!forceRefresh && currentCsrfToken) {
        return currentCsrfToken;
    }

    const response = await request<ApiResponse<AdminCsrfToken>>(`${ADMIN_AUTH_PATH}/csrf`, {
        method: 'GET',
        withCredentials: true,
    });

    currentCsrfToken = requireResponseData(response);
    return currentCsrfToken;
}

/**
 * 使用运营账号登录并建立服务端 Session。
 *
 * @param credentials 登录名和密码
 * @returns 登录成功的运营人员
 */
export async function loginAdminOperator(credentials: AdminLoginRequest): Promise<AdminOperator> {
    const csrfToken = await getAdminCsrfToken(true);
    const response = await request<ApiResponse<AdminOperator>>(`${ADMIN_AUTH_PATH}/login`, {
        method: 'POST',
        data: credentials,
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
        withCredentials: true,
    });

    return requireResponseData(response);
}

/**
 * 查询当前服务端 Session 中的运营人员。
 *
 * @returns 当前登录运营人员
 */
export async function getCurrentAdminOperator(): Promise<AdminOperator> {
    const response = await request<ApiResponse<AdminOperator>>(`${ADMIN_AUTH_PATH}/me`, {
        method: 'GET',
        withCredentials: true,
    });

    return requireResponseData(response);
}

/**
 * 注销当前运营后台会话。
 *
 * 注销后清理前端缓存的 CSRF Token，因为原 Token 所属 Session
 * 已经被服务端销毁。
 */
export async function logoutAdminOperator(): Promise<void> {
    const csrfToken = await getAdminCsrfToken();

    try {
        await request<ApiResponse<null>>(`${ADMIN_AUTH_PATH}/logout`, {
            method: 'POST',
            headers: {
                [csrfToken.headerName]: csrfToken.token,
            },
            withCredentials: true,
        });
    } finally {
        currentCsrfToken = undefined;
    }
}
