import {isPositiveDecimalIdentifier} from "@/lib/business-identifier";
import {forwardMarketForge} from "@/lib/server/market-forge-api";

type ActiveActivityEnvelope = {
    code: number;
    message: string;
    data: { activityId: string } | null;
};

/**
 * 根据当前部署渠道和来源解析已经发布的活动标识。
 *
 * 活动标识由 Java 服务的活动展台提供，运营后台发布新活动后无需修改环境变量或
 * 重启 Next.js。渠道和来源仍由服务端部署配置决定，浏览器不能切换到任意展台。
 *
 * 活动标识在前端协议中保持十进制字符串，不转换为 JavaScript number，
 * 避免超出 Number.MAX_SAFE_INTEGER 后发生精度丢失。
 *
 * @returns 当前生效活动的十进制字符串标识
 * @throws Error 渠道配置非法、没有生效活动、服务响应失败或活动标识格式非法时抛出
 */
export async function resolveActivityId(): Promise<string> {
    const channel = normalizePlacement(process.env.DEMO_RAFFLE_CHANNEL, "web", "DEMO_RAFFLE_CHANNEL");
    const source = normalizePlacement(process.env.DEMO_RAFFLE_SOURCE, "official", "DEMO_RAFFLE_SOURCE");
    const params = new URLSearchParams({channel, source});
    const response = await forwardMarketForge(`/api/v1/activity/stages/active?${params}`);
    const envelope = await parseActiveActivityResponse(response);

    if (!response.ok || envelope.code !== 0 || envelope.data === null) {
        throw new Error(envelope.message || `当前活动解析失败：HTTP ${response.status}`);
    }

    if (!isPositiveDecimalIdentifier(envelope.data.activityId)) {
        throw new Error("当前活动标识格式不合法");
    }

    return envelope.data.activityId;
}

/**
 * 标准化活动展台的渠道或来源配置。
 *
 * @param value 环境变量中的原始配置
 * @param fallback 未配置时采用的默认值
 * @param name 用于错误提示的环境变量名称
 * @returns 格式合法的渠道或来源
 * @throws Error 配置不符合小写字母开头的约束时抛出
 */
function normalizePlacement(value: string | undefined, fallback: string, name: string): string {
    const normalized = value?.trim() || fallback;
    if (!/^[a-z][a-z0-9_-]{0,31}$/.test(normalized)) {
        throw new Error(`${name} 格式不合法`);
    }
    return normalized;
}

/**
 * 解析活动展台接口的统一响应。
 *
 * @param response Java 服务返回的 HTTP 响应
 * @returns 结构已经完成基础校验的活动展台响应
 * @throws Error 响应不是合法的统一响应结构时抛出
 */
async function parseActiveActivityResponse(response: Response): Promise<ActiveActivityEnvelope> {
    const value: unknown = await response.json();

    if (!isRecord(value) || typeof value.code !== "number" || typeof value.message !== "string" || !("data" in value)) {
        throw new Error("当前活动接口响应结构错误");
    }

    if (value.data !== null && (!isRecord(value.data) || typeof value.data.activityId !== "string")) {
        throw new Error("当前活动接口数据结构错误");
    }

    return value as ActiveActivityEnvelope;
}

/**
 * 判断未知值是否为非空普通对象。
 *
 * @param value 待校验的未知值
 * @returns 非空且不是数组的对象返回 true
 */
function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}
