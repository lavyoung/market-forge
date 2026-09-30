package com.lavyoung.marketforge.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuStockDeductedEvent;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuZeroStockEvent;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.strategy.event.AwardStockDeductedEvent;
import com.lavyoung.marketforge.infrastructure.messaging.AbstractMessageListenerAdapter;
import com.lavyoung.marketforge.infrastructure.messaging.MessageConsumeTransaction;
import com.lavyoung.marketforge.types.messaging.MessageEnvelope;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
public class RabbitMessageListenerAdapter extends AbstractMessageListenerAdapter {

    private static final TypeReference<MessageEnvelope<AwardStockDeductedEvent>> AWARD_STOCK_DEDUCTED_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<MessageEnvelope<ActivitySkuStockDeductedEvent>> SKU_STOCK_DEDUCTED_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<MessageEnvelope<ActivitySkuZeroStockEvent>> SKU_STOCK_ZERO_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<MessageEnvelope<SendAwardRecordEvent>> USER_AWARD_SEND_TYPE = new TypeReference<>() {
    };

    private final MessageHandler<AwardStockDeductedEvent> awardStockDeductedEventMessageHandler;
    private final MessageHandler<ActivitySkuStockDeductedEvent> activitySkuStockDeductedEventMessageHandler;
    private final MessageHandler<ActivitySkuZeroStockEvent> activitySkuStockZeroEventMessageHandler;
    private final MessageHandler<SendAwardRecordEvent> sendAwardRecordMessageHandler;

    public RabbitMessageListenerAdapter(
            ObjectMapper objectMapper,
            MessageConsumeTransaction messageConsumeTransaction,
            MessageHandler<AwardStockDeductedEvent> awardStockDeductedEventMessageHandler,
            MessageHandler<ActivitySkuStockDeductedEvent> activitySkuStockDeductedEventMessageHandler,
            MessageHandler<ActivitySkuZeroStockEvent> activitySkuStockZeroEventMessageHandler,
            MessageHandler<SendAwardRecordEvent> sendAwardRecordMessageHandler
    ) {
        super(objectMapper, messageConsumeTransaction);
        this.awardStockDeductedEventMessageHandler = awardStockDeductedEventMessageHandler;
        this.activitySkuStockDeductedEventMessageHandler = activitySkuStockDeductedEventMessageHandler;
        this.activitySkuStockZeroEventMessageHandler = activitySkuStockZeroEventMessageHandler;
        this.sendAwardRecordMessageHandler = sendAwardRecordMessageHandler;
    }

    @RabbitListener(queues = MqConstants.AWARD_STOCK_DEDUCT_QUEUE)
    public void onAwardStockDeducted(Message message) throws Exception {
        consume(message, AWARD_STOCK_DEDUCTED_TYPE, awardStockDeductedEventMessageHandler);
    }

    @RabbitListener(queues = MqConstants.SKU_STOCK_DEDUCT_QUEUE)
    public void onSkuStockDeducted(Message message) throws Exception {
        consume(message, SKU_STOCK_DEDUCTED_TYPE, activitySkuStockDeductedEventMessageHandler);

    }

    @RabbitListener(queues = MqConstants.SKU_STOCK_ZERO_QUEUE)
    public void onSkuStockZero(Message message) throws Exception {
        consume(message, SKU_STOCK_ZERO_TYPE, activitySkuStockZeroEventMessageHandler);
    }

    /**
     * 消费用户中奖记录创建消息并转交应用处理器。
     *
     * @param message RabbitMQ 原始消息
     * @throws Exception 消息反序列化或业务处理失败时抛出
     */
    @RabbitListener(queues = MqConstants.USER_AWARD_SEND_QUEUE)
    public void onUserAwardSend(Message message) throws Exception {
        consume(message, USER_AWARD_SEND_TYPE, sendAwardRecordMessageHandler);
    }
}
