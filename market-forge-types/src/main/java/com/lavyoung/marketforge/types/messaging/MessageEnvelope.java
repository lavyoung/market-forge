package com.lavyoung.marketforge.types.messaging;

import java.time.Instant;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public record MessageEnvelope<T>(
        String messageId,
        String eventType,
        String version,
        Instant occurredAt,
        String traceId,
        T payload
) {


    public static <T extends IntegrationEvent> MessageEnvelope<T> of(T event, String traceId) {
        return new MessageEnvelope<>(
                event.eventId(),
                event.eventType(),
                event.version(),
                event.occurredAt(),
                traceId,
                event
        );
    }
}
