package com.lavyoung.marketforge.domain.award.model.entity;

import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Builder
public record TaskEntity(
        String topic,
        String eventId,
        String eventType,
        SendAwardRecordEvent messageBody,
        LocalDateTime occurredAt,
        String state
) {
}
