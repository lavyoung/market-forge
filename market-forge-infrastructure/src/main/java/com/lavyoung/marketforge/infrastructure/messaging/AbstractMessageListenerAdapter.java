package com.lavyoung.marketforge.infrastructure.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageEnvelope;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import com.lavyoung.marketforge.types.utils.MdcUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

/**
 * 消息监听适配器基类。
 * <p>
 * 为具体 MQ 监听器提供统一消费模板：反序列化消息信封、构建消费上下文、写入 MDC、
 * 执行持久化幂等控制，并把业务事件转交应用层处理器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
@Slf4j
@AllArgsConstructor
public abstract class AbstractMessageListenerAdapter {

    protected final ObjectMapper objectMapper;
    protected final MessageConsumeTransaction messageConsumeTransaction;

    /**
     * 统一处理 RabbitMQ 入站消息，包括反序列化、持久化幂等控制、
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

        try {
            MdcUtil.putTraceId(envelope.traceId());
            MessageContext context = new MessageContext(
                    envelope.messageId(),
                    envelope.traceId(),
                    Boolean.TRUE.equals(message.getMessageProperties().isRedelivered()),
                    message.getMessageProperties().getHeaders()
            );
            boolean consumed = messageConsumeTransaction.consume(envelope, context, messageHandler);
            if (!consumed) {
                log.warn("重复消息已跳过 messageId={} traceId={} eventType={}",
                        envelope.messageId(), envelope.traceId(), envelope.eventType());
            }
        } finally {
            MdcUtil.clearTraceId();
        }
    }
}
