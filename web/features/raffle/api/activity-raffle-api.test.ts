import {
    drawActivityRaffle,
    queryActivityAwards,
    queryCalendarSignStatus,
    submitCalendarSign,
} from "@/features/raffle/api/activity-raffle-api";
import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";

const fetchMock = vi.fn();

/**
 * 创建与 Java 统一返回结构一致的 HTTP 响应。
 *
 * @param data 响应业务数据
 * @param code 业务响应码
 * @param message 响应消息
 * @param status HTTP 状态码
 * @returns 可供 API 客户端解析的响应
 */
function apiResponse(data: unknown, code = 0, message = "成功", status = 200): Response {
    return new Response(JSON.stringify({code, message, data}), {
        status,
        headers: {"Content-Type": "application/json"},
    });
}

describe("activity raffle API", () => {
    beforeEach(() => {
        fetchMock.mockReset();
        vi.stubGlobal("fetch", fetchMock);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("应保留尚未签到的 false 状态", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse(false));

        // When
        const signed = await queryCalendarSignStatus();

        // Then
        expect(signed).toBe(false);
        expect(fetchMock).toHaveBeenCalledWith("/api/behavior/rebates/calendar-sign", {
            cache: "no-store",
            signal: undefined
        });
    });

    it("签到成功时应返回返利订单号和消息事件号", async () => {
        // Given
        const result = {orderIds: ["order-1", "order-2"], eventIds: ["event-1", "event-2"]};
        fetchMock.mockResolvedValue(apiResponse(result));

        // When
        const actual = await submitCalendarSign();

        // Then
        expect(actual).toEqual(result);
        expect(fetchMock).toHaveBeenCalledWith("/api/behavior/rebates/calendar-sign", {method: "POST"});
    });

    it("签到响应结构非法时应拒绝把数据交给页面", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse({orderIds: ["order-1"], eventIds: [""]}));

        // When
        const action = submitCalendarSign();

        // Then
        await expect(action).rejects.toThrow("签到结果格式不正确");
    });

    it("业务失败时应保留后端返回的错误信息", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse(null, 2001, "今天已经签到", 422));

        // When
        const action = submitCalendarSign();

        // Then
        await expect(action).rejects.toThrow("今天已经签到");
    });

    it("网络异常时不得自动重试签到请求", async () => {
        // Given
        fetchMock.mockRejectedValue(new TypeError("network unavailable"));

        // When
        const action = submitCalendarSign();

        // Then
        await expect(action).rejects.toThrow("无法确认签到结果，请先刷新签到状态");
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it("应接受包含超大字符串奖品标识的奖品列表", async () => {
        // Given
        const award = {
            awardId: "9007199254740993",
            awardTitle: "用户积分",
            awardSubtitle: null,
            sort: 1,
            awardRuleLockCount: null,
            awardUnlockScope: "total",
            isAwardUnlock: true,
            waitUnLockCount: 0,
            awardRuleWeightCount: null,
            isAwardWeightUnlock: true,
            waitAwardWeightCount: 0,
        };
        fetchMock.mockResolvedValue(apiResponse([award]));

        // When
        const awards = await queryActivityAwards("9007199254740993");

        // Then
        expect(awards).toEqual([award]);
    });

    it("奖品标识仍为数字时应拒绝错误响应", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse([{
            awardId: 101,
            awardTitle: "用户积分",
            awardSubtitle: null,
            sort: 1,
            awardRuleLockCount: null,
            awardUnlockScope: "total",
            isAwardUnlock: true,
            waitUnLockCount: 0,
            awardRuleWeightCount: null,
            isAwardWeightUnlock: true,
            waitAwardWeightCount: 0,
        }]));

        // When
        const action = queryActivityAwards("9007199254740993");

        // Then
        await expect(action).rejects.toThrow("活动奖品列表数据格式不正确");
    });

    it("应接受包含超大字符串业务标识的抽奖结果", async () => {
        // Given
        const result = {
            orderId: "ROPmv8av9hj0002z38fvn2949wettup",
            activityId: "9007199254740993",
            strategyId: "9007199254740994",
            awardId: "9007199254740995",
            awardKey: "credit",
            awardTitle: "用户积分",
        };
        fetchMock.mockResolvedValue(apiResponse(result));

        // When
        const actual = await drawActivityRaffle("9007199254740993");

        // Then
        expect(actual).toEqual(result);
    });

    it("抽奖结果包含数字业务标识时应拒绝错误响应", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse({
            orderId: "ROPmv8av9hj0002z38fvn2949wettup",
            activityId: 100301,
            strategyId: 100006,
            awardId: 101,
            awardKey: "credit",
            awardTitle: "用户积分",
        }));

        // When
        const action = drawActivityRaffle("9007199254740993");

        // Then
        await expect(action).rejects.toThrow("抽奖结果格式不正确，请先核实订单结果，不要重复提交");
    });
});
