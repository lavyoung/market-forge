import {PageContainer} from '@ant-design/pro-components';
import {Alert} from 'antd';

/**
 * 活动抽奖订单管理页面。
 *
 * 当前页面先承担活动运营菜单的路由落点。下一步将在这里接入
 * 订单查询条件、分页 Mock 接口和 ProTable 表格。
 *
 * @returns 抽奖订单管理页面
 */
function ActivityOrderPage() {
    return (
        <PageContainer title="抽奖订单" subTitle="查询活动产生的用户抽奖订单">
            <Alert
                showIcon
                type="info"
                message="抽奖订单列表尚未接入"
                description="下一步将补充订单数据类型、分页 Mock 接口和查询表格。"
            />
        </PageContainer>
    );
}

export default ActivityOrderPage;
