package com.lavyoung.marketforge.domain.messaging.model.entity;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 待发布消息任务领域实体。
 *
 * @param topic       消息主题或交换机
 * @param eventId     事件唯一标识
 * @param eventType   事件类型或路由键
 * @param messageBody 序列化后的消息体
 * @param occurredAt  事件发生时间
 * @param state       任务状态
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Builder
public record TaskEntity(
        String topic,
        String eventId,
        String eventType,
        String messageBody,
        LocalDateTime occurredAt,
        String state
) {
}
