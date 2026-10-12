package com.lavyoung.marketforge.domain.activity.model.entity;


/**
 * 活动 SKU 发货单实体。
 *
 * <p>用于表达积分支付成功后，对待支付活动 SKU 订单进行发货入账的领域请求。
 * 发货流程会根据用户编号和外部业务幂等号定位待支付订单，再将订单置为完成并给活动账户充值。</p>
 *
 * @param userId        用户编号
 * @param outBusinessNo 外部业务幂等号，对应积分支付订单和活动 SKU 订单的关联键
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record DeliveryOrderEntity(
        String userId,
        String outBusinessNo
) {
}
