import {ApiError} from './errors';

/**
 * Java 服务端统一响应结构。
 */
export interface ApiResponse<T> {
    /** 业务响应码，0 表示成功。 */
    code: number;
    /** 响应说明。 */
    message: string;
    /** 成功时的业务数据，失败时为空。 */
    data: T | null;
}

/**
 * 服务端统一分页结构。
 */
export interface PageResult<T> {
    /** 当前页记录。 */
    records: T[];
    /** 符合查询条件的记录总数。 */
    total: number;
    /** 当前页码，从 1 开始。 */
    pageNumber: number;
    /** 每页记录数。 */
    pageSize: number;
}

/**
 * 从统一响应中读取成功数据。
 *
 * @param response 服务端统一响应
 * @returns 非空业务数据
 * @throws ApiError 服务端业务处理失败或成功响应未返回数据时抛出
 */
export function requireResponseData<T>(response: ApiResponse<T>): T {
    if (response.code !== 0) {
        throw new ApiError(response.code, response.message);
    }
    if (response.data === null) {
        throw new ApiError(-1, '服务端未返回业务数据');
    }

    return response.data;
}
