import HeaderAccount from '@/components/HeaderAccount';
import {getCurrentAdminOperator} from '@/services/auth/auth';
import type {AdminOperator} from '@/services/auth/types';
import {DeploymentUnitOutlined} from '@ant-design/icons';
import type {RequestConfig, RunTimeLayoutConfig} from '@umijs/max';
import {history} from '@umijs/max';
import {ConfigProvider} from 'antd';
import type {ReactNode} from 'react';

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
 * 为运营后台提供统一的 Ant Design 主题。
 *
 * @param container Umi 应用根节点
 * @returns 应用统一主题后的根节点
 */
export function rootContainer(container: ReactNode): ReactNode {
    return (
        <ConfigProvider
            theme={{
                token: {
                    colorPrimary: '#1677ff',
                    colorInfo: '#1677ff',
                    colorSuccess: '#16a36a',
                    colorWarning: '#d99000',
                    colorError: '#d83b36',
                    colorText: '#1d1d1f',
                    colorTextSecondary: '#6e6e73',
                    colorBorder: '#e1e4e8',
                    colorBgLayout: '#f5f7fa',
                    borderRadius: 6,
                    controlHeight: 40,
                    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
                },
                components: {
                    Menu: {
                        itemBorderRadius: 6,
                        itemSelectedBg: '#eaf3ff',
                        itemSelectedColor: '#1677ff',
                        itemHoverBg: '#f3f6f9',
                    },
                    Table: {
                        headerBg: '#f7f8fa',
                        headerColor: '#343438',
                        rowHoverBg: '#f4f8ff',
                        borderColor: '#ebecef',
                    },
                },
            }}
        >
            {container}
        </ConfigProvider>
    );
}

/**
 * 配置运营后台布局和登录路由守卫。
 */
export const layout: RunTimeLayoutConfig = ({initialState}) => ({
    logo: <span className="app-brand-mark"><DeploymentUnitOutlined/></span>,
    title: 'Market Forge',
    navTheme: 'light',
    fixedHeader: true,
    fixSiderbar: true,
    siderWidth: 224,
    colorPrimary: '#1677ff',
    contentStyle: {backgroundColor: '#f5f7fa'},
    token: {
        bgLayout: '#f5f7fa',
        header: {
            colorBgHeader: 'rgba(255, 255, 255, 0.88)',
            colorBgScrollHeader: 'rgba(255, 255, 255, 0.94)',
            colorHeaderTitle: '#1d1d1f',
            colorBgRightActionsItemHover: '#f3f6f9',
            colorTextRightActionsItem: '#1d1d1f',
            heightLayoutHeader: 60,
        },
        sider: {
            colorMenuBackground: '#ffffff',
            colorBgMenuItemHover: '#f3f6f9',
            colorBgMenuItemSelected: '#eaf3ff',
            colorTextMenuSelected: '#1677ff',
            colorTextMenuItemHover: '#1d1d1f',
            colorTextMenu: '#515154',
            colorTextMenuSecondary: '#86868b',
        },
        pageContainer: {
            colorBgPageContainer: '#f5f7fa',
            colorBgPageContainerFixed: 'rgba(245, 247, 250, 0.92)',
            paddingInlinePageContainerContent: 24,
            paddingBlockPageContainerContent: 20,
        },
    },
    menu: {
        locale: false,
        defaultOpenAll: true,
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
