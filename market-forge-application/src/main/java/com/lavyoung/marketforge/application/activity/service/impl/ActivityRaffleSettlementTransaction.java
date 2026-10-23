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
     * <p>履约快照必须从抽奖结果原样进入中奖记录命令。中奖记录与可靠消息任务
     * 在同一事务中保存，随后才将活动参与订单更新为已使用。</p>
     *
     * @param activityOrder 已创建的活动参与订单
     * @param raffleResult  已包含履约快照的抽奖结果
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 订单消费、中奖记录或消息任务创建失败时抛出
     */
    public void settle(ActivityOrderEntity activityOrder, RaffleResult raffleResult) {
        transactionExecutor.execute(() -> {
            awardApplicationService.saveUserAwardRecord(new SaveUserAwardRecordCommand(
                    activityOrder.userId(),
                    activityOrder.activityId(),
                    raffleResult.strategyId(),
                    activityOrder.orderId(),
                    raffleResult.awardId(),
                    raffleResult.fulfillmentSnapshot(),
                    raffleResult.awardTitle(),
                    DateUtil.now()
            ));
            activityPartakeService.consumeRaffleOrder(activityOrder.userId(), activityOrder.orderId());
        });
    }
}
