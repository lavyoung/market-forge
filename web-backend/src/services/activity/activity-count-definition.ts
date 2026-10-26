import {request} from '@umijs/max';
import {getAdminCsrfToken} from '../auth/auth';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {
    ActivityCountDefinitionFormValues,
    ActivityCountDefinitionItem,
    ActivityCountDefinitionPage,
    ActivityCountDefinitionQuery,
} from './types';

/** 活动次数配置运营接口地址。 */
const ACTIVITY_COUNT_PATH = '/api/admin/activity-counts';

/**
 * 分页查询活动次数配置。
 *
 * @param params ProTable 分页参数和筛选条件
 * @returns ProTable 可以直接消费的分页结果
 */
export async function queryActivityCountDefinitions(params: ActivityCountDefinitionQuery): Promise<ActivityCountDefinitionPage> {
    const response = await request<ApiResponse<PageResult<ActivityCountDefinitionItem>>>(ACTIVITY_COUNT_PATH, {
        method: 'GET',
        params: {
            activityCountId: normalizeText(params.activityCountId),
            activityCountName: normalizeText(params.activityCountName),
            pageNumber: params.current ?? 1,
            pageSize: params.pageSize ?? 20,
        },
        withCredentials: true,
    });
    const pageResult = requireResponseData(response);
    return {data: pageResult.records, total: pageResult.total, success: true};
}

/**
 * 查询活动次数配置最新详情。
 *
 * @param activityCountId 活动次数配置业务标识
 * @returns 最新活动次数配置
 */
export async function getActivityCountDefinition(activityCountId: string): Promise<ActivityCountDefinitionItem> {
    const response = await request<ApiResponse<ActivityCountDefinitionItem>>(
        `${ACTIVITY_COUNT_PATH}/${encodeURIComponent(activityCountId)}`,
        {method: 'GET', withCredentials: true},
    );
    return requireResponseData(response);
}

/**
 * 创建活动次数配置。
 *
 * @param values 配置表单值
 * @returns 创建后的配置
 */
export async function createActivityCountDefinition(values: ActivityCountDefinitionFormValues): Promise<ActivityCountDefinitionItem> {
    const response = await writeActivityCountDefinition<ActivityCountDefinitionItem>(
        ACTIVITY_COUNT_PATH, 'POST', normalizeFormValues(values),
    );
    return requireResponseData(response);
}

/**
 * 修改活动次数配置。
 *
 * @param activityCountId 活动次数配置业务标识
 * @param expectedVersion 页面读取到的版本
 * @param values 配置表单值
 * @returns 修改后的配置
 */
export async function updateActivityCountDefinition(activityCountId: string, expectedVersion: number, values: ActivityCountDefinitionFormValues): Promise<ActivityCountDefinitionItem> {
    const response = await writeActivityCountDefinition<ActivityCountDefinitionItem>(
        `${ACTIVITY_COUNT_PATH}/${encodeURIComponent(activityCountId)}`,
        'PUT',
        {...normalizeFormValues(values), expectedVersion},
    );
    return requireResponseData(response);
}

/**
 * 查询活动定义表单可选择的次数配置。
 *
 * @param keywords 配置标识或名称关键字
 * @returns 下拉选择器选项
 */
export async function queryActivityCountOptions(keywords?: string): Promise<Array<{ label: string; value: string }>> {
    const normalizedKeywords = normalizeText(keywords);
    const numericKeywords = normalizedKeywords && /^[1-9][0-9]{0,18}$/.test(normalizedKeywords);
    const page = await queryActivityCountDefinitions({
        current: 1,
        pageSize: 100,
        activityCountId: numericKeywords ? normalizedKeywords : undefined,
        activityCountName: normalizedKeywords && !numericKeywords ? normalizedKeywords : undefined,
    });
    return page.data.map((definition) => ({
        label: `${definition.activityCountName}（日 ${definition.dayCount} / 月 ${definition.monthCount} / 总 ${definition.totalCount}）`,
        value: definition.activityCountId,
    }));
}

async function writeActivityCountDefinition<T>(path: string, method: 'POST' | 'PUT', data: unknown): Promise<ApiResponse<T>> {
    const csrfToken = await getAdminCsrfToken();
    return request<ApiResponse<T>>(path, {
        method,
        data,
        headers: {[csrfToken.headerName]: csrfToken.token},
        withCredentials: true,
    });
}

function normalizeFormValues(values: ActivityCountDefinitionFormValues): ActivityCountDefinitionFormValues {
    return {...values, activityCountName: values.activityCountName.trim()};
}

function normalizeText(value?: string): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
}
