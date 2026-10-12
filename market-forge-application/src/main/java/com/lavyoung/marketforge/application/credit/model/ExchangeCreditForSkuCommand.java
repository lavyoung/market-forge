package com.lavyoung.marketforge.application.credit.model;

/**
 * 积分兑换活动 SKU 命令。
 *
 * <p>该命令表达用户希望兑换的活动商品，不携带积分价格。
 * 应用服务会依据 {@code sku} 查询运营配置的服务端价格，
 * 再创建待支付活动订单并扣减相应积分。</p>
 *
 * @param userId        用户标识
 * @param sku           活动 SKU，决定兑换后给哪个活动充值抽奖次数
 * @param outBusinessNo 外部业务幂等号；同一次兑换请求必须保持稳定
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record ExchangeCreditForSkuCommand(
        String userId,
        Long sku,
        String outBusinessNo
) {
}
