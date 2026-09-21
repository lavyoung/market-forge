package com.lavyoung.marketforge.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuStockDeductedEvent;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuZeroStockEvent;
import com.lavyoung.marketforge.domain.strategy.event.AwardStockDeductedEvent;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageEnvelope;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import com.lavyoung.marketforge.types.utils.MdcUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 *
 * RabbitMQ 入站适配器：把队列里的消息解成领域事件，转交给应用层的 MessageHandler。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMessageListenerAdapter {

    private final IRedisService redisService;
    private final ObjectMapper objectMapper;
    private final MessageHandler<AwardStockDeductedEvent> awardStockDeductedEventMessageHandler;
    private final MessageHandler<ActivitySkuStockDeductedEvent> activitySkuStockDeductedEventMessageHandler;
    private final MessageHandler<ActivitySkuZeroStockEvent> activitySkuZeroStockEventMessageHandler;

    @RabbitListener(queues = MqConstants.AWARD_STOCK_DEDUCT_QUEUE)
    public void onAwardStockDeducted(Message message) throws Exception {
        MessageEnvelope<AwardStockDeductedEvent> envelope = objectMapper.readValue(message.getBody(), new TypeReference<MessageEnvelope<AwardStockDeductedEvent>>() {
        });

        String dedupKey = MqConstants.CONSUMED_KEY_PREFIX + envelope.messageId();
        if (!redisService.setIfAbsent(dedupKey, envelope.occurredAt().toString(), Duration.ofHours(24))) {
            log.warn("重复消息已跳过 messageId={} traceId={}", envelope.messageId(), envelope.traceId());
            return;
        }

        // 奖品库存扣减
        try {
            MessageContext context = new MessageContext(
                    envelope.messageId(),
                    envelope.traceId(),
                    Boolean.TRUE.equals(message.getMessageProperties().isRedelivered()),
                    message.getMessageProperties().getHeaders()
            );
            MdcUtil.putTraceId(envelope.traceId());
            awardStockDeductedEventMessageHandler.handle(envelope.payload(), context);
        } catch (Exception exception) {
            redisService.delete(dedupKey);
            throw exception;
        } finally {
            MdcUtil.clearTraceId();
        }
    }

    @RabbitListener(queues = MqConstants.SKU_STOCK_DEDUCT_QUEUE)
    public void onSkuStockDeducted(Message message) throws Exception {
        MessageEnvelope<ActivitySkuStockDeductedEvent> envelope = objectMapper.readValue(message.getBody(), new TypeReference<MessageEnvelope<ActivitySkuStockDeductedEvent>>() {
        });

        String dedupKey = MqConstants.CONSUMED_KEY_PREFIX + envelope.messageId();
        if (!redisService.setIfAbsent(dedupKey, envelope.occurredAt().toString(), Duration.ofHours(24))) {
            log.warn("重复消息已跳过 messageId={} traceId={}", envelope.messageId(), envelope.traceId());
            return;
        }

        // 奖品库存扣减
        try {
            MessageContext context = new MessageContext(
                    envelope.messageId(),
                    envelope.traceId(),
                    Boolean.TRUE.equals(message.getMessageProperties().isRedelivered()),
                    message.getMessageProperties().getHeaders()
            );
            MdcUtil.putTraceId(envelope.traceId());
            activitySkuStockDeductedEventMessageHandler.handle(envelope.payload(), context);
        } catch (Exception exception) {
            redisService.delete(dedupKey);
            throw exception;
        } finally {
            MdcUtil.clearTraceId();
        }
    }

    @RabbitListener(queues = MqConstants.SKU_STOCK_ZERO_QUEUE)
    public void onSkuStockZero(Message message) throws Exception {
        MessageEnvelope<ActivitySkuZeroStockEvent> envelope = objectMapper.readValue(message.getBody(), new TypeReference<MessageEnvelope<ActivitySkuZeroStockEvent>>() {
        });

        String dedupKey = MqConstants.CONSUMED_KEY_PREFIX + envelope.messageId();
        if (!redisService.setIfAbsent(dedupKey, envelope.occurredAt().toString(), Duration.ofHours(24))) {
            log.warn("重复消息已跳过 messageId={} traceId={}", envelope.messageId(), envelope.traceId());
            return;
        }

        // 奖品库存扣减
        try {
            MessageContext context = new MessageContext(
                    envelope.messageId(),
                    envelope.traceId(),
                    Boolean.TRUE.equals(message.getMessageProperties().isRedelivered()),
                    message.getMessageProperties().getHeaders()
            );
            MdcUtil.putTraceId(envelope.traceId());
            activitySkuZeroStockEventMessageHandler.handle(envelope.payload(), context);
        } catch (Exception exception) {
            redisService.delete(dedupKey);
            throw exception;
        } finally {
            MdcUtil.clearTraceId();
        }
    }
}
