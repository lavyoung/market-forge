package com.lavyoung.marketforge.domain.activity.service.quota.policy.impl;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.quota.policy.ITradePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 积分支付交易策略。
 *
 * <p>用于处理用户使用积分兑换活动 SKU 的场景。
 * 该策略只创建待支付订单，不立即给活动账户充值；后续需要等待积分扣减成功消息，
 * 再由发货流程把订单置为完成并增加活动账户额度。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Service("credit_pay_trade")
@RequiredArgsConstructor
public class CreditPayTradePolicy implements ITradePolicy {

    private final IActivityRepository activityRepository;

    /**
     * 保存积分支付待支付订单。
     *
     * @param createQuotaOrderAggregate 创建活动 SKU 订单聚合
     */
    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        activityRepository.saveCreditPayOrder(createQuotaOrderAggregate);
    }
}
