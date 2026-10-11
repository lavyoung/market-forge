package com.lavyoung.marketforge.domain.credit.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 积分账户订单状态。
 * <p>
 * 积分订单用于记录一次积分账户变动，MPV先采用同步入账模型：
 * 订单创建成功并完成账户更新后，状态直接为 {@link #COMPLETED}。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Getter
@AllArgsConstructor
public enum CreditAccountOrderStateVO {

    /**
     * 积分订单已完成。
     */
    COMPLETED("completed", "已完成"),

    /**
     * 积分订单处理失败。
     */
    FAILED("failed", "失败");

    private final String code;
    private final String desc;
}
