import type {ActivityOrderItem, ActivityOrderPage, ActivityOrderQuery} from '../src/services/activity/types';

interface MockRequest {
    query: Partial<Record<keyof ActivityOrderQuery, string>>;
}

interface MockResponse {
    json(body: ActivityOrderPage): void;
}

const activityOrders: ActivityOrderItem[] = [
    {
        id: '1',
        userId: 'demo-user-001',
        activityId: '100301',
        sku: '9011',
        activityName: '周末幸运抽奖',
        strategyId: '100006',
        orderId: 'ROPmv881ipx0001o868jrfqdaxt3m7o',
        orderTime: '2026-10-18 09:15:12',
        totalCount: 1,
        dayCount: 1,
        monthCount: 1,
        orderState: 'used',
        outBusinessNo: 'raffle-20261018-001',
        createTime: '2026-10-18 09:15:12',
        updateTime: '2026-10-18 09:16:03',
    },
    {
        id: '2',
        userId: 'demo-user-001',
        activityId: '100301',
        sku: '9011',
        activityName: '周末幸运抽奖',
        strategyId: '100006',
        orderId: 'ROPmv88j9me0001cv3im7kxhs03qvxa',
        orderTime: '2026-10-18 09:18:26',
        totalCount: 1,
        dayCount: 1,
        monthCount: 1,
        orderState: 'create',
        outBusinessNo: 'raffle-20261018-002',
        createTime: '2026-10-18 09:18:26',
        updateTime: '2026-10-18 09:18:26',
    },
    {
        id: '3',
        userId: 'user-001',
        activityId: '100301',
        sku: '9011',
        activityName: '周末幸运抽奖',
        strategyId: '100006',
        orderId: 'ROPmv8auxy900018yrjxn73ik531dmv',
        orderTime: '2026-10-18 10:02:44',
        totalCount: 1,
        dayCount: 1,
        monthCount: 1,
        orderState: 'used',
        outBusinessNo: 'raffle-20261018-003',
        createTime: '2026-10-18 10:02:44',
        updateTime: '2026-10-18 10:03:10',
    },
    {
        id: '4',
        userId: 'user-001',
        activityId: '100301',
        sku: '9012',
        activityName: '周末幸运抽奖',
        strategyId: '100006',
        orderId: 'ROPmv8av9hj0002z38fvn2949wettup',
        orderTime: '2026-10-18 10:08:05',
        totalCount: 10,
        dayCount: 1,
        monthCount: 10,
        orderState: 'complete',
        outBusinessNo: 'credit-exchange-001',
        createTime: '2026-10-18 10:08:05',
        updateTime: '2026-10-18 10:08:07',
    },
    {
        id: '5',
        userId: 'user-002',
        activityId: '100302',
        sku: '9021',
        activityName: '新用户体验活动',
        strategyId: '100007',
        orderId: 'ROPmv8av9so0003rmmq1eu57uau7e5d',
        orderTime: '2026-10-18 11:21:37',
        totalCount: 5,
        dayCount: 1,
        monthCount: 5,
        orderState: 'wait_pay',
        outBusinessNo: 'credit-exchange-002',
        createTime: '2026-10-18 11:21:37',
        updateTime: '2026-10-18 11:21:37',
    },
    {
        id: '6',
        userId: 'user-003',
        activityId: '100302',
        sku: '9021',
        activityName: '新用户体验活动',
        strategyId: '100007',
        orderId: 'ROPmv8b0a0g0004u77zr8n1g25dyfp',
        orderTime: '2026-10-18 12:35:19',
        totalCount: 1,
        dayCount: 1,
        monthCount: 1,
        orderState: 'cancel',
        outBusinessNo: 'raffle-20261018-006',
        createTime: '2026-10-18 12:35:19',
        updateTime: '2026-10-18 12:36:00',
    },
    {
        id: '7',
        userId: 'user-004',
        activityId: '100303',
        sku: '9031',
        activityName: '积分回馈活动',
        strategyId: '100008',
        orderId: 'ROPmv8b1b1h0005v88as9o2h36ezgq',
        orderTime: '2026-10-18 13:46:28',
        totalCount: 20,
        dayCount: 2,
        monthCount: 20,
        orderState: 'complete',
        outBusinessNo: 'credit-exchange-003',
        createTime: '2026-10-18 13:46:28',
        updateTime: '2026-10-18 13:46:30',
    },
    {
        id: '8',
        userId: 'user-005',
        activityId: '100303',
        sku: '9031',
        activityName: '积分回馈活动',
        strategyId: '100008',
        orderId: 'ROPmv8b2c2i0006w99bt0p3i47far',
        orderTime: '2026-10-18 14:58:41',
        totalCount: 1,
        dayCount: 1,
        monthCount: 1,
        orderState: 'create',
        outBusinessNo: 'raffle-20261018-008',
        createTime: '2026-10-18 14:58:41',
        updateTime: '2026-10-18 14:58:41',
    },
];

function parsePositiveInteger(value: string | undefined, fallback: number): number {
    const parsed = Number(value);
    return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function containsIgnoreCase(value: string, keyword: string | undefined): boolean {
    return !keyword || value.toLowerCase().includes(keyword.trim().toLowerCase());
}

/**
 * 根据查询条件生成活动订单分页结果。
 *
 * @param query Mock 请求查询参数
 * @returns 可直接交给 ProTable 的分页数据
 */
export function queryActivityOrderPage(query: MockRequest['query']): ActivityOrderPage {
    const current = parsePositiveInteger(query.current, 1);
    const pageSize = Math.min(parsePositiveInteger(query.pageSize, 5), 100);
    const filteredOrders = activityOrders.filter((order) => {
        return (
            containsIgnoreCase(order.userId, query.userId) &&
            containsIgnoreCase(order.orderId, query.orderId) &&
            (!query.activityId || order.activityId === query.activityId.trim()) &&
            (!query.orderState || order.orderState === query.orderState)
        );
    });
    const start = (current - 1) * pageSize;

    return {
        data: filteredOrders.slice(start, start + pageSize),
        total: filteredOrders.length,
        success: true,
    };
}

export default {
    'GET /api/activity/orders': (request: MockRequest, response: MockResponse) => {
        response.json(queryActivityOrderPage(request.query));
    },
};
