import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse,} from "@/lib/server/market-forge-api";
import {resolveActivityId} from "@/features/raffle/server/activity-config";

/**
 * 判断未知值是否为普通对象。
 */
function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

/**
 * 接收浏览器活动抽奖请求，并注入可信用户和活动 SKU。
 *
 * 网络异常时不会重试，因为 Java 服务可能已经完成活动订单和中奖记录落库。
 */
export async function POST(request: Request): Promise<Response> {
    const contentType = request.headers.get("Content-Type") ?? "";

    if (!contentType.toLowerCase().includes("application/json")) {
        return errorResponse(415, 1003, "请求必须使用 application/json");
    }

    let input: unknown;

    try {
        input = await request.json();
    } catch {
        return errorResponse(400, 1003, "请求体不是合法 JSON");
    }

    if (!isRecord(input) || !Number.isSafeInteger(input.activityId) || Number(input.activityId) <= 0) {
        return errorResponse(400, 1001, "activityId 必须是安全范围内的正整数");
    }

    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "活动抽奖接口尚未开放");
    }

    const activityId = Number(input.activityId);
    const userId = process.env.DEMO_RAFFLE_USER_ID?.trim();
    const sku = Number(process.env.DEMO_RAFFLE_SKU);

    if (!userId) {
        return errorResponse(500, 900_000_001, "开发用户未配置");
    }

    if (!Number.isSafeInteger(sku) || sku <= 0) {
        return errorResponse(500, 900_000_001, "开发活动 SKU 未配置");
    }

    try {
        if (activityId !== await resolveActivityId()) {
            return errorResponse(400, 1001, "请求活动与当前生效活动不一致");
        }
        const upstream = await forwardMarketForge("/api/v1/activity/raffles/draw", {
            method: "POST",
            body: JSON.stringify({
                userId,
                activityId,
                sku,
            }),
        });

        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[activity/raffles] 活动抽奖请求异常", error);
        return badGatewayResponse("无法确认抽奖结果，请勿重复提交，应先核实本次抽奖是否已执行");
    }
}
