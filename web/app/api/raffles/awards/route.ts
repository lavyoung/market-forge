import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse} from "@/lib/server/market-forge-api";


export async function GET(request: Request): Promise<Response> {
    const rawStrategyId = new URL(request.url).searchParams.get("strategyId");
    const strategyId = Number(rawStrategyId);


    if (!rawStrategyId || !/^\d+$/.test(rawStrategyId) || !Number.isSafeInteger(strategyId) || strategyId <= 0) {
        return errorResponse(
            400,
            1001,
            "strategyId 必须是安全范围内的正整数",
        );
    }

    try {
        const upstream = await forwardMarketForge(
            `/api/v1/raffles/strategy/awardList?strategyId=${strategyId}`,
            {
                method: "GET",
            },
        );
        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[raffles/awards] 查询奖品失败", error);
        return badGatewayResponse();
    }
}