package com.lavyoung.marketforge.domain.credit.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 积分账户业务类型。
 * <p>
 * 业务类型用于说明积分变动发生的业务原因。它与交易方向分离：
 * {@link CreditAccountTradeTypeVO} 说明积分是收入还是支出，本枚举说明这笔变动来自哪个业务场景。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Getter
@AllArgsConstructor
public enum CreditAccountBusinessTypeVO {

    /**
     * 抽奖中奖后的积分奖品发放。
     */
    AWARD_GRANT("award_grant", "中奖发奖"),

    /**
     * 用户行为返利产生的积分入账，例如日历签到返利。
     */
    SIGN_REBATE("sign_rebate", "签到返利"),

    /**
     * 用户主动消耗积分兑换权益。
     */
    EXCHANGE("exchange", "积分兑换"),

    ;

    private final String code;
    private final String desc;
}
