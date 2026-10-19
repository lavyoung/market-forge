import {queryUserAwardRecords} from '@/services/award/user-award-record';
import type {UserAwardRecordItem, UserAwardRecordQuery} from '@/services/award/types';
import type {ProColumns} from '@ant-design/pro-components';
import {PageContainer, ProTable} from '@ant-design/pro-components';

const columns: ProColumns<UserAwardRecordItem>[] = [
    {title: '用户 ID', dataIndex: 'userId', copyable: true, ellipsis: true, width: 160},
    {title: '活动 ID', dataIndex: 'activityId', copyable: true, hideInSearch: true, width: 120},
    {title: '抽奖策略', dataIndex: 'strategyId', copyable: true, hideInSearch: true, width: 130},
    {title: '奖品 ID', dataIndex: 'awardId', valueType: 'digit', copyable: true, width: 120},
    {title: '奖品名称', dataIndex: 'awardTitle', ellipsis: true, hideInSearch: true, width: 180},
    {title: '订单 ID', dataIndex: 'orderId', copyable: true, ellipsis: true, hideInSearch: true, width: 230},
    {
        title: '发奖状态',
        dataIndex: 'awardState',
        valueType: 'select',
        width: 120,
        valueEnum: {
            create: {text: '已创建', status: 'Default'},
            wait_claim: {text: '待领取', status: 'Warning'},
            granting: {text: '发奖中', status: 'Processing'},
            grant_failed: {text: '发奖失败', status: 'Error'},
            completed: {text: '发奖完成', status: 'Success'},
        },
    },
    {
        title: '中奖时间',
        dataIndex: 'awardTimeRange',
        valueType: 'dateTimeRange',
        hideInTable: true,
        search: {
            transform: (value: string[]) => ({
                startTime: value?.[0],
                endTime: value?.[1],
            }),
        },
    },
    {title: '中奖时间', dataIndex: 'awardTime', valueType: 'dateTime', hideInSearch: true, width: 180},
];

/**
 * 用户中奖记录运营查询页面。
 *
 * 页面只负责声明查询表单和表格展示结构，分页参数转换、请求发送和统一响应
 * 解包均由查询服务完成。
 *
 * @returns 用户中奖记录分页查询页面
 */
function UserAwardRecordPage() {
    return (
        <PageContainer title="中奖记录" subTitle="查询用户抽奖后的中奖与发奖状态">
            <ProTable<UserAwardRecordItem, UserAwardRecordQuery>
                rowKey="orderId"
                headerTitle="用户中奖记录"
                columns={columns}
                request={queryUserAwardRecords}
                search={{labelWidth: 'auto', defaultCollapsed: false, collapseRender: false, span: 12}}
                pagination={{defaultPageSize: 10, showSizeChanger: true}}
                scroll={{x: 1250}}
            />
        </PageContainer>
    );
}

export default UserAwardRecordPage;
