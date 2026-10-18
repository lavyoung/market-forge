import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse} from "@/lib/server/market-forge-api";

/**
 * 查询当前开发用户的积分账户。
 *
 * 浏览器不允许提交 userId。BFF 从服务端环境变量读取当前用户，避免调用方
 * 篡改参数后查询其他用户的积分余额，并将 Java 服务响应原样适配给页面。
 *
 * @returns Java 服务返回的积分账户统一响应
 */
export async function GET(): Promise<Response> {
    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "积分账户查询接口尚未开放");
    }

    const userId = process.env.DEMO_RAFFLE_USER_ID?.trim();
    if (!userId) {
        return errorResponse(500, 900_000_001, "开发用户未配置");
    }

    const params = new URLSearchParams({userId});

    try {
        const upstream = await forwardMarketForge(`/api/v1/credit/accounts?${params}`);
        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[credit/accounts] 查询积分账户失败", error);
        return badGatewayResponse();
    }
}
