package com.lavyoung.marketforge.domain.award.event;

import com.lavyoung.marketforge.types.messaging.BusinessIdempotentIntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 用户中奖记录创建集成事件。
 *
 * <p>中奖记录与本地消息任务在同一事务中保存后，通过该事件通知发奖流程。
 * 事件携带中奖时确定的完整履约快照，消费者无需重新查询可能已经变化的奖品定义。</p>
 *
 * @param eventId            事件唯一标识
 * @param occurredAt         事件发生时间
 * @param userId             用户标识，同时作为分片键
 * @param orderId            抽奖订单标识，也是发奖幂等业务号
 * @param awardId            命中奖品标识
 * @param fulfillmentSnapshot 中奖时固定的奖品履约快照
 * @param awardTitle         命中奖品展示标题
 */
@Builder
public record SendAwardRecordEvent(
        String eventId,
        Instant occurredAt,
        String userId,
        String orderId,
        Long awardId,
        AwardFulfillmentSnapshot fulfillmentSnapshot,
        String awardTitle
) implements BusinessIdempotentIntegrationEvent {

    /**
     * 校验可靠消息所需的完整业务数据。
     *
     * @throws NullPointerException     时间、奖品标识或履约快照为空时抛出
     * @throws IllegalArgumentException 文本为空白或奖品标识非法时抛出
     */
    public SendAwardRecordEvent {
        requireText(eventId, "eventId");
        requireText(userId, "userId");
        requireText(orderId, "orderId");
        requireText(awardTitle, "awardTitle");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(awardId, "awardId must not be null");
        Objects.requireNonNull(fulfillmentSnapshot, "fulfillmentSnapshot must not be null");

        if (awardId <= 0) {
            throw new IllegalArgumentException("awardId must be positive");
        }
    }

    /**
     * 根据中奖业务信息创建可靠消息事件。
     *
     * @param userId             用户标识
     * @param orderId            抽奖订单标识
     * @param awardId            命中奖品标识
     * @param fulfillmentSnapshot 中奖时确定的履约快照
     * @param awardTitle         命中奖品标题
     * @throws NullPointerException     必填对象为空时抛出
     * @throws IllegalArgumentException 必填文本为空白或奖品标识非法时抛出
     */
    public SendAwardRecordEvent(String userId, String orderId, Long awardId,
                                AwardFulfillmentSnapshot fulfillmentSnapshot, String awardTitle) {
        this(UUID.randomUUID().toString(), Instant.now(), userId, orderId,
                awardId, fulfillmentSnapshot, awardTitle);
    }

    @Override
    public String eventType() {
        return MqConstants.USER_AWARD_SEND_ROUTE_KEY;
    }

    @Override
    public String exchange() {
        return MqConstants.USER_AWARD_EXCHANGE;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
