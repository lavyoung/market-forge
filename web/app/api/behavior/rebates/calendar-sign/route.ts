import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse} from "@/lib/server/market-forge-api";

/**
 * 读取当前联调环境配置的用户标识。
 *
 * 用户标识只允许从服务端环境变量读取，不能接收浏览器传入的 userId，
 * 防止调用方伪造身份查询或提交其他用户的签到。
 *
 * @returns 已去除首尾空格的用户标识，未配置时返回 null
 */
function resolveUserId(): string | null {
    const userId = process.env.DEMO_RAFFLE_USER_ID?.trim();
    return userId || null;
}

/**
 * 查询当前用户今天是否已经签到。
 *
 * BFF 将浏览器的无参数 GET 请求转换为 Java 服务需要的
 * userId 查询参数，并保持后端统一响应结构不变。
 *
 * @returns 今日签到状态的统一响应
 */
export async function GET(): Promise<Response> {
    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "签到查询接口尚未开放");
    }

    const userId = resolveUserId();

    if (!userId) {
        return errorResponse(500, 900_000_001, "开发用户未配置");
    }

    const params = new URLSearchParams({userId});

    try {
        const upstream = await forwardMarketForge(`/api/v1/behavior/rebates/calendar-sign/status?${params}`);
        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[behavior/rebates/calendar-sign] 查询签到状态失败", error);
        return badGatewayResponse("签到状态查询失败，请稍后再试");
    }
}

/**
 * 为当前用户提交今日签到。
 *
 * 后端按照用户标识和服务端日期生成每日幂等号，并根据签到返利配置
 * 创建积分、活动 SKU 等返利订单。该请求不会自动重试，避免在响应未知时
 * 重复提交签到。
 *
 * @returns 本次签到创建的返利订单号和消息事件号
 */
export async function POST(): Promise<Response> {
    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "签到接口尚未开放");
    }

    const userId = resolveUserId();

    if (!userId) {
        return errorResponse(500, 900_000_001, "开发用户未配置");
    }

    const params = new URLSearchParams({userId});

    try {
        const upstream = await forwardMarketForge(`/api/v1/behavior/rebates/calendar-sign?${params}`, {method: "POST"});
        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[behavior/rebates/calendar-sign] 提交签到失败", error);
        return badGatewayResponse("无法确认签到结果，请先刷新签到状态，不要重复提交");
    }
}