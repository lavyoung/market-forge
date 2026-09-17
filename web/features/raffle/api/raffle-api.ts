import {ApiResponse, DrawRequest, RaffleResult, StrategyAward} from "@/features/raffle/model/raffle-types";

function isRecord(value: unknown): value is Record<string, unknown> {
    return (
        typeof value === "object" &&
        value !== null &&
        !Array.isArray(value)
    );
}

function isPositiveId(value: unknown): value is number {
    return (typeof value === "number" && Number.isSafeInteger(value) && value > 0);
}

function isNullableString(value: unknown): value is string | null {
    return value === null || typeof value === "string";
}

function isStrategyAward(value: unknown): value is StrategyAward {
    return (
        isRecord(value) &&
        isPositiveId(value.strategyId) &&
        isPositiveId(value.awardId) &&
        typeof value.awardTitle === "string" &&
        typeof value.awardCount === "number" &&
        typeof value.awardCountSurplus === "number" &&
        typeof value.awardRate === "number" &&
        typeof value.sort === "number" &&
        Number.isInteger(value.sort)
    );
}

function isRaffleResult(value: unknown): value is RaffleResult {
    return (
        isRecord(value) &&
        isPositiveId(value.strategyId) &&
        isPositiveId(value.awardId) &&
        isNullableString(value.awardKey) &&
        isNullableString(value.awardConfig) &&
        isNullableString(value.awardDesc)
    )
}

/**
 * 检查 HTTP 状态与统一业务响应，返回待验证的业务数据。
 */
async function unwrap(response: Response): Promise<unknown> {
    let result: unknown;

    try {
        result = await response.json();
    } catch {
        throw new Error("接口未返回合法JSON");
    }

    if (!isRecord(result) || typeof result.code !== "number" || typeof result.message !== "string" || !("data" in result)) {
        throw new Error("接口响应格式错误");
    }

    const envelope: ApiResponse<unknown> = {
        code: result.code,
        message: result.message,
        data: result.data,
    };

    if (!response.ok || envelope.code !== 0) {
        throw new Error(envelope.message || `请求失败：HTTP ${response.status}`);
    }

    if (envelope.data === null) {
        throw new Error("接口未返回业务数据");
    }

    return envelope.data;
}

/**
 * 加载策略奖品。
 */
export async function getStrategyAwards(strategyId: number, signal?: AbortSignal): Promise<StrategyAward[]> {
    const response = await fetch(`/api/raffles/awards?strategyId=${strategyId}`, {
        cache: "no-cache",
        signal,
    });
    const data = await unwrap(response);

    if (!Array.isArray(data) || !data.every(isStrategyAward)) {
        throw new Error("奖品列表数据格式不正确");
    }

    return data;
}

/**
 * 执行一次真实抽奖。
 *
 * 网络异常时不自动重试，因为服务端可能已经完成抽奖。
 */
export async function drawRaffle(strategyId: number): Promise<RaffleResult> {

    const body: DrawRequest = {
        strategyId,
    };

    let response: Response;

    try {
        response = await fetch("/api/raffles", {
            method: "POST",
            headers: {
                "content-type": "application/json",
            },
            body: JSON.stringify(body),
        });
    } catch {
        throw new Error("网络连接异常，未能确认抽奖结果，请勿重复提交");
    }

    const data = await unwrap(response);

    if (!isRaffleResult(data)) {
        throw new Error("抽奖结果格式不正确，请先核实本次抽奖结果，不要重复提交");
    }


    return data;
}