package com.lavyoung.marketforge.domain.activity.service.quota.policy.impl;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.quota.policy.ITradePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * 返利无支付交易策略。
 *
 * <p>用于处理签到返利、行为返利等无需用户额外支付积分的 SKU 入账场景。
 * 这类订单创建后即可视为完成，因此直接保存订单并给用户活动账户增加抽奖额度。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Service("rebate_no_pay_trade")
@RequiredArgsConstructor
public class RebateNoPayTradePolicy implements ITradePolicy {

    private final IActivityRepository activityRepository;

    /**
     * 保存无支付订单并立即完成活动账户充值。
     *
     * @param createQuotaOrderAggregate 创建活动 SKU 订单聚合
     */
    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        activityRepository.saveOrderAggregate(createQuotaOrderAggregate);
    }
}
