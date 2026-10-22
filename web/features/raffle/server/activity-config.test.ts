import {resolveActivityId} from "@/features/raffle/server/activity-config";
import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";

const {forwardMock} = vi.hoisted(() => ({forwardMock: vi.fn()}));

vi.mock("@/lib/server/market-forge-api", () => ({forwardMarketForge: forwardMock}));

function stageResponse(activityId: string | null, code = 0, message = "成功", status = 200): Response {
    const data = activityId === null ? null : {activityId};
    return Response.json({code, message, data}, {status});
}

describe("activity stage resolver", () => {
    beforeEach(() => {
        forwardMock.mockReset();
    });

    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it("应按部署渠道和来源解析当前生效活动", async () => {
        // Given
        vi.stubEnv("DEMO_RAFFLE_CHANNEL", "app");
        vi.stubEnv("DEMO_RAFFLE_SOURCE", "campaign");
        forwardMock.mockResolvedValue(stageResponse("100301"));

        // When
        const activityId = await resolveActivityId();

        // Then
        expect(activityId).toBe(100301);
        expect(forwardMock).toHaveBeenCalledWith("/api/v1/activity/stages/active?channel=app&source=campaign");
    });

    it("未配置渠道和来源时应使用演示站点默认值", async () => {
        // Given
        forwardMock.mockResolvedValue(stageResponse("100301"));

        // When
        await resolveActivityId();

        // Then
        expect(forwardMock).toHaveBeenCalledWith("/api/v1/activity/stages/active?channel=web&source=official");
    });

    it("后端没有生效活动时应保留业务错误信息", async () => {
        // Given
        forwardMock.mockResolvedValue(stageResponse(null, 500_006_002, "当前渠道没有生效活动", 422));

        // When / Then
        await expect(resolveActivityId()).rejects.toThrow("当前渠道没有生效活动");
    });

    it("十八位活动标识超出安全整数范围时应中止页面初始化", async () => {
        // Given
        forwardMock.mockResolvedValue(stageResponse("9007199254740993"));

        // When / Then
        await expect(resolveActivityId()).rejects.toThrow("当前活动标识超出前端安全整数范围");
    });
});
