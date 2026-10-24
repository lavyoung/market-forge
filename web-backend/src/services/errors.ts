/**
 * 运营后台统一 API 异常。
 */
export class ApiError extends Error {
    readonly code: number;
    readonly httpStatus?: number;

    constructor(code: number, message: string, httpStatus?: number) {
        super(message);
        this.name = 'ApiError';
        this.code = code;
        this.httpStatus = httpStatus;
    }
}

interface ApiFailurePayload {
    code: number;
    message: string;
}

interface RequestErrorLike {
    data?: unknown;
    response?: { status?: number; data?: unknown };
    message?: string;
}

/**
 * 将 Umi 请求异常或业务异常统一转换为 {@link ApiError}。
 *
 * @param error 原始异常
 * @param fallbackMessage 无法识别异常结构时使用的稳定文案
 * @returns 保留业务码、HTTP 状态和服务端文案的 API 异常
 */
export function normalizeApiError(error: unknown, fallbackMessage = '请求处理失败'): ApiError {
    if (error instanceof ApiError) {
        return error;
    }

    const candidate = isObject(error) ? (error as RequestErrorLike) : undefined;
    const payload = toFailurePayload(candidate?.data) ?? toFailurePayload(candidate?.response?.data);
    const httpStatus = candidate?.response?.status;
    const message = payload?.message || getHttpStatusMessage(httpStatus) || candidate?.message || fallbackMessage;
    return new ApiError(payload?.code ?? -1, message, httpStatus);
}

/**
 * 获取适合直接展示给运营人员的错误文案。
 *
 * @param error 原始异常
 * @param fallbackMessage 无法识别异常时的场景文案
 * @returns 服务端业务文案或稳定兜底文案
 */
export function getApiErrorMessage(error: unknown, fallbackMessage: string): string {
    return normalizeApiError(error, fallbackMessage).message;
}

function toFailurePayload(value: unknown): ApiFailurePayload | undefined {
    if (!isObject(value) || typeof value.code !== 'number' || typeof value.message !== 'string') {
        return undefined;
    }
    return {code: value.code, message: value.message};
}

function getHttpStatusMessage(httpStatus: number | undefined): string | undefined {
    switch (httpStatus) {
        case 403:
            return '当前账号没有执行该操作的权限';
        case 409:
            return '数据已被其他操作修改，请刷新后重试';
        case 422:
            return '业务配置校验失败，请核对必填配置';
        case 503:
            return '服务当前容量不足，请稍后重试';
        default:
            return undefined;
    }
}

function isObject(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
}
