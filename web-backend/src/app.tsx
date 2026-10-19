import HeaderAccount from '@/components/HeaderAccount';
import {getCurrentAdminOperator} from '@/services/auth/auth';
import type {AdminOperator} from '@/services/auth/types';
import type {RequestConfig, RunTimeLayoutConfig} from '@umijs/max';
import {history} from '@umijs/max';

/**
 * 运营后台初始化状态。
 */
export interface InitialState {
    /** 当前登录的运营人员；为空表示尚未登录。 */
    currentOperator?: AdminOperator;
}

/**
 * 初始化运营后台登录状态。
 *
 * 页面刷新时通过服务端 Session 恢复用户身份，不能仅依靠前端缓存判断登录状态。
 *
 * @returns 运营后台初始化状态
 */
export async function getInitialState(): Promise<InitialState> {
    try {
        const currentOperator = await getCurrentAdminOperator();
        return {currentOperator};
    } catch (error) {
        if (isUnauthorized(error)) {
            return {};
        }

        throw error;
    }
}

/**
 * 运营后台统一请求配置。
 */
export const request: RequestConfig = {
    timeout: 10000,
    withCredentials: true,
    responseInterceptors: [
        [
            response => response,
            error => {
                if (isUnauthorized(error) && history.location.pathname !== '/login') {
                    const location = history.location;
                    const redirect = encodeURIComponent(`${location.pathname}${location.search}`);
                    history.replace(`/login?redirect=${redirect}`);
                }

                return Promise.reject(error);
            },
        ],
    ],
};

/**
 * 配置运营后台布局和登录路由守卫。
 */
export const layout: RunTimeLayoutConfig = ({initialState}) => ({
    logo: false,
    menu: {
        locale: false,
    },
    actionsRender: () => [<HeaderAccount key="header-account"/>],
    onPageChange: () => {
        const {location} = history;

        if (!initialState?.currentOperator && location.pathname !== '/login') {
            const redirect = encodeURIComponent(`${location.pathname}${location.search}`);
            history.replace(`/login?redirect=${redirect}`);
        }
    },
});

/**
 * 判断请求异常是否为服务端返回的 HTTP 401。
 *
 * @param error 请求异常
 * @returns 是否需要重新登录
 */
function isUnauthorized(error: unknown): boolean {
    if (typeof error !== 'object' || error === null || !('response' in error)) {
        return false;
    }

    const response = (error as { response?: { status?: number } }).response;
    return response?.status === 401;
}
