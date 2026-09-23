package com.lavyoung.marketforge.domain.award.event;

import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Builder
public record SendAwardRecordEvent(
        String eventId,
        Instant occurredAt,
        String userId,
        Long awardId,
        String awardTitle
) implements IntegrationEvent {

    /**
     * 用户发奖消息
     *
     * @param userId     用户ID
     * @param awardId    奖品id
     * @param awardTitle 奖品名称
     */
    public SendAwardRecordEvent(String userId, Long awardId, String awardTitle) {
        this(UUID.randomUUID().toString(), Instant.now(), userId, awardId, awardTitle);
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
