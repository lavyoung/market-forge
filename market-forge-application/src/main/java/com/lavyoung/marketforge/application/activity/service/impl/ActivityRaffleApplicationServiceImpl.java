package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 活动抽奖应用服务默认实现。
 * <p>
 * 本类只做跨域用例编排：活动域负责资格订单和额度，策略域负责命中奖品，
 * 用户中奖域负责中奖记录和可靠消息任务落库。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ActivityRaffleApplicationServiceImpl implements IActivityRaffleApplicationService {

    private final IRaffleActivityPartakeService activityPartakeService;
    private final IStrategyRaffleService strategyRaffleService;
    private final ActivityRaffleSettlementTransaction settlementTransaction;

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityRaffleResult raffle(ActivityRaffleCommand command) {
        ActivityRaffleCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityOrderEntity activityOrder = activityPartakeService.createRaffleOrder(new PartakeRaffleActivityEntity(
                validCommand.userId(),
                validCommand.sku(),
                validCommand.activityId()
        ));
        RaffleResult raffleResult = strategyRaffleService.raffle(new RaffleCommand(
                activityOrder.userId(),
                activityOrder.strategyId()
        ));

        settlementTransaction.settle(activityOrder, raffleResult);
        return new ActivityRaffleResult(
                activityOrder.orderId(),
                activityOrder.activityId(),
                raffleResult.strategyId(),
                raffleResult.awardId(),
                raffleResult.awardKey(),
                raffleResult.awardConfig(),
                raffleResult.awardDesc()
        );
    }
}
