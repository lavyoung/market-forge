package com.lavyoung.marketforge.types.messaging;

/**
 * 通用事件编解码器
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
public interface IntegrationEventCodec {

    /**
     * 序列化str
     *
     * @param event
     * @return
     */
    String serialize(IntegrationEvent event);

    /**
     * 反序列化为指定类型
     *
     * @param eventType
     * @param messageBody
     * @return
     */
    IntegrationEvent deserialize(String eventType, String messageBody);
}
