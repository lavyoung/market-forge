package com.lavyoung.marketforge.domain.credit.model.aggregate;

import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;

/**
 * 积分账户调整聚合。
 * <p>
 * 表示一次需要原子完成的积分账户变动。仓储实现需要在同一事务中完成：
 * 创建积分账户订单，以及新增或更新用户积分账户余额。
 *
 * @param order 积分账户订单，记录本次积分变动的来源、数量和幂等号
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record CreditAccountAdjustAggregate(
        CreditAccountOrderEntity order
) {
}
