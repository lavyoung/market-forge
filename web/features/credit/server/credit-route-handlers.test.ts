import {GET as querySkuProducts} from "@/app/api/activity/sku-products/route";
import {GET as queryCreditAccount} from "@/app/api/credit/accounts/route";
import {POST as exchangeCredit} from "@/app/api/credit/exchanges/sku/route";
import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";

const {forwardMock, toProxyMock} = vi.hoisted(() => ({
    forwardMock: vi.fn(),
    toProxyMock: vi.fn(async (response: Response) => response),
}));

vi.mock("@/lib/server/market-forge-api", () => ({
    forwardMarketForge: forwardMock,
    toProxyResponse: toProxyMock,
    errorResponse(status: number, code: number, message: string) {
        return Response.json({code, message, data: null}, {status});
    },
    badGatewayResponse(message = "后端服务暂时不可用，请稍后再试") {
        return Response.json({code: 900_000_001, message, data: null}, {status: 502});
    },
}));

describe("credit BFF route handlers", () => {
    beforeEach(() => {
        forwardMock.mockReset();
        toProxyMock.mockClear();
        vi.stubEnv("DEMO_RAFFLE_ENABLED", "true");
        vi.stubEnv("DEMO_RAFFLE_USER_ID", "user-001");
        vi.stubEnv("DEMO_RAFFLE_ACTIVITY_ID", "100301");
        forwardMock.mockResolvedValue(Response.json({code: 0, message: "成功", data: {}}));
    });

    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it("积分账户查询应只使用服务端配置的用户身份", async () => {
        // When
        const response = await queryCreditAccount();

        // Then
        expect(response.status).toBe(200);
        expect(forwardMock).toHaveBeenCalledWith("/api/v1/credit/accounts?userId=user-001");
    });

    it("商品查询应拒绝访问非当前配置活动", async () => {
        // Given
        const request = new Request("http://localhost/api/activity/sku-products?activityId=100302");

        // When
        const response = await querySkuProducts(request);

        // Then
        expect(response.status).toBe(400);
        expect(forwardMock).not.toHaveBeenCalled();
    });

    it("积分兑换应由 BFF 补充用户身份并构造稳定幂等号", async () => {
        // Given
        const requestId = "9cd37031-489f-4fa4-8a08-e69bd27ad58c";
        const request = new Request("http://localhost/api/credit/exchanges/sku", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({sku: 9011, requestId, userId: "attacker"}),
        });

        // When
        const response = await exchangeCredit(request);

        // Then
        expect(response.status).toBe(200);
        expect(forwardMock).toHaveBeenCalledWith("/api/v1/credit/exchanges/sku", {
            method: "POST",
            body: JSON.stringify({
                userId: "user-001",
                sku: 9011,
                outBusinessNo: `credit-exchange-${requestId}`,
            }),
        });
    });

    it("积分兑换应拒绝非法请求标识", async () => {
        // Given
        const request = new Request("http://localhost/api/credit/exchanges/sku", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({sku: 9011, requestId: "not-a-uuid"}),
        });

        // When
        const response = await exchangeCredit(request);

        // Then
        expect(response.status).toBe(400);
        expect(forwardMock).not.toHaveBeenCalled();
    });

    it("积分兑换应拒绝不是对象的合法 JSON", async () => {
        // Given
        const request = new Request("http://localhost/api/credit/exchanges/sku", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: "null",
        });

        // When
        const response = await exchangeCredit(request);

        // Then
        expect(response.status).toBe(400);
        expect(forwardMock).not.toHaveBeenCalled();
    });
});
