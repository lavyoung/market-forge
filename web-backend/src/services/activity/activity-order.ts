import {request} from '@umijs/max';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {ActivityOrderItem, ActivityOrderPage, ActivityOrderQuery} from './types';

const ACTIVITY_ORDER_PATH = '/api/admin/activity/orders';

/**
 * 分页查询跨分库汇总后的活动订单。
 *
 * 将 ProTable 的分页参数转换为 Java 接口参数，并把服务端统一分页响应
 * 转换为 ProTable 所需结构。
 *
 * @param params ProTable 分页及筛选条件
 * @returns ProTable 活动订单分页结果
 */
export async function queryActivityOrders(params: ActivityOrderQuery): Promise<ActivityOrderPage> {
    const response = await request<ApiResponse<PageResult<ActivityOrderItem>>>(ACTIVITY_ORDER_PATH, {
        method: 'GET',
        params: {
            userId: normalizeText(params.userId),
            activityId: normalizeText(params.activityId),
            orderId: normalizeText(params.orderId),
            orderState: params.orderState,
            startTime: normalizeDateTime(params.startTime),
            endTime: normalizeDateTime(params.endTime),
            pageNumber: params.current ?? 1,
            pageSize: params.pageSize ?? 5,
        },
        withCredentials: true,
    });

    const pageResult = requireResponseData(response);

    return {
        data: pageResult.records,
        total: pageResult.total,
        success: true,
    };
}

/**
 * 去除查询文本首尾空白，空字符串不传给后端。
 *
 * @param value 原始查询文本
 * @returns 标准化文本
 */
function normalizeText(value?: string): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
}

/**
 * 将 ProTable 时间字符串转换成 LocalDateTime 接受的 ISO 格式。
 *
 * @param value 页面时间字符串
 * @returns ISO 本地时间
 */
function normalizeDateTime(value?: string): string | undefined {
    return normalizeText(value)?.replace(' ', 'T');
}
