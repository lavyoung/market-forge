package com.lavyoung.marketforge.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuStockDeductedEvent;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuZeroStockEvent;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
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
 * RabbitMQ 入站适配器。
 * <p>
 * 将各业务队列中的消息反序列化为领域事件，并转交给应用层 {@link MessageHandler}。
 * 消息幂等、MDC 追踪和异常回滚由父类消费模板统一处理。
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
    private static final TypeReference<MessageEnvelope<SendRebateEvent>> SEND_REBATE_TYPE = new TypeReference<>() {
    };

    private final MessageHandler<AwardStockDeductedEvent> awardStockDeductedEventMessageHandler;
    private final MessageHandler<ActivitySkuStockDeductedEvent> activitySkuStockDeductedEventMessageHandler;
    private final MessageHandler<ActivitySkuZeroStockEvent> activitySkuStockZeroEventMessageHandler;
    private final MessageHandler<SendAwardRecordEvent> sendAwardRecordMessageHandler;
    private final MessageHandler<SendRebateEvent> sendRebateMessageHandler;

    public RabbitMessageListenerAdapter(
            ObjectMapper objectMapper,
            MessageConsumeTransaction messageConsumeTransaction,
            MessageHandler<AwardStockDeductedEvent> awardStockDeductedEventMessageHandler,
            MessageHandler<ActivitySkuStockDeductedEvent> activitySkuStockDeductedEventMessageHandler,
            MessageHandler<ActivitySkuZeroStockEvent> activitySkuStockZeroEventMessageHandler,
            MessageHandler<SendAwardRecordEvent> sendAwardRecordMessageHandler,
            MessageHandler<SendRebateEvent> sendRebateMessageHandler
    ) {
        super(objectMapper, messageConsumeTransaction);
        this.awardStockDeductedEventMessageHandler = awardStockDeductedEventMessageHandler;
        this.activitySkuStockDeductedEventMessageHandler = activitySkuStockDeductedEventMessageHandler;
        this.activitySkuStockZeroEventMessageHandler = activitySkuStockZeroEventMessageHandler;
        this.sendAwardRecordMessageHandler = sendAwardRecordMessageHandler;
        this.sendRebateMessageHandler = sendRebateMessageHandler;
    }

    /**
     * 消费奖品库存扣减消息。
     *
     * @param message RabbitMQ 原始消息
     * @throws Exception 消息反序列化或业务处理失败时抛出
     */
    @RabbitListener(queues = MqConstants.AWARD_STOCK_DEDUCT_QUEUE)
    public void onAwardStockDeducted(Message message) throws Exception {
        consume(message, AWARD_STOCK_DEDUCTED_TYPE, awardStockDeductedEventMessageHandler);
    }

    /**
     * 消费活动 SKU 库存扣减消息。
     *
     * @param message RabbitMQ 原始消息
     * @throws Exception 消息反序列化或业务处理失败时抛出
     */
    @RabbitListener(queues = MqConstants.SKU_STOCK_DEDUCT_QUEUE)
    public void onSkuStockDeducted(Message message) throws Exception {
        consume(message, SKU_STOCK_DEDUCTED_TYPE, activitySkuStockDeductedEventMessageHandler);

    }

    /**
     * 消费活动 SKU 库存清零消息。
     *
     * @param message RabbitMQ 原始消息
     * @throws Exception 消息反序列化或业务处理失败时抛出
     */
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

    /**
     * 消费用户行为返利消息并转交应用处理器。
     *
     * @param message RabbitMQ 原始消息
     * @throws Exception 消息反序列化或业务处理失败时抛出
     */
    @RabbitListener(queues = MqConstants.SEND_REBATE_QUEUE)
    public void onSendRebate(Message message) throws Exception {
        consume(message, SEND_REBATE_TYPE, sendRebateMessageHandler);
    }
}
