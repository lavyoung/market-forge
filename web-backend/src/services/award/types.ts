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
    activityId: string;
    /** 抽奖策略标识。 */
    strategyId: string;
    /** 奖品标识。 */
    awardId: string;
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
    awardId?: string;
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

/**
 * 奖品定义生命周期状态。
 */
export type AwardDefinitionState = 'draft' | 'active' | 'disabled';

/**
 * 奖品领取模式。
 */
export type AwardClaimMode = 'automatic' | 'manual';

/**
 * 奖品定义列表项及详情。
 */
export interface AwardDefinitionItem {
    /** 奖品业务标识。服务端使用字符串返回，避免 JavaScript 整数精度丢失。 */
    awardId: string;
    /** 奖品业务编码。 */
    awardCode: string;
    /** 奖品展示名称。 */
    awardName: string;
    /** 发奖处理器类型编码。 */
    awardType: string;
    /** 奖品领取模式。 */
    claimMode: AwardClaimMode;
    /** 第三方履约供应商编码，内部奖品可以为空。 */
    providerCode: string | null;
    /** 奖品发放配置 JSON。 */
    awardConfig: string;
    /** 发放配置结构版本。 */
    configVersion: number;
    /** 奖品说明。 */
    awardDesc: string | null;
    /** 奖品定义生命周期状态。 */
    state: AwardDefinitionState;
    /** 乐观锁版本。 */
    version: number;
    /** 创建人。 */
    createdBy: string;
    /** 最后修改人。 */
    updatedBy: string;
    /** 创建时间。 */
    createTime: string;
    /** 最后修改时间。 */
    updateTime: string;
}

/**
 * ProTable 提交的奖品定义查询条件。
 */
export interface AwardDefinitionQuery {
    /** 当前页码，从 1 开始。 */
    current?: number;
    /** 每页记录数。 */
    pageSize?: number;
    /** 奖品业务编码，精确匹配。 */
    awardCode?: string;
    /** 奖品名称前缀。 */
    awardName?: string;
    /** 发奖处理器类型编码。 */
    awardType?: string;
    /** 奖品生命周期状态。 */
    state?: AwardDefinitionState;
}

/**
 * ProTable 所需的奖品定义分页结果。
 */
export interface AwardDefinitionPage {
    /** 当前页奖品定义。 */
    data: AwardDefinitionItem[];
    /** 满足条件的总记录数。 */
    total: number;
    /** 请求是否成功。 */
    success: boolean;
}

/**
 * 创建或编辑奖品定义的业务字段。
 */
export interface AwardDefinitionFormValues {
    awardCode: string;
    awardName: string;
    awardType: string;
    claimMode: AwardClaimMode;
    providerCode?: string;
    awardConfig: string;
    configVersion: number;
    awardDesc?: string;
}
