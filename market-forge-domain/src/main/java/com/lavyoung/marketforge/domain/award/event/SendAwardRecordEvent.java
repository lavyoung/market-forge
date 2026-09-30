package com.lavyoung.marketforge.domain.award.event;

import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户中奖记录创建集成事件。
 * <p>
 * 中奖记录与本地消息任务在同一事务中保存后，通过该事件通知下游发奖或领取流程。
 * 当前事件只表达“中奖记录已创建”的事实，不直接改变奖品领取或发放状态。
 *
 * @param eventId    事件唯一标识，用于可靠投递和消费幂等
 * @param occurredAt 事件发生时间
 * @param userId     用户标识，同时作为中奖记录和任务表分片键
 * @param orderId    抽奖参与订单号
 * @param awardId    命中奖品标识
 * @param awardTitle 命中奖品标题
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Builder
public record SendAwardRecordEvent(
        String eventId,
        Instant occurredAt,
        String userId,
        String orderId,
        Long awardId,
        String awardTitle
) implements IntegrationEvent {

    /**
     * 基于中奖记录关键信息创建用户中奖记录事件。
     *
     * @param userId     用户标识
     * @param orderId    抽奖参与订单号
     * @param awardId    命中奖品标识
     * @param awardTitle 命中奖品标题
     */
    public SendAwardRecordEvent(String userId, String orderId, Long awardId, String awardTitle) {
        this(UUID.randomUUID().toString(), Instant.now(), userId, orderId, awardId, awardTitle);
    }

    @Override
    public String eventType() {
        return MqConstants.USER_AWARD_SEND_ROUTE_KEY;
    }

    @Override
    public String exchange() {
        return MqConstants.USER_AWARD_EXCHANGE;
    }
}
