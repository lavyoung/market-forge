package com.lavyoung.marketforge.types.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件的基础实现，记录事件标识和发生时间。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
public abstract class BaseDomainEvent implements DomainEvent {

    /** 领域事件的唯一标识。 */
    private final String eventId;

    /** 领域事件的发生时间。 */
    private final LocalDateTime occurredOn;

    /**
     * 创建领域事件并初始化事件标识和发生时间。
     */
    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String eventId() {
        return eventId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

}
