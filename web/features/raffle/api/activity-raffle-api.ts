import {
    ActivityArmoryRequest,
    ActivityAward,
    ActivityDrawRequest,
    ActivityDrawResult,
    ApiResponse,
    AwardUnlockScope,
} from "@/features/raffle/model/activity-raffle-types";

/**
 * 判断未知值是否为普通对象。
 */
function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

/**
 * 判断未知值是否为 JavaScript 可安全表达的正整数标识。
 */
function isPositiveId(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value > 0;
}

/**
 * 判断未知值是否为非负整数。
 */
function isNonNegativeInteger(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value >= 0;
}

/**
 * 判断未知值是否为空或非负整数。
 */
function isNullableNonNegativeInteger(value: unknown): value is number | null {
    return value === null || isNonNegativeInteger(value);
}

/**
 * 判断未知值是否为空或字符串。
 */
function isNullableString(value: unknown): value is string | null {
    return value === null || typeof value === "string";
}

/**
 * 判断未知值是否为后端支持的奖品解锁范围。
 */
function isAwardUnlockScope(value: unknown): value is AwardUnlockScope {
    return value === "day" || value === "total";
}

/**
 * 校验活动奖品响应是否符合前端契约。
 */
function isActivityAward(value: unknown): value is ActivityAward {
    return isRecord(value)
        && isPositiveId(value.awardId)
        && typeof value.awardTitle === "string"
        && isNullableString(value.awardSubtitle)
        && isNonNegativeInteger(value.sort)
        && isNullableNonNegativeInteger(value.awardRuleLockCount)
        && isAwardUnlockScope(value.awardUnlockScope)
        && typeof value.isAwardUnlock === "boolean"
        && isNonNegativeInteger(value.waitUnLockCount)
        && isNullableNonNegativeInteger(value.awardRuleWeightCount)
        && typeof value.isAwardWeightUnlock === "boolean"
        && isNonNegativeInteger(value.waitAwardWeightCount);
}

/**
 * 校验活动抽奖响应是否符合前端契约。
 */
function isActivityDrawResult(value: unknown): value is ActivityDrawResult {
    return isRecord(value)
        && typeof value.orderId === "string"
        && isPositiveId(value.activityId)
        && isPositiveId(value.strategyId)
        && isPositiveId(value.awardId)
        && isNullableString(value.awardKey)
        && typeof value.awardTitle === "string";
}

type UnwrapOptions = {
    allowNullData?: boolean;
};

/**
 * 解开统一响应并检查 HTTP 状态和业务响应码。
 *
 * @param response HTTP 响应
 * @param options 不同接口的数据约束
 * @returns 经过统一响应校验的业务数据
 * @throws Error 响应格式错误、业务失败或缺少必要业务数据时抛出
 */
async function unwrap(response: Response, options: UnwrapOptions = {}): Promise<unknown> {
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

    if (envelope.data === null && !options.allowNullData) {
        throw new Error("接口未返回业务数据");
    }

    return envelope.data;
}

/**
 * 查询当前用户在指定活动下看到的奖品及解锁状态。
 *
 * @param activityId 活动标识
 * @param signal 请求取消信号
 * @returns 按活动维度返回的奖品列表
 */
export async function queryActivityAwards(activityId: number, signal?: AbortSignal): Promise<ActivityAward[]> {
    const params = new URLSearchParams({activityId: String(activityId)});
    const response = await fetch(`/api/activity/raffles/awards?${params}`, {
        cache: "no-store",
        signal,
    });
    const data = await unwrap(response);

    if (!Array.isArray(data) || !data.every(isActivityAward)) {
        throw new Error("活动奖品列表数据格式不正确");
    }

    return data;
}

/**
 * 执行一次活动抽奖。
 *
 * 网络异常时不会自动重试，因为 Java 服务可能已经完成抽奖和订单落库。
 *
 * @param activityId 活动标识
 * @returns Java 服务确认的中奖结果
 */
export async function drawActivityRaffle(activityId: number): Promise<ActivityDrawResult> {
    const body: ActivityDrawRequest = {activityId};

    let response: Response;

    try {
        response = await fetch("/api/activity/raffles", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(body),
        });
    } catch {
        throw new Error("网络连接异常，无法确认抽奖结果，请勿重复提交");
    }

    const data = await unwrap(response);

    if (!isActivityDrawResult(data)) {
        throw new Error("抽奖结果格式不正确，请先核实订单结果，不要重复提交");
    }

    return data;
}

/**
 * 装配指定活动的运行时缓存。
 *
 * 该请求只用于开发联调，不会创建活动订单或扣减抽奖次数。
 *
 * @param activityId 活动标识
 * @throws Error 网络异常、BFF 校验失败或后端装配失败时抛出
 */
export async function armoryActivityRaffle(activityId: number): Promise<void> {
    const body: ActivityArmoryRequest = {activityId};

    let response: Response;

    try {
        response = await fetch("/api/activity/raffles/armory", {
            method: "PUT",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(body),
        });
    } catch {
        throw new Error("网络连接异常，活动装配未完成");
    }

    await unwrap(response, {allowNullData: true});
}
