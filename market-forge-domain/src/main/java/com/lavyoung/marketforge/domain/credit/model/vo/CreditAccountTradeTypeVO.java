package com.lavyoung.marketforge.domain.credit.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 积分账户交易类型。
 * <p>
 * 交易类型只表达积分余额变动方向，不表达具体业务原因。具体原因由
 * {@link CreditAccountBusinessTypeVO} 描述，例如中奖发奖、签到返利或积分兑换。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Getter
@AllArgsConstructor
public enum CreditAccountTradeTypeVO {

    /**
     * 积分收入，例如签到返利、抽奖中奖发积分、运营补偿。
     */
    INCOME("income", "收入"),

    /**
     * 积分支出，例如积分兑换抽奖次数、兑换优惠券、兑换实物。
     */
    EXPENSE("expense", "支出"),

    ;

    private final String code;
    private final String desc;
}
