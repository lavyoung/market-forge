import {request} from '@umijs/max';
import {getAdminCsrfToken} from '../auth/auth';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {
    AwardDefinitionFormValues,
    AwardDefinitionItem,
    AwardDefinitionPage,
    AwardDefinitionQuery,
    AwardDefinitionState,
} from './types';

/** 奖品定义运营管理接口地址。 */
const AWARD_DEFINITION_PATH = '/api/admin/awards';

/**
 * 分页查询奖品定义。
 *
 * @param params ProTable 分页参数和奖品筛选条件
 * @returns ProTable 可以直接消费的分页结果
 * @throws Error 服务端返回失败响应或缺少分页数据时抛出
 */
export async function queryAwardDefinitions(params: AwardDefinitionQuery): Promise<AwardDefinitionPage> {
    const response = await request<ApiResponse<PageResult<AwardDefinitionItem>>>(AWARD_DEFINITION_PATH, {
        method: 'GET',
        params: {
            awardCode: normalizeText(params.awardCode),
            awardName: normalizeText(params.awardName),
            awardType: normalizeText(params.awardType),
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
 * 查询单个奖品定义的最新详情。
 *
 * @param awardId 奖品业务标识
 * @returns 最新奖品定义
 */
export async function getAwardDefinition(awardId: string): Promise<AwardDefinitionItem> {
    const response = await request<ApiResponse<AwardDefinitionItem>>(
        `${AWARD_DEFINITION_PATH}/${encodeURIComponent(awardId)}`,
        {
            method: 'GET',
            withCredentials: true,
        },
    );
    return requireResponseData(response);
}

/**
 * 创建奖品定义草稿。
 *
 * @param values 奖品定义表单数据
 * @returns 创建后的奖品定义
 */
export async function createAwardDefinition(values: AwardDefinitionFormValues): Promise<AwardDefinitionItem> {
    const response = await writeAwardDefinition<AwardDefinitionItem>(
        AWARD_DEFINITION_PATH,
        'POST',
        normalizeFormValues(values),
    );
    return requireResponseData(response);
}

/**
 * 修改奖品定义草稿。
 *
 * @param awardId 奖品业务标识
 * @param expectedVersion 页面最近读取到的乐观锁版本
 * @param values 奖品定义表单数据
 * @returns 修改后的奖品定义
 */
export async function updateAwardDefinition(
    awardId: string,
    expectedVersion: number,
    values: AwardDefinitionFormValues,
): Promise<AwardDefinitionItem> {
    const data = {...normalizeFormValues(values), expectedVersion};
    const response = await writeAwardDefinition<AwardDefinitionItem>(
        `${AWARD_DEFINITION_PATH}/${encodeURIComponent(awardId)}`,
        'PUT',
        data,
    );
    return requireResponseData(response);
}

/**
 * 变更奖品定义生命周期状态。
 *
 * @param awardId 奖品业务标识
 * @param targetState 目标状态
 * @param expectedVersion 页面最近读取到的乐观锁版本
 * @returns 状态变更后的奖品定义
 */
export async function changeAwardDefinitionState(
    awardId: string,
    targetState: Exclude<AwardDefinitionState, 'draft'>,
    expectedVersion: number,
): Promise<AwardDefinitionItem> {
    const response = await writeAwardDefinition<AwardDefinitionItem>(
        `${AWARD_DEFINITION_PATH}/${encodeURIComponent(awardId)}/state`,
        'PATCH',
        {
            targetState,
            expectedVersion,
        },
    );
    return requireResponseData(response);
}

/**
 * 发送带有当前 Session CSRF Token 的奖品写请求。
 *
 * @param path 请求地址
 * @param method HTTP 写方法
 * @param data 请求体
 * @returns 服务端统一响应
 */
async function writeAwardDefinition<T>(
    path: string,
    method: 'POST' | 'PUT' | 'PATCH',
    data: unknown,
): Promise<ApiResponse<T>> {
    const csrfToken = await getAdminCsrfToken();
    return request<ApiResponse<T>>(path, {
        method,
        data,
        headers: {[csrfToken.headerName]: csrfToken.token},
        withCredentials: true,
    });
}

/**
 * 清理表单中的可选文本并保持必填字段原值。
 *
 * @param values 原始表单值
 * @returns 可提交给服务端的奖品定义
 */
function normalizeFormValues(values: AwardDefinitionFormValues): AwardDefinitionFormValues {
    return {...values, providerCode: normalizeText(values.providerCode), awardDesc: normalizeText(values.awardDesc)};
}

/**
 * 去除可选文本首尾空白。
 *
 * @param value 原始文本
 * @returns 清理后的文本；空内容返回 undefined
 */
function normalizeText(value?: string): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
}
