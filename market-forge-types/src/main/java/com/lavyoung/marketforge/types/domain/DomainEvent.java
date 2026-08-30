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

    String eventId();

    LocalDateTime occurredOn();

}
