import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse,} from "@/lib/server/market-forge-api";
import {resolveActivityId} from "@/features/raffle/server/activity-config";
import {isPositiveDecimalIdentifier} from "@/lib/business-identifier";

/**
 * 判断未知值是否为普通对象。
 */
function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

/**
 * 接收开发联调页面的活动装配命令。
 *
 * 该接口只允许装配环境变量指定的演示活动，并将浏览器提交的
 * JSON 命令转换为 Java 服务所需的 PUT 查询参数。
 *
 * @param request 浏览器发起的活动装配请求
 * @returns Java 服务的统一响应
 */
export async function PUT(request: Request): Promise<Response> {
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

    if (!isRecord(input) || !isPositiveDecimalIdentifier(input.activityId)) {
        return errorResponse(400, 1001, "activityId 必须是十进制正整数字符串");
    }

    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "活动装配接口尚未开放");
    }

    const activityId = input.activityId;
    try {
        if (activityId !== await resolveActivityId()) {
            return errorResponse(400, 1001, "请求活动与当前生效活动不一致");
        }
        const path = `/api/v1/activity/raffles/armory?activityId=${encodeURIComponent(activityId)}`;
        const upstream = await forwardMarketForge(path, {method: "PUT"});

        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[activity/raffles/armory] 活动装配请求异常", error);
        return badGatewayResponse("活动装配失败，请稍后重试");
    }
}
