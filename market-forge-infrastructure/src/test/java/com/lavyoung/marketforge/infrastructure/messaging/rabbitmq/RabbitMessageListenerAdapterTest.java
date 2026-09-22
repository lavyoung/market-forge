package com.lavyoung.marketforge.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuStockDeductedEvent;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuZeroStockEvent;
import com.lavyoung.marketforge.domain.strategy.event.AwardStockDeductedEvent;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageEnvelope;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 验证 MQ 入站适配器的反序列化、幂等去重与失败回滚行为。
 *
 * @author lavyoung
 */
@ExtendWith(MockitoExtension.class)
class RabbitMessageListenerAdapterTest {

    private static final String TRACE_ID = "trace-0001";
    private static final long STRATEGY_ID = 100_001L;
    private static final long AWARD_ID = 100_011;
    private static final String USER_ID = "user-001";
    private static final String DEDUP_KEY_PREFIX = "mq:consumed:";

    @Mock
    private IRedisService redisService;

    @Mock
    private MessageHandler<AwardStockDeductedEvent> awardStockDeductedHandler;

    @Mock
    private MessageHandler<ActivitySkuStockDeductedEvent> activitySkuStockDeductedEventMessageHandler;

    @Mock
    private MessageHandler<ActivitySkuZeroStockEvent> activitySkuZeroStockEventMessageHandler;

    private ObjectMapper objectMapper;

    private RabbitMessageListenerAdapter adapter;

    /**
     * 用真实的 ObjectMapper 构建适配器，以便真实覆盖 JSON 反序列化路径。
     */
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        adapter = new RabbitMessageListenerAdapter(
                objectMapper,
                redisService
        );
    }

    /**
     * 正常消息应被解出正确的事件与上下文，并转交处理器。
     */
    @Test
    void shouldDeserializeEnvelopeAndDispatchToHandler() throws Exception {
        AwardStockDeductedEvent event = newEvent();
        when(redisService.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        adapter.onAwardStockDeducted(messageOf(event));

        ArgumentCaptor<AwardStockDeductedEvent> eventCaptor =
                ArgumentCaptor.forClass(AwardStockDeductedEvent.class);
        ArgumentCaptor<MessageContext> contextCaptor =
                ArgumentCaptor.forClass(MessageContext.class);
        verify(awardStockDeductedHandler).handle(eventCaptor.capture(), contextCaptor.capture());

        assertEquals(STRATEGY_ID, eventCaptor.getValue().strategyId());
        assertEquals(AWARD_ID, eventCaptor.getValue().awardId());
        assertEquals(USER_ID, eventCaptor.getValue().userId());
        assertEquals(event.eventId(), contextCaptor.getValue().messageId());
        assertEquals(TRACE_ID, contextCaptor.getValue().traceId());
    }

    /**
     * 幂等键已存在时，重复消息必须被跳过，不得再次触发业务处理。
     */
    @Test
    void shouldSkipDuplicateMessage() throws Exception {
        AwardStockDeductedEvent event = newEvent();
        when(redisService.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        adapter.onAwardStockDeducted(messageOf(event));

        verifyNoInteractions(awardStockDeductedHandler);
    }

    /**
     * 处理失败时必须撤销幂等占位并继续向上抛出，保证重试有机会重新处理。
     */
    @Test
    void shouldReleaseIdempotentKeyWhenHandlerFails() throws Exception {
        AwardStockDeductedEvent event = newEvent();
        when(redisService.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        doThrow(new IllegalStateException("扣减失败"))
                .when(awardStockDeductedHandler).handle(any(), any());

        Message message = messageOf(event);

        assertThrows(IllegalStateException.class, () -> adapter.onAwardStockDeducted(message));

        verify(redisService).delete(DEDUP_KEY_PREFIX + event.eventId());
    }

    private AwardStockDeductedEvent newEvent() {
        return new AwardStockDeductedEvent(STRATEGY_ID, AWARD_ID, USER_ID);
    }

    /**
     * 把事件包成信封、序列化成真实字节，再包成一条 AMQP 消息。
     */
    private Message messageOf(AwardStockDeductedEvent event) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(MessageEnvelope.of(event, TRACE_ID));
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(event.eventId());
        properties.setRedelivered(false);
        return new Message(body, properties);
    }

    @Test
    void shouldDispatchSkuStockDeductedEvent() throws Exception {
        ActivitySkuStockDeductedEvent event =
                new ActivitySkuStockDeductedEvent(10001L, 20001L, "user-001");

        when(redisService.setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)
        )).thenReturn(true);

        adapter.onSkuStockDeducted(messageOf(event));

        verify(activitySkuStockDeductedEventMessageHandler).handle(
                eq(event),
                any(MessageContext.class)
        );
    }

    @Test
    void shouldDispatchSkuStockZeroEvent() throws Exception {
        ActivitySkuZeroStockEvent event =
                new ActivitySkuZeroStockEvent(10001L);

        when(redisService.setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)
        )).thenReturn(true);

        adapter.onSkuStockZero(messageOf(event));

        verify(activitySkuZeroStockEventMessageHandler).handle(
                eq(event),
                any(MessageContext.class)
        );
    }

    private <T extends IntegrationEvent> Message messageOf(T event)
            throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(
                MessageEnvelope.of(event, TRACE_ID)
        );

        MessageProperties properties = new MessageProperties();
        properties.setMessageId(event.eventId());
        properties.setRedelivered(false);

        return new Message(body, properties);
    }
}