/**
 * 活动订单状态。
 */
export type ActivityOrderState = 'create' | 'used' | 'cancel' | 'wait_pay' | 'complete';

/**
 * 活动订单列表项。
 *
 * 业务标识使用字符串，避免 JavaScript number 无法安全承载 Java long。
 */
export interface ActivityOrderItem {
    /** 用户唯一标识。 */
    userId: string;
    /** 活动唯一标识。 */
    activityId: string;
    /** 活动 SKU。 */
    sku: string;
    /** 下单时保存的活动名称快照。 */
    activityName: string;
    /** 抽奖策略唯一标识。 */
    strategyId: string;
    /** 全业务唯一订单号。 */
    orderId: string;
    /** 业务下单时间。 */
    orderTime: string;
    /** 订单授予的总抽奖次数。 */
    totalCount: number;
    /** 订单授予的日抽奖次数。 */
    dayCount: number;
    /** 订单授予的月抽奖次数。 */
    monthCount: number;
    /** 当前订单状态。 */
    orderState: ActivityOrderState;
    /** 外部业务幂等号。 */
    outBusinessNo: string | null;
    /** 数据创建时间。 */
    createTime: string;
    /** 数据最后更新时间。 */
    updateTime: string;
}

/**
 * ProTable 提交的活动订单查询条件。
 */
export interface ActivityOrderQuery {
    /** ProTable 当前页，从 1 开始。 */
    current?: number;
    /** 每页记录数。 */
    pageSize?: number;
    /** 用户唯一标识，精确匹配。 */
    userId?: string;
    /** 活动唯一标识。 */
    activityId?: string;
    /** 全业务唯一订单号，精确匹配。 */
    orderId?: string;
    /** 订单状态。 */
    orderState?: ActivityOrderState;
    /** 下单时间起点，包含该时间。 */
    startTime?: string;
    /** 下单时间终点，不包含该时间。 */
    endTime?: string;
}

/**
 * ProTable 要求的分页响应结构。
 */
export interface ActivityOrderPage {
    /** 当前页订单。 */
    data: ActivityOrderItem[];
    /** 符合查询条件的总记录数。 */
    total: number;
    /** 请求是否成功。 */
    success: boolean;
}

/**
 * 活动展台生命周期状态。
 */
export type ActivityStageState = 'create' | 'active' | 'expire';

/**
 * 活动展台列表项及详情。
 */
export interface ActivityStageItem {
    /** 展台业务标识。 */
    stageId: string;
    /** 投放渠道。 */
    channel: string;
    /** 流量来源。 */
    source: string;
    /** 当前展台关联的活动标识。 */
    activityId: string;
    /** 展台生命周期状态。 */
    state: ActivityStageState;
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
 * ProTable 提交的活动展台查询条件。
 */
export interface ActivityStageQuery {
    current?: number;
    pageSize?: number;
    channel?: string;
    source?: string;
    state?: ActivityStageState;
}

/**
 * ProTable 所需的活动展台分页结果。
 */
export interface ActivityStagePage {
    data: ActivityStageItem[];
    total: number;
    success: boolean;
}

/**
 * 创建或编辑活动展台的业务字段。
 */
export interface ActivityStageFormValues {
    channel: string;
    source: string;
    activityId: string;
}
