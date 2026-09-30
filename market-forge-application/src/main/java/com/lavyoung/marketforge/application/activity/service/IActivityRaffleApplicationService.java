package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;

/**
 * 活动抽奖应用用例入口。
 * <p>
 * 负责跨活动域、策略域和用户中奖域完成一次完整抽奖：先创建并消费活动参与订单，
 * 再执行策略抽奖，最后保存中奖记录和可靠消息任务。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IActivityRaffleApplicationService {

    /**
     * 执行一次活动抽奖。
     *
     * @param command 活动抽奖命令
     * @return 活动抽奖结果，包含活动订单号和命中奖品信息
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动额度不足、策略抽奖失败或中奖记录保存失败时抛出
     */
    ActivityRaffleResult raffle(ActivityRaffleCommand command);
}
