package com.lavyoung.marketforge.application.behavior.service;

import com.lavyoung.marketforge.application.behavior.model.CreateBehaviorRebateOrderCommand;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateOrderResult;

/**
 * 用户行为返利应用服务。
 *
 * <p>负责编排用户行为返利入账用例，并在本地消息任务创建后触发事务后即时投递。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/09
 */
public interface IBehaviorRebateApplicationService {

    /**
     * 创建用户行为返利订单。
     *
     * @param command 创建用户行为返利订单命令
     * @return 返利订单创建结果，包含订单号和待投递事件标识
     */
    BehaviorRebateOrderResult createOrder(CreateBehaviorRebateOrderCommand command);
}
