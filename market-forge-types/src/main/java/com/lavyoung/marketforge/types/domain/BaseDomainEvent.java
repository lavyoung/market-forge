package com.lavyoung.marketforge.types.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件的基础实现，记录事件标识和发生时间。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public abstract class BaseDomainEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public String eventId() {
        return eventId;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

}
