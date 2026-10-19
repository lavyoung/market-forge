import {queryActivityOrders} from '@/services/activity/activity-order';
import type {ActivityOrderItem, ActivityOrderQuery} from '@/services/activity/types';
import {PageContainer, ProColumns, ProTable} from '@ant-design/pro-components';

const columns: ProColumns<ActivityOrderItem>[] = [
    {title: '用户 ID', dataIndex: 'userId', copyable: true, ellipsis: true, width: 160},
    {title: '活动 ID', dataIndex: 'activityId', copyable: true, width: 120},
    {title: '活动名称', dataIndex: 'activityName', ellipsis: true, hideInSearch: true, width: 160},
    {title: '活动 SKU', dataIndex: 'sku', copyable: true, hideInSearch: true, width: 110},
    {title: '抽奖策略', dataIndex: 'strategyId', copyable: true, hideInSearch: true, width: 120},
    {title: '订单 ID', dataIndex: 'orderId', copyable: true, ellipsis: true, width: 220},
    {title: '总额度', dataIndex: 'totalCount', hideInSearch: true, width: 90},
    {title: '月额度', dataIndex: 'monthCount', hideInSearch: true, width: 90},
    {title: '日额度', dataIndex: 'dayCount', hideInSearch: true, width: 90},
    {
        title: '订单状态',
        dataIndex: 'orderState',
        valueType: 'select',
        width: 110,
        valueEnum: {
            create: {text: '已创建', status: 'Default'},
            used: {text: '已使用', status: 'Success'},
            cancel: {text: '已作废', status: 'Error'},
            wait_pay: {text: '待支付', status: 'Processing'},
            complete: {text: '已完成', status: 'Success'},
        },
    },
    {
        title: '下单时间',
        dataIndex: 'orderTimeRange',
        valueType: 'dateTimeRange',
        hideInTable: true,
        search: {
            transform: (value: string[]) => ({
                startTime: value?.[0],
                endTime: value?.[1],
            }),
        },
    },
    {title: '下单时间', dataIndex: 'orderTime', valueType: 'dateTime', hideInSearch: true, width: 170},
    {title: '创建时间', dataIndex: 'createTime', valueType: 'dateTime', hideInSearch: true, width: 170},
    {title: '更新时间', dataIndex: 'updateTime', valueType: 'dateTime', hideInSearch: true, width: 170},
];

/**
 * 活动抽奖订单管理页面。
 *
 * @returns 抽奖订单管理页面
 */
function ActivityOrderPage() {
    return (
        <PageContainer title="抽奖订单" subTitle="查询活动产生的用户抽奖订单">
            <ProTable<ActivityOrderItem, ActivityOrderQuery>
                rowKey="orderId"
                headerTitle="抽奖订单列表"
                columns={columns}
                request={queryActivityOrders}
                search={{labelWidth: 'auto', defaultCollapsed: false, collapseRender: false, span: 12}}
                pagination={{defaultPageSize: 5, showSizeChanger: true}}
                scroll={{x: 1650}}
            />
        </PageContainer>
    );
}

export default ActivityOrderPage;
