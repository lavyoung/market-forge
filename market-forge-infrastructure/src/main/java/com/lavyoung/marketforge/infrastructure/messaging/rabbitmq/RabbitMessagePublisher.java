package com.lavyoung.marketforge.infrastructure.messaging.rabbitmq;

import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MessageEnvelope;
import com.lavyoung.marketforge.types.messaging.MessagePublisher;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import com.lavyoung.marketforge.types.utils.MdcUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 消息发布实现：发送后同步等待 broker 确认，路由失败或未被确认即抛异常。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMessagePublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(IntegrationEvent event) {
        String traceId = MdcUtil.getTraceId();
        MessageEnvelope<IntegrationEvent> envelope = MessageEnvelope.of(event, traceId);
        CorrelationData correlationData = new CorrelationData(envelope.messageId());


        rabbitTemplate.convertAndSend(
                event.exchange(),
                event.routingKey(),
                envelope,
                message -> {
                    message.getMessageProperties().setMessageId(envelope.messageId());
                    message.getMessageProperties().setHeader("eventType", envelope.eventType());
                    message.getMessageProperties().setHeader(MdcUtil.getTraceIdKey(), envelope.traceId());
                    return message;
                },
                correlationData
        );

        // 校验发送结果
        verify(correlationData, event, envelope);
    }

    /**
     * 校验本次发送结果：先看是否被退回，再看 broker 是否确认。
     *
     * @param correlationData 本次发送的关联数据
     * @param event           被发送的集成事件
     * @param envelope        实际投递的消息信封
     * @throws IllegalStateException 消息被退回或未被 broker 确认
     */
    private void verify(CorrelationData correlationData, IntegrationEvent event, MessageEnvelope<IntegrationEvent> envelope) {
        CorrelationData.Confirm confirm = awaitConfirm(correlationData);

        ReturnedMessage returned = correlationData.getReturned();

        if (returned != null) {
            throw new IllegalStateException(
                    "消息无法路由到队列 routingKey=" + event.routingKey()
                            + ", replyText=" + returned.getReplyText());
        }

        if (!confirm.isAck()) {
            throw new IllegalStateException(
                    "broker 未确认消息 messageId=" + envelope.messageId()
                            + ", reason=" + confirm.getReason());
        }

        log.debug("消息发送成功 messageId={} routingKey={}", envelope.messageId(), event.routingKey());
    }

    /**
     * 阻塞等待 broker 的确认结果。
     *
     * @param correlationData 本次发送的关联数据
     * @return broker 返回的确认结果
     * @throws IllegalStateException 等待被中断或超时
     */
    private CorrelationData.Confirm awaitConfirm(CorrelationData correlationData) {
        try {
            return correlationData.getFuture().get(MqConstants.CONFIRM_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待 broker 确认时被中断", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("等待 broker 确认失败", exception);
        }
    }
}
