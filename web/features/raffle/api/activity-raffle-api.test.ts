import {queryCalendarSignStatus, submitCalendarSign} from "@/features/raffle/api/activity-raffle-api";
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

describe("calendar sign API", () => {
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
});
