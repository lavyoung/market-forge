import {request} from '@umijs/max';
import type {ApiResponse} from '../types';
import {requireResponseData} from '../types';
import type {AwardTypeMetadata} from './types';

/** 奖品类型运营元数据接口地址。 */
const AWARD_TYPE_METADATA_PATH = '/api/admin/award-types';

/**
 * 查询系统当前真实支持的奖品类型和结构化表单定义。
 *
 * @returns 按奖品类型编码稳定排序的元数据列表
 * @throws Error 服务端返回失败响应或缺少响应数据时抛出
 */
export async function queryAwardTypeMetadata(): Promise<AwardTypeMetadata[]> {
    const response = await request<ApiResponse<AwardTypeMetadata[]>>(AWARD_TYPE_METADATA_PATH, {
        method: 'GET',
        withCredentials: true,
    });
    return requireResponseData(response);
}
