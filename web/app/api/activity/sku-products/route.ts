import {badGatewayResponse, errorResponse, forwardMarketForge, toProxyResponse} from "@/lib/server/market-forge-api";
import {resolveActivityId} from "@/features/raffle/server/activity-config";

/**
 * 查询当前活动可使用积分兑换的 SKU 商品。
 *
 * 请求活动必须与服务端部署配置一致，浏览器不能借助查询参数读取其他活动的
 * 商品配置。商品价格、库存和到账次数完全来自 Java 服务，BFF 不自行计算。
 *
 * @param request 浏览器发起的商品查询请求
 * @returns Java 服务返回的活动 SKU 商品列表统一响应
 */
export async function GET(request: Request): Promise<Response> {
    const rawActivityId = new URL(request.url).searchParams.get("activityId");
    const activityId = Number(rawActivityId);

    if (!rawActivityId || !/^\d+$/.test(rawActivityId) || !Number.isSafeInteger(activityId) || activityId <= 0) {
        return errorResponse(400, 1001, "activityId 必须是安全范围内的正整数");
    }

    if (process.env.DEMO_RAFFLE_ENABLED !== "true") {
        return errorResponse(503, 900_000_001, "积分兑换商品接口尚未开放");
    }

    try {
        if (activityId !== await resolveActivityId()) {
            return errorResponse(400, 1001, "请求活动与当前生效活动不一致");
        }
        const params = new URLSearchParams({activityId: String(activityId)});
        const upstream = await forwardMarketForge(`/api/v1/activity/sku-products?${params}`);
        return toProxyResponse(upstream);
    } catch (error) {
        console.error("[activity/sku-products] 查询积分兑换商品失败", error);
        return badGatewayResponse();
    }
}
