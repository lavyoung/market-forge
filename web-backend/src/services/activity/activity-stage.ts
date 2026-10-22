import {request} from '@umijs/max';
import {getAdminCsrfToken} from '../auth/auth';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {ActivityStageFormValues, ActivityStageItem, ActivityStagePage, ActivityStageQuery} from './types';

/** 活动展台运营管理接口地址。 */
const ACTIVITY_STAGE_PATH = '/api/admin/activity-stages';

/**
 * 分页查询活动展台。
 *
 * @param params ProTable 分页参数和展台筛选条件
 * @returns ProTable 可以直接消费的分页结果
 * @throws Error 服务端返回失败响应或缺少分页数据时抛出
 */
export async function queryActivityStages(params: ActivityStageQuery): Promise<ActivityStagePage> {
    const response = await request<ApiResponse<PageResult<ActivityStageItem>>>(ACTIVITY_STAGE_PATH, {
        method: 'GET',
        params: {
            channel: normalizeText(params.channel),
            source: normalizeText(params.source),
            state: params.state,
            pageNumber: params.current ?? 1,
            pageSize: params.pageSize ?? 20,
        },
        withCredentials: true,
    });
    const pageResult = requireResponseData(response);
    return {data: pageResult.records, total: pageResult.total, success: true};
}

/**
 * 查询活动展台最新详情。
 *
 * @param stageId 展台业务标识
 * @returns 最新活动展台
 */
export async function getActivityStage(stageId: string): Promise<ActivityStageItem> {
    const response = await request<ApiResponse<ActivityStageItem>>(`${ACTIVITY_STAGE_PATH}/${encodeURIComponent(stageId)}`, {
        method: 'GET',
        withCredentials: true,
    });
    return requireResponseData(response);
}

/**
 * 创建待上架活动展台。
 *
 * @param values 活动展台表单数据
 * @returns 创建后的活动展台
 */
export async function createActivityStage(values: ActivityStageFormValues): Promise<ActivityStageItem> {
    const response = await writeActivityStage<ActivityStageItem>(ACTIVITY_STAGE_PATH, 'POST', normalizeFormValues(values));
    return requireResponseData(response);
}

/**
 * 修改待上架活动展台。
 *
 * @param stageId 展台业务标识
 * @param expectedVersion 页面最近读取到的乐观锁版本
 * @param values 活动展台表单数据
 * @returns 修改后的活动展台
 */
export async function updateActivityStage(stageId: string, expectedVersion: number, values: ActivityStageFormValues): Promise<ActivityStageItem> {
    const response = await writeActivityStage<ActivityStageItem>(`${ACTIVITY_STAGE_PATH}/${encodeURIComponent(stageId)}`, 'PUT', {
        ...normalizeFormValues(values),
        expectedVersion
    });
    return requireResponseData(response);
}

/**
 * 预热并发布活动展台。
 *
 * @param stageId 展台业务标识
 * @param expectedVersion 页面最近读取到的乐观锁版本
 * @returns 发布后的活动展台
 */
export async function publishActivityStage(stageId: string, expectedVersion: number): Promise<ActivityStageItem> {
    const response = await writeActivityStage<ActivityStageItem>(`${ACTIVITY_STAGE_PATH}/${encodeURIComponent(stageId)}/publish`, 'POST', {expectedVersion});
    return requireResponseData(response);
}

/**
 * 发送带有当前 Session CSRF Token 的活动展台写请求。
 *
 * @param path 请求地址
 * @param method HTTP 写方法
 * @param data 请求体
 * @returns 服务端统一响应
 */
async function writeActivityStage<T>(path: string, method: 'POST' | 'PUT', data: unknown): Promise<ApiResponse<T>> {
    const csrfToken = await getAdminCsrfToken();
    return request<ApiResponse<T>>(path, {
        method,
        data,
        headers: {[csrfToken.headerName]: csrfToken.token},
        withCredentials: true,
    });
}

function normalizeFormValues(values: ActivityStageFormValues): ActivityStageFormValues {
    return {channel: values.channel.trim(), source: values.source.trim(), activityId: values.activityId.trim()};
}

function normalizeText(value?: string): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
}
