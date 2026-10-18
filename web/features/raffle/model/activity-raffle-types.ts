/**
 * Java 服务统一响应结构。
 *
 * code 为 0 表示业务处理成功；失败响应和无返回值的成功响应允许 data 为空。
 */
export type ApiResponse<T> = {
    code: number;
    message: string;
    data: T | null;
};

/**
 * 奖品解锁次数统计范围。
 *
 * day 表示只统计活动当日次数，total 表示统计活动历史累计次数。
 */
export type AwardUnlockScope = "day" | "total";

/**
 * 当前用户视角下的活动奖品展示信息。
 *
 * 该类型与后端 ActivityAwardResponse 保持一致，不包含策略库存、中奖概率等内部配置。
 */
export type ActivityAward = {
    awardId: number;
    awardTitle: string;
    awardSubtitle: string | null;
    sort: number;
    awardRuleLockCount: number | null;
    awardUnlockScope: AwardUnlockScope;
    isAwardUnlock: boolean;
    waitUnLockCount: number;
    awardRuleWeightCount: number | null;
    isAwardWeightUnlock: boolean;
    waitAwardWeightCount: number;
};

/**
 * 活动抽奖结果。
 *
 * 中奖结果完全由 Java 服务决定，前端动画只能展示该结果，不能自行计算奖品。
 */
export type ActivityDrawResult = {
    orderId: string;
    activityId: number;
    strategyId: number;
    awardId: number;
    awardKey: string | null;
    awardTitle: string;
};

/**
 * 浏览器提交给 Next.js BFF 的活动抽奖请求。
 *
 * userId 和活动 SKU 不允许由浏览器指定，由 BFF 从可信登录状态或服务端配置中补充。
 */
export type ActivityDrawRequest = {
    activityId: number;
};

/**
 * 浏览器提交给 Next.js BFF 的活动装配请求。
 *
 * 装配只需要活动标识，不需要用户标识，因为它预热的是活动级公共缓存。
 */
export type ActivityArmoryRequest = {
    activityId: number;
};

/**
 * 当前用户在指定活动下的抽奖额度。
 *
 * 三个维度的额度会共同参与抽奖资格判断：
 * 总额度控制整个活动期间的可用次数，月额度和日额度分别限制
 * 当前月份与当前日期内的可用次数。
 */
export type ActivityAccount = {
    /** 活动期间累计获得的抽奖次数。 */
    totalCount: number;

    /** 活动期间当前剩余的抽奖次数。 */
    totalCountSurplus: number;

    /** 当日累计获得的抽奖次数。 */
    dayCount: number;

    /** 当日当前剩余的抽奖次数。 */
    dayCountSurplus: number;

    /** 当月累计获得的抽奖次数。 */
    monthCount: number;

    /** 当月当前剩余的抽奖次数。 */
    monthCountSurplus: number;
};

/**
 * 用户完成日历签到后创建的返利任务信息。
 *
 * 一次签到可能同时命中积分和活动 SKU 等多条返利配置，
 * 因此后端以列表返回返利订单号和可靠消息事件号。
 */
export type CalendarSignResult = {
    /** 本次签到创建的返利订单号。 */
    orderIds: string[];

    /** 本次签到创建的可靠消息事件标识。 */
    eventIds: string[];
};
