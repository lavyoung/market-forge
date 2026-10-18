/**
 * 当前用户的积分账户快照。
 *
 * userId 用于确认服务端返回的是当前开发用户的数据；availableAmount
 * 表示此刻可以参与积分兑换的余额，最终能否扣减仍以 Java 服务事务校验为准。
 */
export type CreditAccount = {
    userId: string;
    availableAmount: number;
};

/**
 * 活动下可使用积分兑换的 SKU 商品。
 *
 * 商品同时携带库存、积分价格和兑换成功后增加的总/月/日抽奖次数，
 * 页面只负责展示这些服务端配置，不自行推导价格或到账额度。
 */
export type ActivitySkuProduct = {
    sku: number;
    activityId: number;
    stockCount: number;
    stockCountSurplus: number;
    totalCount: number;
    dayCount: number;
    monthCount: number;
    creditAmount: number;
};

/**
 * 浏览器提交给积分兑换 BFF 的请求。
 *
 * userId 和后端 outBusinessNo 不允许由浏览器直接指定。requestId 只表示
 * 一次页面操作，BFF 会将其转换为长度受控的业务幂等号并补充可信用户身份。
 */
export type CreditExchangeRequest = {
    sku: number;
    requestId: string;
};
