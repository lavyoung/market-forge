package com.lavyoung.marketforge.infrastructure.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.messaging.*;
import com.lavyoung.marketforge.types.utils.MdcUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

import static com.lavyoung.marketforge.types.messaging.MqConstants.CONSUMED_MARK_TTL;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
@Slf4j
@AllArgsConstructor
public abstract class AbstractMessageListenerAdapter {

    protected final ObjectMapper objectMapper;
    protected final IRedisService redisService;

    /**
     * 统一处理 RabbitMQ 入站消息，包括反序列化、临时幂等控制、
     * 消息上下文构建、链路追踪和异常清理。
     *
     * @param message        RabbitMQ 原始消息
     * @param envelopeType   消息信封的具体泛型类型
     * @param messageHandler 对应的应用消息处理器
     * @param <T>            集成事件类型
     * @throws Exception 消息反序列化或业务处理失败
     */
    protected <T extends IntegrationEvent> void consume(Message message,
                                                        TypeReference<MessageEnvelope<T>> envelopeType,
                                                        MessageHandler<T> messageHandler
    ) throws Exception {
        MessageEnvelope<T> envelope = objectMapper.readValue(message.getBody(), envelopeType);

        String dedupKey = MqConstants.CONSUMED_KEY_PREFIX + envelope.messageId();

        if (!reserveMessage(dedupKey, envelope)) {
            return;
        }

        try {
            MdcUtil.putTraceId(envelope.traceId());
            MessageContext context = new MessageContext(
                    envelope.messageId(),
                    envelope.traceId(),
                    Boolean.TRUE.equals(message.getMessageProperties().isRedelivered()),
                    message.getMessageProperties().getHeaders()
            );
            messageHandler.handle(envelope.payload(), context);
        } catch (Exception e) {
            redisService.delete(dedupKey);
            throw e;
        } finally {
            MdcUtil.clearTraceId();
        }
    }

    /**
     * 尝试为消息建立临时消费标记。
     *
     * @param dedupKey 幂等键
     * @param envelope 消息信封
     * @return 首次消费返回 true，重复消息返回 false
     */
    private boolean reserveMessage(String dedupKey, MessageEnvelope<?> envelope) {
        boolean reserved = redisService.setIfAbsent(dedupKey, envelope.occurredAt().toString(),
                CONSUMED_MARK_TTL
        );

        if (!reserved) {
            log.warn("重复消息已跳过 messageId={} traceId={} eventType={}",
                    envelope.messageId(),
                    envelope.traceId(),
                    envelope.eventType()
            );
        }
        return reserved;
    }
}
