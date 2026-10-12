package com.lavyoung.marketforge.application.credit.model;

/**
 * 积分兑换活动 SKU 命令。
 *
 * <p>用于表达用户消耗积分兑换抽奖次数的应用层请求。该命令会被应用服务拆解成两个领域动作：
 * 先通过活动账户额度服务创建待支付 SKU 订单，再在积分账户中扣减积分。
 * 积分扣减成功后，系统通过可靠消息异步完成活动订单发货入账。</p>
 *
 * @param userId        用户标识
 * @param sku           活动 SKU，决定兑换后给哪个活动充值抽奖次数
 * @param creditAmount  本次需要消耗的积分数量，必须为正数
 * @param outBusinessNo 外部业务幂等号，同一次兑换请求必须保持稳定
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record ExchangeCreditForSkuCommand(
        String userId,
        Long sku,
        Long creditAmount,
        String outBusinessNo
) {
}
