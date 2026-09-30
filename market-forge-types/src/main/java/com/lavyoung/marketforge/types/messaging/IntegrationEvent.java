package com.lavyoung.marketforge.types.messaging;

import java.time.Instant;

/**
 * 集成事件基础契约。
 * <p>
 * 用于跨进程、跨边界传递领域事实。事件实现必须提供稳定的事件标识、发生时间和事件类型，
 * 发布适配器会据此构建消息信封、路由键以及可靠消息幂等键。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface IntegrationEvent {

    /**
     * 事件唯一标识。
     *
     * @return 事件唯一标识
     */
    String eventId();

    /**
     * 事件发生时间。
     *
     * @return 事件发生时间
     */
    Instant occurredAt();

    /**
     * 事件类型，默认也作为消息路由键。
     *
     * @return 事件类型
     */
    String eventType();

    /**
     * 事件版本。
     *
     * @return 事件版本号
     */
    default String version() {
        return "1.0";
    }

    /**
     * 消息交换机。
     *
     * @return 交换机名称
     */
    default String exchange() {
        return MqConstants.EXCHANGE;
    }

    /**
     * 消息路由键。
     *
     * @return 路由键
     */
    default String routingKey() {
        return eventType();
    }
}
