import {request} from '@umijs/max';
import type {ActivityOrderPage, ActivityOrderQuery} from './types';

/**
 * 分页查询活动订单。
 *
 * 页面只依赖该服务函数，不感知当前数据来自 Mock 还是实际 Java 接口。
 * 后续联调只需要保持请求和响应契约一致。
 *
 * @param params 分页及筛选条件
 * @returns 活动订单分页结果
 */
export async function queryActivityOrders(params: ActivityOrderQuery): Promise<ActivityOrderPage> {
    return request<ActivityOrderPage>('/api/activity/orders', {
        method: 'GET',
        params,
    });
}
