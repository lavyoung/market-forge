import {request} from '@umijs/max';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {UserAwardRecordItem, UserAwardRecordPage, UserAwardRecordQuery} from './types';

/**
 * 用户中奖记录运营查询地址。
 *
 * 浏览器请求经过 Umi 代理后，会被转发到后端
 * /api/v1/admin/award/records。
 */
const USER_AWARD_RECORD_PATH = '/api/admin/award/records';

/**
 * 分页查询跨分库汇总后的用户中奖记录。
 *
 * 将 ProTable 使用的 current 参数转换为后端使用的 pageNo，
 * 并将服务端统一分页响应转换成 ProTable 所需的数据结构。
 *
 * @param params ProTable 分页参数和中奖记录筛选条件
 * @returns ProTable 可以直接消费的中奖记录分页结果
 * @throws Error 服务端返回失败响应或未返回分页数据时抛出
 */
export async function queryUserAwardRecords(params: UserAwardRecordQuery): Promise<UserAwardRecordPage> {
    const response = await request<ApiResponse<PageResult<UserAwardRecordItem>>>(USER_AWARD_RECORD_PATH, {
        method: 'GET',
        params: {
            userId: normalizeText(params.userId),
            awardId: params.awardId,
            awardState: params.awardState,
            startTime: normalizeDateTime(params.startTime),
            endTime: normalizeDateTime(params.endTime),
            pageNo: params.current ?? 1,
            pageSize: params.pageSize ?? 10,
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
 * 去除查询文本首尾空白。
 *
 * 空字符串转换为 undefined 后，Umi 不会将其作为有效筛选值发送，
 * 防止后端收到没有业务意义的空白用户标识。
 *
 * @param value 页面提交的原始文本
 * @returns 清理后的文本；没有有效内容时返回 undefined
 */
function normalizeText(value?: string): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
}

/**
 * 将页面时间转换为后端 LocalDateTime 接受的 ISO 格式。
 *
 * Ant Design 表单可能产生“2026-10-19 10:30:00”格式，
 * 后端要求使用“2026-10-19T10:30:00”，因此需要替换中间分隔符。
 *
 * @param value 页面提交的日期时间
 * @returns ISO 本地日期时间；未填写时返回 undefined
 */
function normalizeDateTime(value?: string): string | undefined {
    return normalizeText(value)?.replace(' ', 'T');
}
