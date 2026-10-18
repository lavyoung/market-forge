import {
    CreditExchangeOutcomeUnknownError,
    exchangeCreditForSku,
    queryActivitySkuProducts,
    queryCreditAccount,
} from "@/features/credit/api/credit-api";
import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";

const fetchMock = vi.fn();

/**
 * 创建与 Java 统一响应结构一致的 HTTP 响应。
 *
 * @param data 响应业务数据
 * @param code 业务响应码
 * @param message 响应消息
 * @param status HTTP 状态码
 * @returns 可供积分 API 客户端解析的响应
 */
function apiResponse(data: unknown, code = 0, message = "成功", status = 200): Response {
    return new Response(JSON.stringify({code, message, data}), {
        status,
        headers: {"Content-Type": "application/json"},
    });
}

describe("credit API", () => {
    beforeEach(() => {
        fetchMock.mockReset();
        vi.stubGlobal("fetch", fetchMock);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("应查询并校验当前用户积分账户", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse({userId: "user-001", availableAmount: 1200}));

        // When
        const account = await queryCreditAccount();

        // Then
        expect(account).toEqual({userId: "user-001", availableAmount: 1200});
        expect(fetchMock).toHaveBeenCalledWith("/api/credit/accounts", {cache: "no-store", signal: undefined});
    });

    it("积分超过 JavaScript 安全整数范围时应暴露契约错误", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse({userId: "user-001", availableAmount: Number.MAX_SAFE_INTEGER + 1}));

        // When
        const action = queryCreditAccount();

        // Then
        await expect(action).rejects.toThrow("积分账户数据格式不正确");
    });

    it("应返回活动下全部合法积分兑换商品", async () => {
        // Given
        const products = [{
            sku: 9011,
            activityId: 100301,
            stockCount: 100,
            stockCountSurplus: 90,
            totalCount: 10,
            dayCount: 2,
            monthCount: 10,
            creditAmount: 100,
        }];
        fetchMock.mockResolvedValue(apiResponse(products));

        // When
        const actual = await queryActivitySkuProducts(100301);

        // Then
        expect(actual).toEqual(products);
        expect(fetchMock).toHaveBeenCalledWith("/api/activity/sku-products?activityId=100301", {
            cache: "no-store",
            signal: undefined
        });
    });

    it("商品积分价格为零时应拒绝交给页面", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse([{
            sku: 9011,
            activityId: 100301,
            stockCount: 100,
            stockCountSurplus: 90,
            totalCount: 10,
            dayCount: 2,
            monthCount: 10,
            creditAmount: 0,
        }]));

        // When
        const action = queryActivitySkuProducts(100301);

        // Then
        await expect(action).rejects.toThrow("积分兑换商品数据格式不正确");
    });

    it("应使用 SKU 和同一次请求标识提交兑换", async () => {
        // Given
        const requestId = "9cd37031-489f-4fa4-8a08-e69bd27ad58c";
        fetchMock.mockResolvedValue(apiResponse("activity-order-001"));

        // When
        const orderId = await exchangeCreditForSku(9011, requestId);

        // Then
        expect(orderId).toBe("activity-order-001");
        expect(fetchMock).toHaveBeenCalledWith("/api/credit/exchanges/sku", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({sku: 9011, requestId}),
        });
    });

    it("服务端明确拒绝兑换时应保留业务错误", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse(null, 3001, "用户积分不足", 422));

        // When
        const action = exchangeCreditForSku(9011, "9cd37031-489f-4fa4-8a08-e69bd27ad58c");

        // Then
        await expect(action).rejects.toThrow("用户积分不足");
        await expect(action).rejects.not.toBeInstanceOf(CreditExchangeOutcomeUnknownError);
    });

    it("网络异常时应标记结果未知且不得自动重试", async () => {
        // Given
        fetchMock.mockRejectedValue(new TypeError("network unavailable"));

        // When
        const action = exchangeCreditForSku(9011, "9cd37031-489f-4fa4-8a08-e69bd27ad58c");

        // Then
        await expect(action).rejects.toBeInstanceOf(CreditExchangeOutcomeUnknownError);
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it("BFF 返回服务端异常时也应标记兑换结果未知", async () => {
        // Given
        fetchMock.mockResolvedValue(apiResponse(null, 900_000_001, "无法确认兑换结果，请勿重复提交", 502));

        // When
        const action = exchangeCreditForSku(9011, "9cd37031-489f-4fa4-8a08-e69bd27ad58c");

        // Then
        await expect(action).rejects.toBeInstanceOf(CreditExchangeOutcomeUnknownError);
        await expect(action).rejects.toThrow("无法确认兑换结果，请勿重复提交");
    });
});
