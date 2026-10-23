import {
    ActivityAccount,
    ActivityArmoryRequest,
    ActivityAward,
    ActivityDrawRequest,
    ActivityDrawResult,
    ApiResponse,
    AwardUnlockScope,
    CalendarSignResult,
} from "@/features/raffle/model/activity-raffle-types";
import {isPositiveDecimalIdentifier} from "@/lib/business-identifier";

/**
 * 判断未知值是否为普通对象。
 */
function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
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
 * 判断未知值是否为非空字符串数组。
 *
 * @param value Java 服务返回的未知数据
 * @returns 每个数组元素都是非空字符串时返回 true
 */
function isNonEmptyStringArray(value: unknown): value is string[] {
    return Array.isArray(value) && value.every((item) => typeof item === "string" && item.trim().length > 0);
}

/**
 * 判断未知值是否为后端支持的奖品解锁范围。
 */
function isAwardUnlockScope(value: unknown): value is AwardUnlockScope {
    return value === "day" || value === "total";
}

/**
 * 校验活动奖品响应是否符合前端契约。
 *
 * 奖品标识必须是十进制正整数字符串；排序、解锁次数和权重门槛
 * 仍然按照数值字段校验。
 *
 * @param value Java 服务返回的未知数据
 * @returns 数据符合活动奖品响应契约时返回 true
 */
function isActivityAward(value: unknown): value is ActivityAward {
    return isRecord(value)
        && isPositiveDecimalIdentifier(value.awardId)
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
 * 校验活动账户响应是否符合前端契约。
 *
 * TypeScript 类型不会校验网络返回的 JSON，因此六个额度字段都必须
 * 在运行时确认为非负安全整数，防止错误数据进入额度和权重计算。
 *
 * @param value Java 服务返回的未知数据
 * @returns 数据符合活动账户契约时返回 true
 */
function isActivityAccount(value: unknown): value is ActivityAccount {
    return isRecord(value)
        && isNonNegativeInteger(value.totalCount)
        && isNonNegativeInteger(value.totalCountSurplus)
        && isNonNegativeInteger(value.dayCount)
        && isNonNegativeInteger(value.dayCountSurplus)
        && isNonNegativeInteger(value.monthCount)
        && isNonNegativeInteger(value.monthCountSurplus);
}

/**
 * 校验日历签到响应是否符合前端契约。
 *
 * @param value Java 服务返回的未知数据
 * @returns 数据包含合法订单号和事件号列表时返回 true
 */
function isCalendarSignResult(value: unknown): value is CalendarSignResult {
    return isRecord(value)
        && isNonEmptyStringArray(value.orderIds)
        && isNonEmptyStringArray(value.eventIds);
}

/**
 * 校验活动抽奖结果是否符合前端契约。
 *
 * 订单号和业务标识都保持字符串。活动、策略和奖品标识还必须满足
 * 十进制正整数格式，避免空字符串或任意文本进入页面状态。
 *
 * @param value Java 服务返回的未知数据
 * @returns 数据符合活动抽奖结果契约时返回 true
 */
function isActivityDrawResult(value: unknown): value is ActivityDrawResult {
    return isRecord(value)
        && typeof value.orderId === "string"
        && value.orderId.trim().length > 0
        && isPositiveDecimalIdentifier(value.activityId)
        && isPositiveDecimalIdentifier(value.strategyId)
        && isPositiveDecimalIdentifier(value.awardId)
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
export async function queryActivityAwards(activityId: string, signal?: AbortSignal): Promise<ActivityAward[]> {
    const params = new URLSearchParams({activityId});
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
 * 查询当前用户在指定活动下的抽奖额度。
 *
 * userId 不由浏览器提交，而是由 Next.js BFF 从可信服务端配置中补充。
 * 响应不会被浏览器缓存，确保签到或抽奖后可以重新读取最新额度。
 *
 * @param activityId 活动标识
 * @param signal 请求取消信号
 * @returns 当前用户的总、月、日活动额度
 * @throws Error 网络失败、业务失败或响应结构不符合契约时抛出
 */
export async function queryActivityAccount(activityId: string, signal?: AbortSignal): Promise<ActivityAccount> {
    const params = new URLSearchParams({activityId});
    const response = await fetch(`/api/activity/raffles/account?${params}`, {cache: "no-store", signal});
    const data = await unwrap(response);

    if (!isActivityAccount(data)) {
        throw new Error("活动账户额度数据格式不正确");
    }

    return data;
}

/**
 * 查询当前用户今天是否已经完成日历签到。
 *
 * userId 由 Next.js BFF 从可信服务端配置补充，浏览器不提交用户身份。
 *
 * @param signal 请求取消信号
 * @returns 今天已经签到返回 true，否则返回 false
 * @throws Error 网络失败、业务失败或响应不是布尔值时抛出
 */
export async function queryCalendarSignStatus(signal?: AbortSignal): Promise<boolean> {
    const response = await fetch("/api/behavior/rebates/calendar-sign", {cache: "no-store", signal});
    const data = await unwrap(response);

    if (typeof data !== "boolean") {
        throw new Error("签到状态数据格式不正确");
    }

    return data;
}

/**
 * 为当前用户提交今日签到。
 *
 * 该方法不会自动重试。网络中断时后端可能已经完成签到，
 * 调用方应重新查询签到状态，而不是直接再次提交。
 *
 * @returns 本次签到创建的返利订单号和消息事件号
 * @throws Error 网络失败、业务失败或签到结果结构错误时抛出
 */
export async function submitCalendarSign(): Promise<CalendarSignResult> {
    let response: Response;

    try {
        response = await fetch("/api/behavior/rebates/calendar-sign", {method: "POST"});
    } catch {
        throw new Error("网络连接异常，无法确认签到结果，请先刷新签到状态");
    }

    const data = await unwrap(response);

    if (!isCalendarSignResult(data)) {
        throw new Error("签到结果格式不正确，请先刷新签到状态");
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
export async function drawActivityRaffle(activityId: string): Promise<ActivityDrawResult> {
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
export async function armoryActivityRaffle(activityId: string): Promise<void> {
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
