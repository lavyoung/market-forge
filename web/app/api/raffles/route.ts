import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse} from "@/lib/server/market-forge-api";

function isRecord(value: unknown): value is Record<string, number> {
    return (
        typeof value === "object" && value !== null && !Array.isArray(value)
    );
}

export async function POST(request: Request): Promise<Response> {
    const contentType = request.headers.get("Content-Type") ?? "";

    if (!contentType.toLowerCase().includes("application/json")) {
        return errorResponse(
            415,
            1003,
            "请求必须使用 application/json",
        );
    }

    let input: unknown;

    try {
        input = await request.json();
    } catch {
        return errorResponse(400, 1003, "请求体不是合法JSON")
    }

    if (!isRecord(input) || typeof input.strategyId !== "number" || !Number.isSafeInteger(input.strategyId) ||
        input.strategyId <= 0) {
        return errorResponse(400, 1001, "strategyId 必须是安全范围内的正整数");
    }

    // 目前没有接入登录，这条抽奖路径仅允许本地开发使用。
    const demoRaffleEnabled = process.env.DEMO_RAFFLE_ENABLED === "true";
    if (!demoRaffleEnabled) {
        return errorResponse(
            503,
            900_000_001,
            "抽奖接口尚未开放",
        );
    }

    const userId = process.env.DEMO_RAFFLE_USER_ID?.trim();

    if (!userId) {
        return errorResponse(
            500,
            900_000_001,
            "开发用户未配置",
        );
    }

    try {
        const upstream = await forwardMarketForge("/api/v1/raffles", {
            method: "POST",
            body: JSON.stringify({
                userId,
                strategyId: input.strategyId,
            }),
        },);
        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[raffles] 抽奖请求异常", error);

        return badGatewayResponse(
            "未能确认抽奖结果，请勿重复提交，应先核实本次抽奖是否已执行",
        );
    }

}
