import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse,} from "@/lib/server/market-forge-api";
import {resolveActivityId} from "@/features/raffle/server/activity-config";
import {isPositiveDecimalIdentifier} from "@/lib/business-identifier";

/**
 * 查询演示用户在指定活动下看到的奖品与解锁状态。
 *
 * userId 由服务端配置提供，浏览器只能选择公开的活动标识。
 */
export async function GET(request: Request): Promise<Response> {
    const activityId = new URL(request.url).searchParams.get("activityId");

    if (!isPositiveDecimalIdentifier(activityId)) {
        return errorResponse(400, 1001, "activityId 必须是十进制正整数字符串");
    }

    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "活动查询接口尚未开放");
    }

    const userId = process.env.DEMO_RAFFLE_USER_ID?.trim();
    if (!userId) {
        return errorResponse(500, 900_000_001, "开发用户未配置");
    }

    try {
        if (activityId !== await resolveActivityId()) {
            return errorResponse(400, 1001, "请求活动与当前生效活动不一致");
        }
        const upstream = await forwardMarketForge("/api/v1/activity/raffles/awards", {
            method: "POST",
            body: JSON.stringify({
                userId,
                activityId,
            }),
        });

        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[activity/raffles/awards] 查询活动奖品失败", error);
        return badGatewayResponse();
    }
}
