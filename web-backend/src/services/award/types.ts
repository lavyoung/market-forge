/**
 * 发奖状态。
 */
export type AwardState = 'create' | 'wait_claim' | 'granting' | 'grant_failed' | 'completed';

/**
 * 用户中奖记录列表项。
 */
export interface UserAwardRecordItem {
    /** 全局唯一抽奖订单号。 */
    orderId: string;
    /** 用户唯一标识。 */
    userId: string;
    /** 活动标识。 */
    activityId: number;
    /** 抽奖策略标识。 */
    strategyId: number;
    /** 奖品标识。 */
    awardId: number;
    /** 奖品标题。 */
    awardTitle: string;
    /** 当前发奖状态。 */
    awardState: AwardState;
    /** 用户中奖时间。 */
    awardTime: string;
}

/**
 * ProTable 提交的中奖记录查询条件。
 */
export interface UserAwardRecordQuery {
    /** 当前页码，从 1 开始。 */
    current?: number;
    /** 每页记录数。 */
    pageSize?: number;
    /** 用户标识，精确匹配。 */
    userId?: string;
    /** 奖品标识，精确匹配。 */
    awardId?: number;
    /** 发奖状态。 */
    awardState?: AwardState;
    /** 中奖时间起点，包含该时间。 */
    startTime?: string;
    /** 中奖时间终点，不包含该时间。 */
    endTime?: string;
}

/**
 * ProTable 要求的中奖记录分页响应。
 */
export interface UserAwardRecordPage {
    /** 当前页中奖记录。 */
    data: UserAwardRecordItem[];
    /** 符合条件的记录总数。 */
    total: number;
    /** 请求是否成功。 */
    success: boolean;
}
