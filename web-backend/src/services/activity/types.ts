/**
 * 活动订单状态。
 *
 * `create`、`used`、`cancel` 属于用户抽奖参与订单；
 * `wait_pay`、`complete` 属于活动 SKU 额度订单。
 */
export type ActivityOrderState = 'create' | 'used' | 'cancel' | 'wait_pay' | 'complete';

/**
 * 活动订单列表项。
 *
 * 所有 ID 均使用字符串，避免 JavaScript `number` 无法安全承载 Java `long`。
 */
export interface ActivityOrderItem {
    /** 数据库主键。 */
    id: string;
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
    /** 订单创建时间。 */
    orderTime: string;
    /** 订单增加的总额度。 */
    totalCount: number;
    /** 订单增加的日额度。 */
    dayCount: number;
    /** 订单增加的月额度。 */
    monthCount: number;
    /** 当前订单状态。 */
    orderState: ActivityOrderState;
    /** 外部业务幂等号。 */
    outBusinessNo?: string;
    /** 数据创建时间。 */
    createTime: string;
    /** 数据最后更新时间。 */
    updateTime: string;
}

/**
 * 活动订单分页查询条件。
 */
export interface ActivityOrderQuery {
    /** 当前页，从 1 开始。 */
    current?: number;
    /** 每页记录数。 */
    pageSize?: number;
    /** 用户唯一标识，支持模糊匹配。 */
    userId?: string;
    /** 活动唯一标识。 */
    activityId?: string;
    /** 订单号，支持模糊匹配。 */
    orderId?: string;
    /** 订单状态。 */
    orderState?: ActivityOrderState;
}

/**
 * ProTable 使用的活动订单分页响应。
 */
export interface ActivityOrderPage {
    /** 当前页订单数据。 */
    data: ActivityOrderItem[];
    /** 符合查询条件的记录总数。 */
    total: number;
    /** 请求是否成功。 */
    success: boolean;
}
