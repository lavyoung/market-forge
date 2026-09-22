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
     * 用户参与抽奖活动时扣减活动账户额度并生成抽奖单；如存在未使用订单则直接返回该订单。
     *
     * @param partakeRaffleActivity 用户参与活动入参
     * @return 抽奖参与订单
     */
    ActivityOrderEntity createRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivity);
}
