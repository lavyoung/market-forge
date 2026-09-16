/**
 * 后端统一响应结构。
 *
 * code 为 0 时表示业务执行成功。
 * 无返回数据的接口，其 data 可以为 null。
 */
export type ApiResponse<T> = {
    code: number;
    message: string;
    data: T | null;
}

/**
 * 策略奖品列表中的单个奖品。
 */
export type StrategyAward = {
    strategyId: number;
    awardId: number;
    awardTitle: string;
    awardCount: number;
    awardCountSurplus: number;
    awardRate: number;
    sort: number;
}

/**
 * Java 后端返回的抽奖结果。
 *
 * 奖品规则及说明字段允许为空。
 */
export type RaffleResult = {
    strategyId: number;
    awardId: number;
    awardKey: string;
    awardConfig: string;
    awardDesc: string;
}

/**
 * 浏览器提交给 Next.js 的抽奖请求。
 *
 * 不允许浏览器自行指定登录用户。
 */
export type DrawRequest = {
    strategyId: number;
}