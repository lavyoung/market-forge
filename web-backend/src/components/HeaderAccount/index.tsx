import {logoutAdminOperator} from '@/services/auth/auth';
import {getApiErrorMessage} from '@/services/errors';
import {LogoutOutlined, UserOutlined} from '@ant-design/icons';
import {history, useModel} from '@umijs/max';
import {Avatar, Button, message, Space, Tooltip, Typography} from 'antd';
import {useState} from 'react';
import styles from './index.less';

const {Text} = Typography;

/**
 * 后台顶部操作员信息和退出入口。
 *
 * @returns 已登录操作员信息；未登录时不渲染
 */
function HeaderAccount() {
    const {initialState, setInitialState} = useModel('@@initialState');
    const [loggingOut, setLoggingOut] = useState(false);
    const operator = initialState?.currentOperator;

    if (!operator) {
        return null;
    }

    /**
     * 注销服务端 Session，并清理前端登录状态。
     */
    const handleLogout = async (): Promise<void> => {
        setLoggingOut(true);

        try {
            await logoutAdminOperator();
            await setInitialState({});
            history.replace('/login');
            message.success('已退出登录');
        } catch (error) {
            message.error(getApiErrorMessage(error, '退出登录失败，请稍后重试'));
        } finally {
            setLoggingOut(false);
        }
    };

    return (
        <Space className={styles.account} size={10}>
            <Avatar className={styles.avatar} size={32} icon={<UserOutlined/>}/>

            <span className={styles.identity}>
                <Text strong className={styles.displayName}>
                    {operator.displayName}
                </Text>
                <Text type="secondary" className={styles.username}>
                    {operator.username}
                </Text>
            </span>

            <Tooltip title="退出登录">
                <Button
                    className={styles.logout}
                    type="text"
                    icon={<LogoutOutlined/>}
                    loading={loggingOut}
                    aria-label="退出登录"
                    onClick={handleLogout}
                />
            </Tooltip>
        </Space>
    );
}

export default HeaderAccount;
