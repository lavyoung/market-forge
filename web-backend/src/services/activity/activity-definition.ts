import {request} from '@umijs/max';
import {getAdminCsrfToken} from '../auth/auth';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {
    ActivityDefinitionFormValues,
    ActivityDefinitionItem,
    ActivityDefinitionPage,
    ActivityDefinitionQuery,
    ActivityDefinitionState,
} from './types';

/** 活动定义运营查询接口地址。 */
const ACTIVITY_DEFINITION_PATH = '/api/admin/activities';

/**
 * 分页查询活动定义。
 *
 * @param params ProTable 分页参数和活动筛选条件
 * @returns ProTable 可以直接消费的分页结果
 * @throws Error 服务端返回失败响应或缺少分页数据时抛出
 */
export async function queryActivityDefinitions(params: ActivityDefinitionQuery): Promise<ActivityDefinitionPage> {
    const response = await request<ApiResponse<PageResult<ActivityDefinitionItem>>>(ACTIVITY_DEFINITION_PATH, {
        method: 'GET',
        params: {
            activityId: normalizeText(params.activityId),
            activityName: normalizeText(params.activityName),
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
 * 查询活动定义最新详情。
 *
 * @param activityId 活动业务标识
 * @returns 最新活动定义
 */
export async function getActivityDefinition(activityId: string): Promise<ActivityDefinitionItem> {
    const response = await request<ApiResponse<ActivityDefinitionItem>>(
        `${ACTIVITY_DEFINITION_PATH}/${encodeURIComponent(activityId)}`,
        {method: 'GET', withCredentials: true},
    );
    return requireResponseData(response);
}

/**
 * 创建活动定义草稿。
 *
 * @param values 活动配置表单
 * @returns 创建后的活动定义
 */
export async function createActivityDefinition(values: ActivityDefinitionFormValues): Promise<ActivityDefinitionItem> {
    const response = await writeActivityDefinition<ActivityDefinitionItem>(
        ACTIVITY_DEFINITION_PATH,
        'POST',
        normalizeFormValues(values),
    );
    return requireResponseData(response);
}

/**
 * 修改活动定义草稿。
 *
 * @param activityId 活动业务标识
 * @param expectedVersion 页面读取到的版本
 * @param values 活动配置表单
 * @returns 修改后的活动定义
 */
export async function updateActivityDefinition(activityId: string, expectedVersion: number, values: ActivityDefinitionFormValues): Promise<ActivityDefinitionItem> {
    const response = await writeActivityDefinition<ActivityDefinitionItem>(
        `${ACTIVITY_DEFINITION_PATH}/${encodeURIComponent(activityId)}`,
        'PUT',
        {...normalizeFormValues(values), expectedVersion},
    );
    return requireResponseData(response);
}

/**
 * 推进活动定义生命周期。
 *
 * @param activityId 活动业务标识
 * @param targetState 目标状态
 * @param expectedVersion 页面读取到的版本
 * @returns 状态变更后的活动定义
 */
export async function changeActivityDefinitionState(activityId: string, targetState: Exclude<ActivityDefinitionState, 'draft'>, expectedVersion: number): Promise<ActivityDefinitionItem> {
    const response = await writeActivityDefinition<ActivityDefinitionItem>(
        `${ACTIVITY_DEFINITION_PATH}/${encodeURIComponent(activityId)}/state`,
        'PATCH',
        {targetState, expectedVersion},
    );
    return requireResponseData(response);
}

/**
 * 查询可供活动展台选择的已开放活动。
 *
 * <p>纯数字关键字按活动标识精确查询，其他关键字按活动名称前缀查询。下拉框最多加载
 * 一百条记录，避免把无边界活动数据一次性拉到浏览器。</p>
 *
 * @param keywords 活动标识或活动名称关键字
 * @returns ProFormSelect 可消费的选项
 * @throws Error 服务端活动查询失败时抛出
 */
export async function queryOpenActivityOptions(keywords?: string): Promise<Array<{ label: string; value: string }>> {
    const normalizedKeywords = normalizeText(keywords);
    const numericKeywords = normalizedKeywords && /^[1-9][0-9]{0,18}$/.test(normalizedKeywords);
    const page = await queryActivityDefinitions({
        current: 1,
        pageSize: 100,
        state: 'open',
        activityId: numericKeywords ? normalizedKeywords : undefined,
        activityName: normalizedKeywords && !numericKeywords ? normalizedKeywords : undefined,
    });
    return page.data.map((activity) => ({
        label: `${activity.activityName} (${activity.activityId})`,
        value: activity.activityId,
    }));
}

async function writeActivityDefinition<T>(path: string, method: 'POST' | 'PUT' | 'PATCH', data: unknown): Promise<ApiResponse<T>> {
    const csrfToken = await getAdminCsrfToken();
    return request<ApiResponse<T>>(path, {
        method,
        data,
        headers: {[csrfToken.headerName]: csrfToken.token},
        withCredentials: true,
    });
}

function normalizeFormValues(values: ActivityDefinitionFormValues): ActivityDefinitionFormValues {
    return {
        ...values,
        activityName: values.activityName.trim(),
        activityDesc: values.activityDesc.trim(),
        activityCountId: values.activityCountId.trim(),
        strategyId: values.strategyId.trim(),
    };
}

function normalizeText(value?: string): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
}
