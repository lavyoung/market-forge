package com.lavyoung.marketforge.types.messaging;

import java.time.Instant;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface IntegrationEvent {

    String eventId();

    Instant occurredAt();

    String eventType();

    default String version() {
        return "1.0";
    }

    default String exchange() {
        return MqConstants.EXCHANGE;
    }

    default String routingKey() {
        return eventType();
    }
}
