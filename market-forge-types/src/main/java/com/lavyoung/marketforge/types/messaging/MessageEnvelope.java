package com.lavyoung.marketforge.types.messaging;

import java.time.Instant;

/**
 * MQ 消息信封。
 * <p>
 * 信封包裹业务集成事件，并携带消息唯一标识、事件类型、版本、链路追踪等基础元数据。
 * 入站适配器依赖信封元数据完成幂等登记和日志追踪。
 *
 * @param messageId  消息唯一标识
 * @param eventType  事件类型或路由键
 * @param version    事件版本
 * @param occurredAt 事件发生时间
 * @param traceId    链路追踪标识
 * @param payload    业务事件负载
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

    /**
     * 根据集成事件创建消息信封。
     *
     * @param event   集成事件
     * @param traceId 当前链路追踪标识
     * @param <T>     集成事件类型
     * @return 包含事件元数据和负载的消息信封
     */
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
