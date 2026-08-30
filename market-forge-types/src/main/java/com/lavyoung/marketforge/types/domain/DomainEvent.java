package com.lavyoung.marketforge.types.domain;

import java.time.LocalDateTime;

/**
 * 领域事件标记接口。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public interface DomainEvent {

    /**
     * 获取领域事件的唯一标识。
     *
     * @return 事件唯一标识
     */
    String eventId();

    /**
     * 获取领域事件的发生时间。
     *
     * @return 事件发生时间
     */
    LocalDateTime occurredOn();

}
