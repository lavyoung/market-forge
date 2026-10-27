import {request} from '@umijs/max';
import {getAdminCsrfToken} from '../auth/auth';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {
    ActivitySkuDefinitionFormValues,
    ActivitySkuDefinitionItem,
    ActivitySkuDefinitionPage,
    ActivitySkuDefinitionQuery,
    ActivitySkuStockAdjustmentFormValues,
} from './types';

/** 活动 SKU 运营接口地址。 */
const ACTIVITY_SKU_PATH = '/api/admin/activity-skus';

/**
 * 分页查询活动 SKU。
 *
 * @param params ProTable 分页参数和筛选条件
 * @returns ProTable 可以直接消费的分页结果
 */
export async function queryActivitySkuDefinitions(params: ActivitySkuDefinitionQuery): Promise<ActivitySkuDefinitionPage> {
    const response = await request<ApiResponse<PageResult<ActivitySkuDefinitionItem>>>(ACTIVITY_SKU_PATH, {
        method: 'GET',
        params: {
            sku: normalizeText(params.sku),
            activityId: normalizeText(params.activityId),
            activityCountId: normalizeText(params.activityCountId),
            pageNumber: params.current ?? 1,
            pageSize: params.pageSize ?? 20,
        },
        withCredentials: true,
    });
    const pageResult = requireResponseData(response);
    return {data: pageResult.records, total: pageResult.total, success: true};
}

/**
 * 查询活动 SKU 最新详情。
 *
 * @param sku SKU 业务标识
 * @returns 最新 SKU 定义
 */
export async function getActivitySkuDefinition(sku: string): Promise<ActivitySkuDefinitionItem> {
    const response = await request<ApiResponse<ActivitySkuDefinitionItem>>(
        `${ACTIVITY_SKU_PATH}/${encodeURIComponent(sku)}`,
        {method: 'GET', withCredentials: true},
    );
    return requireResponseData(response);
}

/**
 * 创建活动 SKU。
 *
 * @param values SKU 表单值
 * @returns 创建后的 SKU 定义
 */
export async function createActivitySkuDefinition(values: ActivitySkuDefinitionFormValues): Promise<ActivitySkuDefinitionItem> {
    const response = await writeActivitySkuDefinition<ActivitySkuDefinitionItem>(
        ACTIVITY_SKU_PATH,
        'POST',
        normalizeDefinitionValues(values, true),
    );
    return requireResponseData(response);
}

/**
 * 修改活动 SKU 普通配置。
 *
 * @param sku SKU 业务标识
 * @param expectedVersion 页面读取到的乐观锁版本
 * @param values SKU 表单值
 * @returns 修改后的 SKU 定义
 */
export async function updateActivitySkuDefinition(sku: string, expectedVersion: number, values: ActivitySkuDefinitionFormValues): Promise<ActivitySkuDefinitionItem> {
    const response = await writeActivitySkuDefinition<ActivitySkuDefinitionItem>(
        `${ACTIVITY_SKU_PATH}/${encodeURIComponent(sku)}`,
        'PUT',
        {...normalizeDefinitionValues(values, false), expectedVersion},
    );
    return requireResponseData(response);
}

/**
 * 调整活动 SKU 库存。
 *
 * @param sku SKU 业务标识
 * @param expectedVersion 页面读取到的乐观锁版本
 * @param values 库存调整表单值
 * @returns 调整后的 SKU 定义
 */
export async function adjustActivitySkuStock(sku: string, expectedVersion: number, values: ActivitySkuStockAdjustmentFormValues): Promise<ActivitySkuDefinitionItem> {
    const response = await writeActivitySkuDefinition<ActivitySkuDefinitionItem>(
        `${ACTIVITY_SKU_PATH}/${encodeURIComponent(sku)}/stock-adjustments`,
        'POST',
        {quantityDelta: values.quantityDelta, reason: values.reason.trim(), expectedVersion},
    );
    return requireResponseData(response);
}

async function writeActivitySkuDefinition<T>(path: string, method: 'POST' | 'PUT', data: unknown): Promise<ApiResponse<T>> {
    const csrfToken = await getAdminCsrfToken();
    return request<ApiResponse<T>>(path, {
        method,
        data,
        headers: {[csrfToken.headerName]: csrfToken.token},
        withCredentials: true,
    });
}

function normalizeDefinitionValues(values: ActivitySkuDefinitionFormValues, includeStock: boolean) {
    return {
        activityId: values.activityId.trim(),
        activityCountId: values.activityCountId.trim(),
        ...(includeStock ? {stockCount: values.stockCount} : {}),
        creditAmount: values.creditAmount ?? null,
    };
}

function normalizeText(value?: string): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
}
