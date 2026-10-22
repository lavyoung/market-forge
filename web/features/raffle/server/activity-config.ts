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
 * @returns 当前生效且可由 JavaScript 安全表达的活动标识
 * @throws Error 渠道配置非法、没有生效活动、服务响应失败或活动标识越界时抛出
 */
export async function resolveActivityId(): Promise<number> {
    const channel = normalizePlacement(process.env.DEMO_RAFFLE_CHANNEL, "web", "DEMO_RAFFLE_CHANNEL");
    const source = normalizePlacement(process.env.DEMO_RAFFLE_SOURCE, "official", "DEMO_RAFFLE_SOURCE");
    const params = new URLSearchParams({channel, source});
    const response = await forwardMarketForge(`/api/v1/activity/stages/active?${params}`);
    const envelope = await parseActiveActivityResponse(response);

    if (!response.ok || envelope.code !== 0 || envelope.data === null) {
        throw new Error(envelope.message || `当前活动解析失败：HTTP ${response.status}`);
    }

    const activityId = Number(envelope.data.activityId);
    if (!/^\d+$/.test(envelope.data.activityId) || !Number.isSafeInteger(activityId) || activityId <= 0) {
        throw new Error("当前活动标识超出前端安全整数范围");
    }
    return activityId;
}

function normalizePlacement(value: string | undefined, fallback: string, name: string): string {
    const normalized = value?.trim() || fallback;
    if (!/^[a-z][a-z0-9_-]{0,31}$/.test(normalized)) {
        throw new Error(`${name} 格式不合法`);
    }
    return normalized;
}

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

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}
