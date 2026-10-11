package com.lavyoung.marketforge.domain.activity.service.quota.policy;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.types.exception.BusinessException;

/**
 * 活动 SKU 订单交易策略。
 *
 * <p>不同来源的活动 SKU 订单拥有不同的交易处理方式。
 * 例如行为返利 SKU 不需要支付，创建订单后可以直接完成并充值；
 * 积分兑换 SKU 需要先创建待支付订单，等积分扣减成功后再发货入账。</p>
 *
 * <p>该策略接口用于把“订单创建后的处理差异”从额度下单模板流程中拆出去，
 * 避免在模板服务中堆积交易类型判断。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public interface ITradePolicy {

    /**
     * 执行活动 SKU 订单交易处理。
     *
     * @param createQuotaOrderAggregate 创建额度订单聚合
     * @throws BusinessException 当订单保存、账户入账或交易状态变更失败时抛出
     */
    void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate);
}
