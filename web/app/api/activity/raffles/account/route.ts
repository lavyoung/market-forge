import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse,} from "@/lib/server/market-forge-api";
import {resolveActivityId} from "@/features/raffle/server/activity-config";

/**
 * 查询当前开发用户在指定活动下的抽奖额度。
 *
 * 浏览器只允许提交活动标识。用户标识由 BFF 从服务端环境变量读取，
 * 防止调用方通过篡改 userId 查询其他用户的活动账户。
 *
 * BFF 对浏览器暴露只读 GET 接口，再将请求适配为 Java 服务所需的
 * POST JSON 请求。
 *
 * @param request 浏览器发起的账户查询请求
 * @returns Java 服务返回的总、月、日活动额度响应
 */
export async function GET(request: Request): Promise<Response> {
    const rawActivityId = new URL(request.url).searchParams.get("activityId");
    const activityId = Number(rawActivityId);

    if (
        !rawActivityId
        || !/^\d+$/.test(rawActivityId)
        || !Number.isSafeInteger(activityId)
        || activityId <= 0
    ) {
        return errorResponse(
            400,
            1001,
            "activityId 必须是安全范围内的正整数",
        );
    }

    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "活动账户查询接口尚未开放");
    }

    const userId = process.env.DEMO_RAFFLE_USER_ID?.trim();
    if (!userId) {
        return errorResponse(500, 900_000_001, "开发用户未配置");
    }

    try {
        if (activityId !== await resolveActivityId()) {
            return errorResponse(400, 1001, "请求活动与当前生效活动不一致");
        }
        const upstream = await forwardMarketForge(
            "/api/v1/activity/raffles/account",
            {
                method: "POST",
                body: JSON.stringify({
                    userId,
                    activityId,
                }),
            },
        );

        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[activity/raffles/account] 查询活动账户失败", error);
        return badGatewayResponse();
    }
}
