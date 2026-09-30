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
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
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
    private final IActivityArmory activityArmory;

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityRaffleResult raffle(ActivityRaffleCommand command) {
        ActivityRaffleCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        // 创建一笔活动订单
        ActivityOrderEntity activityOrder = activityPartakeService.createRaffleOrder(new PartakeRaffleActivityEntity(
                validCommand.userId(),
                validCommand.sku(),
                validCommand.activityId()
        ));
        // 抽奖
        RaffleResult raffleResult = strategyRaffleService.raffle(new RaffleCommand(
                activityOrder.userId(),
                activityOrder.strategyId()
        ));

        // 结算一次活动抽奖结果 用户发奖和活动订单消费
        settlementTransaction.settle(activityOrder, raffleResult);
        return new ActivityRaffleResult(
                activityOrder.orderId(),
                activityOrder.activityId(),
                raffleResult.strategyId(),
                raffleResult.awardId(),
                raffleResult.awardKey(),
                raffleResult.awardConfig(),
                raffleResult.awardTitle(),
                raffleResult.awardDesc()
        );
    }

    @Override
    public void armory(Long activityId) {
        // 装配是活动ID发起的，所以需要把活动ID对应的sku记录一起查询出来进行装配
        // 活动装配会预热活动详情、活动次数、SKU 库存。
        boolean assembled = activityArmory.assembleActivitySkuByActivityId(activityId);
        if (!assembled) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ASSEMBLY_FAILED, activityId);
        }
        // 策略装配会预热概率表、权重表、奖品库存
        strategyRaffleService.initStrategyRaffleByActivityId(activityId);
    }
}
