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
 * 从统一响应中读取成功数据。
 *
 * @param response 服务端统一响应
 * @returns 非空业务数据
 * @throws Error 服务端业务处理失败或数据为空时抛出
 */
export function requireResponseData<T>(response: ApiResponse<T>): T {
    if (response.code !== 0 || response.data === null) {
        throw new Error(response.message);
    }

    return response.data;
}
