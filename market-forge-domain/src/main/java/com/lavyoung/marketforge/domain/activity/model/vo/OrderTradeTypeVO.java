package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 活动 SKU 订单交易类型值对象。
 *
 * <p>用于描述活动 SKU 订单由哪种交易方式产生。
 * 不同交易方式决定订单创建后的处理策略：无支付交易可以直接完成并充值到账；
 * 积分支付交易需要先创建待支付订单，等待积分扣减成功后再发货入账。</p>
 *
 * <p>注意：该枚举描述的是“活动商品订单的交易方式”，不是积分账户流水的收入/支出方向。
 * 积分账户收入/支出由 {@code CreditAccountTradeTypeVO} 表达。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Getter
@AllArgsConstructor
public enum OrderTradeTypeVO {

    /**
     * 返利无支付交易。
     *
     * <p>例如签到返利得到活动 SKU，这类订单不需要用户额外支付积分，
     * 创建订单后可以直接完成并给活动账户增加抽奖额度。</p>
     */
    REBATE_NO_PAY("rebate_no_pay_trade", "返利无支付交易"),

    /**
     * 积分支付交易。
     *
     * <p>例如用户使用积分兑换活动 SKU，这类订单创建后应先进入待支付状态，
     * 等积分账户扣减成功后，再通过支付成功消息触发发货入账。</p>
     */
    CREDIT_PAY("credit_pay_trade", "积分支付交易");

    /**
     * 交易类型编码，持久化或用于 Spring 策略 Bean 查找。
     */
    private final String code;

    /**
     * 交易类型说明，用于日志和后台展示。
     */
    private final String desc;
}
