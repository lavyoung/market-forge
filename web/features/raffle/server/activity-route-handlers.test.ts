import {GET as queryActivityAccount} from "@/app/api/activity/raffles/account/route";
import {PUT as armoryActivity} from "@/app/api/activity/raffles/armory/route";
import {GET as queryActivityAwards} from "@/app/api/activity/raffles/awards/route";
import {POST as drawActivity} from "@/app/api/activity/raffles/route";
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

const ACTIVITY_ID = "9007199254740993";
const SKU = "9007199254740994";

/**
 * 配置活动展台查询和后续业务接口的成功响应。
 */
function mockActiveActivity(): void {
    forwardMock
        .mockResolvedValueOnce(Response.json({code: 0, message: "成功", data: {activityId: ACTIVITY_ID}}))
        .mockResolvedValueOnce(Response.json({code: 0, message: "成功", data: {}}));
}

describe("activity raffle BFF route handlers", () => {
    beforeEach(() => {
        forwardMock.mockReset();
        toProxyMock.mockClear();
        vi.stubEnv("DEMO_RAFFLE_ENABLED", "true");
        vi.stubEnv("DEMO_RAFFLE_USER_ID", "user-001");
        vi.stubEnv("DEMO_RAFFLE_SKU", SKU);
    });

    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it("抽奖请求应原样转发超大活动标识和 SKU", async () => {
        // Given
        mockActiveActivity();
        const request = new Request("http://localhost/api/activity/raffles", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({activityId: ACTIVITY_ID}),
        });

        // When
        const response = await drawActivity(request);

        // Then
        expect(response.status).toBe(200);
        expect(forwardMock).toHaveBeenLastCalledWith("/api/v1/activity/raffles/draw", {
            method: "POST",
            body: JSON.stringify({userId: "user-001", activityId: ACTIVITY_ID, sku: SKU}),
        });
    });

    it("抽奖请求应拒绝旧式数字活动标识", async () => {
        // Given
        const request = new Request("http://localhost/api/activity/raffles", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({activityId: 100301}),
        });

        // When
        const response = await drawActivity(request);

        // Then
        expect(response.status).toBe(400);
        expect(forwardMock).not.toHaveBeenCalled();
    });

    it("账户查询应原样转发超大字符串活动标识", async () => {
        // Given
        mockActiveActivity();
        const request = new Request(`http://localhost/api/activity/raffles/account?activityId=${ACTIVITY_ID}`);

        // When
        const response = await queryActivityAccount(request);

        // Then
        expect(response.status).toBe(200);
        expect(forwardMock).toHaveBeenLastCalledWith("/api/v1/activity/raffles/account", {
            method: "POST",
            body: JSON.stringify({userId: "user-001", activityId: ACTIVITY_ID}),
        });
    });

    it("活动装配应原样转发超大字符串活动标识", async () => {
        // Given
        mockActiveActivity();
        const request = new Request("http://localhost/api/activity/raffles/armory", {
            method: "PUT",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({activityId: ACTIVITY_ID}),
        });

        // When
        const response = await armoryActivity(request);

        // Then
        expect(response.status).toBe(200);
        expect(forwardMock).toHaveBeenLastCalledWith(
            `/api/v1/activity/raffles/armory?activityId=${ACTIVITY_ID}`,
            {method: "PUT"},
        );
    });

    it("奖品查询应原样转发超大字符串活动标识", async () => {
        // Given
        mockActiveActivity();
        const request = new Request(`http://localhost/api/activity/raffles/awards?activityId=${ACTIVITY_ID}`);

        // When
        const response = await queryActivityAwards(request);

        // Then
        expect(response.status).toBe(200);
        expect(forwardMock).toHaveBeenLastCalledWith("/api/v1/activity/raffles/awards", {
            method: "POST",
            body: JSON.stringify({userId: "user-001", activityId: ACTIVITY_ID}),
        });
    });
});
