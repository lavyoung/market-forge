package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.OrderTradeTypeVO;

/**
 * 活动 SKU 订单创建实体。
 *
 * <p>用于表达一次给用户创建活动 SKU 订单的领域请求。该实体既可以承载
 * 行为返利产生的无支付 SKU 入账，也可以承载积分支付兑换 SKU 的待支付下单。</p>
 *
 * @param userId         用户编号
 * @param sku            活动 SKU，决定给哪个活动增加抽奖额度
 * @param outBusinessNo  外部业务幂等号，同一业务请求必须保持稳定
 * @param orderTradeType 订单交易类型，决定订单创建后的处理策略
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
public record SkuRechargeEntity(
        String userId,
        Long sku,
        String outBusinessNo,
        OrderTradeTypeVO orderTradeType
) {

}
