package com.lavyoung.marketforge.domain.activity.service;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.PartakeRaffleActivityEntity;

/**
 * 活动抽奖参与服务端口。
 * <p>
 * 负责校验活动状态、消费用户活动账户额度，并创建可用于抽奖的参与订单。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
public interface IRaffleActivityPartakeService {

    /**
     * 创建抽奖参与订单。
     * <p>
     * 该方法用于“抽奖次数出账”场景。用户实际参与抽奖时，服务应校验活动状态和有效时间，
     * 查询并复用未消费的参与订单；若不存在未消费订单，则校验并扣减用户活动账户的总、月、日额度，
     * 创建一笔可用于后续抽奖流程消费的参与订单。
     *
     * @param partakeRaffleActivity 用户参与活动入参，包含用户、活动和 SKU
     * @return 抽奖参与订单；存在未消费订单时返回既有订单
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 当参数非法、活动不可用、SKU 不匹配或账户额度不足时抛出
     */
    ActivityOrderEntity createRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivity);
}
