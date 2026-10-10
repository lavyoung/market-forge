package com.lavyoung.marketforge.application.behavior.service;

import com.lavyoung.marketforge.application.behavior.model.BehaviorRebateOrderCreateResult;
import com.lavyoung.marketforge.application.behavior.model.CreateBehaviorRebateOrderCommand;

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
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 行为类型无法识别、返利配置异常或订单创建失败时抛出
     */
    BehaviorRebateOrderCreateResult createOrder(CreateBehaviorRebateOrderCommand command);
}
