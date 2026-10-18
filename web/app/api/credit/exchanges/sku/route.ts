import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse} from "@/lib/server/market-forge-api";

type CreditExchangeBody = {
    sku?: unknown;
    requestId?: unknown;
};

const REQUEST_ID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/**
 * 判断解析后的 JSON 是否为可读取字段的普通对象。
 *
 * @param value 请求体 JSON
 * @returns 非空且不是数组的对象返回 true
 */
function isRequestBody(value: unknown): value is CreditExchangeBody {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

/**
 * 使用积分兑换活动 SKU。
 *
 * 浏览器只提交 SKU 和一次操作的 UUID。BFF 补充可信用户身份，并将 UUID
 * 转换为数据库长度范围内的 outBusinessNo。相同页面请求再次到达 BFF 时会
 * 得到相同幂等号，最终仍由 Java 服务和数据库唯一键保证业务幂等。
 *
 * @param request 浏览器发起的积分兑换请求
 * @returns Java 服务返回的活动订单号统一响应
 */
export async function POST(request: Request): Promise<Response> {
    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "积分兑换接口尚未开放");
    }

    const userId = process.env.DEMO_RAFFLE_USER_ID?.trim();
    if (!userId) {
        return errorResponse(500, 900_000_001, "开发用户未配置");
    }

    let parsedBody: unknown;

    try {
        parsedBody = await request.json();
    } catch {
        return errorResponse(400, 1001, "请求体必须是合法 JSON");
    }

    if (!isRequestBody(parsedBody)) {
        return errorResponse(400, 1001, "请求体必须是 JSON 对象");
    }

    const body = parsedBody;

    if (typeof body.sku !== "number" || !Number.isSafeInteger(body.sku) || body.sku <= 0) {
        return errorResponse(400, 1001, "sku 必须是安全范围内的正整数");
    }

    if (typeof body.requestId !== "string" || !REQUEST_ID_PATTERN.test(body.requestId)) {
        return errorResponse(400, 1001, "requestId 必须是合法 UUID");
    }

    const outBusinessNo = `credit-exchange-${body.requestId.toLowerCase()}`;

    try {
        const upstream = await forwardMarketForge("/api/v1/credit/exchanges/sku", {
            method: "POST",
            body: JSON.stringify({userId, sku: body.sku, outBusinessNo}),
        });
        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[credit/exchanges/sku] 积分兑换请求失败", error);
        return badGatewayResponse("后端服务暂时不可用，无法确认兑换结果，请勿重复提交");
    }
}
