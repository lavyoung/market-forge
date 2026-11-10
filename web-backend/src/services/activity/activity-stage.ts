import {request} from '@umijs/max';
import {getAdminCsrfToken} from '../auth/auth';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {
    ActivityStageFormValues,
    ActivityStageItem,
    ActivityStagePage,
    ActivityStagePublicationItem,
    ActivityStageQuery,
} from './types';

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
 * @returns 发布任务最新状态
 */
export async function publishActivityStage(stageId: string, expectedVersion: number): Promise<ActivityStagePublicationItem> {
    const response = await writeActivityStage<ActivityStagePublicationItem>(`${ACTIVITY_STAGE_PATH}/${encodeURIComponent(stageId)}/publish`, 'POST', {expectedVersion});
    return requireResponseData(response);
}

/**
 * 查询指定展台版本的发布任务进度。
 *
 * @param stageId 展台业务标识
 * @param stageVersion 发布任务对应的展台版本
 * @returns 发布任务最新状态
 */
export async function getActivityStagePublication(stageId: string, stageVersion: number): Promise<ActivityStagePublicationItem> {
    const path = `${ACTIVITY_STAGE_PATH}/${encodeURIComponent(stageId)}/publications/${stageVersion}`;
    const response = await request<ApiResponse<ActivityStagePublicationItem>>(path, {
        method: 'GET',
        withCredentials: true,
    });
    return requireResponseData(response);
}

/**
 * 轮询处理中发布任务，直到任务结束或达到页面等待上限。
 *
 * <p>轮询仅用于当前请求命中其他发布请求时同步展示进度。达到上限后返回最新的处理中任务，
 * 不把前端等待超时误判为服务端发布失败。</p>
 *
 * @param stageId 展台业务标识
 * @param stageVersion 发布任务对应的展台版本
 * @returns 终态任务或等待上限时的最新任务
 */
export async function waitForActivityStagePublication(stageId: string, stageVersion: number): Promise<ActivityStagePublicationItem> {
    const maxAttempts = 30;
    const intervalMillis = 1000;
    let latest = await getActivityStagePublication(stageId, stageVersion);

    for (let attempt = 1; latest.state === 'processing' && attempt < maxAttempts; attempt += 1) {
        await delay(intervalMillis);
        latest = await getActivityStagePublication(stageId, stageVersion);
    }

    return latest;
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

function delay(milliseconds: number): Promise<void> {
    return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}
