package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.award.model.SaveUserAwardRecordCommand;
import com.lavyoung.marketforge.application.award.service.IAwardApplicationService;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.tx.ITransactionExecutor;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import com.lavyoung.marketforge.types.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 活动抽奖结算事务。
 * <p>
 * 将“活动参与订单置为已使用”和“中奖记录及可靠消息任务落库”放入同一本地事务，
 * 避免订单已被消费但中奖记录或消息任务缺失。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class ActivityRaffleSettlementTransaction {

    private final IRaffleActivityPartakeService activityPartakeService;
    private final IAwardApplicationService awardApplicationService;
    private final ITransactionExecutor transactionExecutor;

    /**
     * 结算一次活动抽奖结果。
     *
     * @param activityOrder 已创建的活动参与订单
     * @param raffleResult  策略抽奖结果
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动订单状态更新、中奖记录保存或任务创建失败时抛出
     */
    public void settle(ActivityOrderEntity activityOrder, RaffleResult raffleResult) {
        transactionExecutor.execute(() -> {
            awardApplicationService.saveUserAwardRecord(new SaveUserAwardRecordCommand(
                    activityOrder.userId(),
                    activityOrder.activityId(),
                    raffleResult.strategyId(),
                    activityOrder.orderId(),
                    raffleResult.awardId(),
                    Objects.requireNonNullElse(raffleResult.awardDesc(), String.valueOf(raffleResult.awardId())),
                    DateUtil.now()
            ));
            activityPartakeService.consumeRaffleOrder(activityOrder.userId(), activityOrder.orderId());
        });
    }
}
