package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 活动订单状态值对象。
 * <p>
 * 保留活动额度订单的状态扩展点，具体状态由业务流程逐步补充。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Getter
@AllArgsConstructor
public enum OrderStateVO {

    /**
     * 待支付。
     *
     * <p>用户已创建积分兑换 SKU 订单，但积分尚未扣减成功，
     * 此时不能给用户活动账户增加抽奖额度。</p>
     */
    WAIT_PAY("wait_pay", "待支付"),

    /**
     * 已完成。
     *
     * <p>订单已经完成发货，用户活动账户、月账户、日账户额度已经入账。</p>
     */
    COMPLETE("complete", "已完成"),

    ;

    /**
     * 状态编码，持久化到活动订单表。
     */
    private final String code;

    /**
     * 状态说明，用于日志和后台展示。
     */
    private final String desc;
}
