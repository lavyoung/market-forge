import type {ActivitySkuProduct, CreditAccount, CreditExchangeRequest,} from "@/features/credit/model/credit-types";

type ApiResponse<T> = {
    code: number;
    message: string;
    data: T | null;
};

/**
 * 表示积分兑换请求已经发出，但浏览器无法确认服务端最终处理结果。
 *
 * 该错误与明确的业务失败不同。调用方收到它后不能生成新的请求标识重试，
 * 否则第一次请求已经成功时可能造成重复兑换和重复扣减积分。
 */
export class CreditExchangeOutcomeUnknownError extends Error {
    constructor(message: string) {
        super(message);
        this.name = "CreditExchangeOutcomeUnknownError";
    }
}

/**
 * 判断未知值是否为普通对象。
 *
 * @param value 待判断的数据
 * @returns 非空且不是数组的对象返回 true
 */
function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

/**
 * 判断未知值是否为 JavaScript 可以精确表达的正整数。
 *
 * @param value 待判断的数据
 * @returns 安全范围内的正整数返回 true
 */
function isPositiveSafeInteger(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value > 0;
}

/**
 * 判断未知值是否为 JavaScript 可以精确表达的非负整数。
 *
 * @param value 待判断的数据
 * @returns 安全范围内的非负整数返回 true
 */
function isNonNegativeSafeInteger(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value >= 0;
}

/**
 * 解析 Java 服务统一响应，并保留后端业务错误信息。
 *
 * @param response HTTP 响应
 * @returns 统一响应中的业务数据
 * @throws Error HTTP 失败、业务失败、响应结构错误或业务数据为空时抛出
 */
async function unwrap(response: Response): Promise<unknown> {
    let value: unknown;

    try {
        value = await response.json();
    } catch {
        throw new Error("接口未返回合法 JSON");
    }

    if (!isRecord(value)
        || typeof value.code !== "number"
        || typeof value.message !== "string"
        || !("data" in value)) {
        throw new Error("接口响应结构错误");
    }

    const envelope: ApiResponse<unknown> = {
        code: value.code,
        message: value.message,
        data: value.data,
    };

    if (!response.ok || envelope.code !== 0) {
        throw new Error(envelope.message || `请求失败：HTTP ${response.status}`);
    }

    if (envelope.data === null) {
        throw new Error("接口未返回业务数据");
    }

    return envelope.data;
}

/**
 * 校验积分账户响应是否符合页面契约。
 *
 * @param value Java 服务返回的数据
 * @returns 用户和余额字段合法时返回 true
 */
function isCreditAccount(value: unknown): value is CreditAccount {
    return isRecord(value)
        && typeof value.userId === "string"
        && value.userId.trim().length > 0
        && isNonNegativeSafeInteger(value.availableAmount);
}

/**
 * 校验活动 SKU 商品响应是否符合页面契约。
 *
 * @param value Java 服务返回的数据
 * @returns 商品全部字段合法时返回 true
 */
function isActivitySkuProduct(value: unknown): value is ActivitySkuProduct {
    return isRecord(value)
        && isPositiveSafeInteger(value.sku)
        && isPositiveSafeInteger(value.activityId)
        && isNonNegativeSafeInteger(value.stockCount)
        && isNonNegativeSafeInteger(value.stockCountSurplus)
        && isNonNegativeSafeInteger(value.totalCount)
        && isNonNegativeSafeInteger(value.dayCount)
        && isNonNegativeSafeInteger(value.monthCount)
        && isPositiveSafeInteger(value.creditAmount);
}

/**
 * 查询当前用户的可用积分余额。
 *
 * 用户身份由 BFF 从服务端配置补充，浏览器不能指定或替换 userId。
 *
 * @param signal 请求取消信号
 * @returns 当前用户积分账户快照
 * @throws Error 网络失败、业务失败或响应结构不符合契约时抛出
 */
export async function queryCreditAccount(signal?: AbortSignal): Promise<CreditAccount> {
    const response = await fetch("/api/credit/accounts", {cache: "no-store", signal});
    const data = await unwrap(response);

    if (!isCreditAccount(data)) {
        throw new Error("积分账户数据格式不正确");
    }

    return data;
}

/**
 * 查询当前活动配置的积分兑换商品。
 *
 * @param activityId 当前活动标识
 * @param signal 请求取消信号
 * @returns 活动 SKU 商品列表
 * @throws Error 网络失败、业务失败或商品字段不符合契约时抛出
 */
export async function queryActivitySkuProducts(activityId: number, signal?: AbortSignal): Promise<ActivitySkuProduct[]> {
    const params = new URLSearchParams({activityId: String(activityId)});
    const response = await fetch(`/api/activity/sku-products?${params}`, {cache: "no-store", signal});
    const data = await unwrap(response);

    if (!Array.isArray(data) || !data.every(isActivitySkuProduct)) {
        throw new Error("积分兑换商品数据格式不正确");
    }

    return data;
}

/**
 * 使用积分兑换指定活动 SKU。
 *
 * 该请求不会自动重试。网络中断或响应结构异常时，Java 服务可能已经完成
 * 订单创建和积分扣减，因此调用方必须进入结果未知状态并阻止再次兑换。
 *
 * @param sku 要兑换的活动商品 SKU
 * @param requestId 本次页面操作的唯一请求标识
 * @returns Java 服务创建的活动订单号
 * @throws CreditExchangeOutcomeUnknownError 无法确认服务端处理结果时抛出
 * @throws Error 服务端明确拒绝兑换时抛出对应业务错误
 */
export async function exchangeCreditForSku(sku: number, requestId: string): Promise<string> {
    const body: CreditExchangeRequest = {sku, requestId};
    let response: Response;

    try {
        response = await fetch("/api/credit/exchanges/sku", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(body),
        });
    } catch {
        throw new CreditExchangeOutcomeUnknownError("网络连接异常，无法确认兑换结果，请刷新余额和抽奖额度后再操作");
    }

    let data: unknown;

    try {
        data = await unwrap(response);
    } catch (error) {
        if (!response.ok && response.status < 500) {
            throw error;
        }

        const message = error instanceof Error && error.message
            ? error.message
            : "兑换响应无法确认，请刷新余额和抽奖额度，不要重复兑换";
        throw new CreditExchangeOutcomeUnknownError(message);
    }

    if (typeof data !== "string" || data.trim().length === 0) {
        throw new CreditExchangeOutcomeUnknownError("兑换订单号缺失，请刷新余额和抽奖额度，不要重复兑换");
    }

    return data;
}
