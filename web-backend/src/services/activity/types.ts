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
