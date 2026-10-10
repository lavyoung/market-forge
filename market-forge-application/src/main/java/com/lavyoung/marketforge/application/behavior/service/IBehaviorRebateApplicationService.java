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

    /**
     * 创建用户今日日历签到返利订单。
     *
     * <p>该方法用于签到快捷入口。调用方只需要传入用户标识，
     * 应用层会统一生成签到行为类型和当天签到幂等号，避免 Controller 重复拼接业务编号。</p>
     *
     * @param userId 签到用户标识
     * @return 返利订单创建结果，包含订单号和待投递事件标识
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 用户标识为空、签到配置异常或订单创建失败时抛出
     */
    BehaviorRebateOrderCreateResult calendarSignRebate(String userId);

    /**
     * 查询用户今天是否已经完成日历签到返利。
     *
     * <p>该方法用于前端抽奖页初始化时判断签到按钮状态。
     * 应用层会基于当前日期生成签到幂等号，再交给领域服务判断对应返利订单是否存在。</p>
     *
     * @param userId 用户标识
     * @return 今天已经签到返回 {@code true}，否则返回 {@code false}
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 用户标识为空时抛出
     */
    boolean isCalendarSignRebate(String userId);
}
