/**
 * 运营后台初始化状态。
 */
export interface InitialState {
    /** 当前登录的运营人员名称。 */
    operatorName: string;
}

/**
 * 初始化运营后台运行时数据。
 *
 * 当前章节尚未接入登录系统，因此先返回固定运营人员。
 * 后续接入认证接口时，只需要替换该方法的数据来源。
 *
 * @returns 运营后台初始化状态
 */
export async function getInitialState(): Promise<InitialState> {
    return {
        operatorName: '运营管理员',
    };
}

/**
 * 配置运营后台整体布局。
 *
 * @returns ProLayout 运行时配置
 */
export const layout = () => {
    return {
        logo: false,
        menu: {
            locale: false,
        },
    };
};
