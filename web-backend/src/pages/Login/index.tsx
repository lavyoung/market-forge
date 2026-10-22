import loginArtwork from '@/assets/login-architecture.png';
import {getCurrentAdminOperator, loginAdminOperator} from '@/services/auth/auth';
import type {AdminLoginRequest} from '@/services/auth/types';
import {ArrowRightOutlined, DeploymentUnitOutlined, LockOutlined, UserOutlined} from '@ant-design/icons';
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
        if (!initialState?.currentOperator) {
            return undefined;
        }

        let active = true;
        getCurrentAdminOperator()
            .then(() => {
                if (active) {
                    history.replace(resolveRedirectPath(location.search));
                }
            })
            .catch(() => {
                if (active) {
                    void setInitialState({});
                }
            });

        return () => {
            active = false;
        };
    }, [initialState?.currentOperator, location.search, setInitialState]);

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
            <section className={styles.artwork} aria-hidden="true">
                <img className={styles.artworkImage} src={loginArtwork} alt=""/>
                <div className={styles.artworkBrand}>
                    <span className={styles.brandMark}><DeploymentUnitOutlined/></span>
                    <span>Market Forge</span>
                </div>
                <p className={styles.artworkCaption}>专注每一次营销活动的稳定运行</p>
            </section>

            <section className={styles.loginRegion} aria-label="运营后台登录">
                <div className={styles.mobileBrand}>
                    <span className={styles.brandMark}><DeploymentUnitOutlined/></span>
                    <span>Market Forge</span>
                </div>

                <div className={styles.loginPanel}>
                    <header className={styles.heading}>
                        <span className={styles.eyebrow}>OPERATIONS CONSOLE</span>
                        <h1>欢迎回来</h1>
                        <p>登录 Market Forge 运营后台</p>
                    </header>

                    <LoginForm<AdminLoginRequest>
                        className={styles.loginForm}
                        onFinish={handleLogin}
                        submitter={{
                            searchConfig: {submitText: '登录'},
                            submitButtonProps: {
                                loading: submitting,
                                block: true,
                                icon: <ArrowRightOutlined/>,
                            },
                        }}
                    >
                        <ProFormText
                            name="username"
                            label="账号"
                            fieldProps={{size: 'large', prefix: <UserOutlined/>, autoComplete: 'username'}}
                            placeholder="请输入登录账号"
                            rules={[
                                {required: true, message: '请输入登录账号'},
                                {max: 64, message: '登录账号不能超过 64 个字符'},
                            ]}
                        />

                        <ProFormText.Password
                            name="password"
                            label="密码"
                            fieldProps={{size: 'large', prefix: <LockOutlined/>, autoComplete: 'current-password'}}
                            placeholder="请输入登录密码"
                            rules={[
                                {required: true, message: '请输入登录密码'},
                                {max: 128, message: '登录密码不能超过 128 个字符'},
                            ]}
                        />
                    </LoginForm>

                    <p className={styles.copyright}>© 2026 Market Forge</p>
                </div>
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
