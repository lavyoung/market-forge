import {loginAdminOperator} from '@/services/auth/auth';
import type {AdminLoginRequest} from '@/services/auth/types';
import {LockOutlined, UserOutlined} from '@ant-design/icons';
import {LoginForm, ProFormText} from '@ant-design/pro-components';
import {history, useLocation, useModel} from '@umijs/max';
import {message} from 'antd';
import {useEffect, useState} from 'react';
import styles from './index.less';

/**
 * 运营后台登录页面。
 *
 * @returns 登录表单
 */
function LoginPage() {
    const {initialState, setInitialState} = useModel('@@initialState');
    const location = useLocation();
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (initialState?.currentOperator) {
            history.replace(resolveRedirectPath(location.search));
        }
    }, [initialState?.currentOperator, location.search]);

    /**
     * 校验账号密码并建立服务端登录会话。
     *
     * @param credentials 登录凭证
     * @returns 始终返回 true，由表单结束提交状态
     */
    const handleLogin = async (credentials: AdminLoginRequest): Promise<boolean> => {
        setSubmitting(true);

        try {
            const currentOperator = await loginAdminOperator(credentials);
            await setInitialState({currentOperator});
            message.success('登录成功');
            history.replace(resolveRedirectPath(location.search));
        } catch {
            message.error('用户名或密码错误');
        } finally {
            setSubmitting(false);
        }

        return true;
    };

    return (
        <main className={styles.page}>
            <section className={styles.loginPanel} aria-label="运营后台登录">
                <LoginForm<AdminLoginRequest>
                    title="Market Forge"
                    subTitle="运营后台"
                    onFinish={handleLogin}
                    submitter={{
                        searchConfig: {submitText: '登录'},
                        submitButtonProps: {loading: submitting, block: true},
                    }}
                >
                    <ProFormText
                        name="username"
                        fieldProps={{size: 'large', prefix: <UserOutlined/>}}
                        placeholder="登录名"
                        rules={[
                            {required: true, message: '请输入登录名'},
                            {max: 64, message: '登录名不能超过 64 个字符'},
                        ]}
                    />

                    <ProFormText.Password
                        name="password"
                        fieldProps={{size: 'large', prefix: <LockOutlined/>}}
                        placeholder="登录密码"
                        rules={[
                            {required: true, message: '请输入登录密码'},
                            {max: 128, message: '登录密码不能超过 128 个字符'},
                        ]}
                    />
                </LoginForm>
            </section>
        </main>
    );
}

/**
 * 从查询参数中读取登录成功后的跳转地址。
 *
 * 只接受站内绝对路径，避免攻击者构造外部跳转地址。
 *
 * @param search 当前查询字符串
 * @returns 安全的站内跳转地址
 */
function resolveRedirectPath(search: string): string {
    const redirect = new URLSearchParams(search).get('redirect');

    if (redirect?.startsWith('/') && !redirect.startsWith('//')) {
        return redirect;
    }

    return '/activity/order';
}

export default LoginPage;
