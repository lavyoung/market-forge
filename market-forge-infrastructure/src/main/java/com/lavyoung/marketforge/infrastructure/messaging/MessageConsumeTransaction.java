package com.lavyoung.marketforge.infrastructure.messaging;

import com.lavyoung.marketforge.infrastructure.persistent.repository.ProcessedMessageRepository;
import com.lavyoung.marketforge.types.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 消息消费事务模板。
 * <p>
 * 将持久化幂等登记与业务处理放在同一事务中，处理失败时撤销登记，使消息能够安全重试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MessageConsumeTransaction {

    private final ProcessedMessageRepository processedMessageRepository;

    /**
     * 在本地事务中消费一条消息。
     *
     * @param envelope       消息信封
     * @param context        消息上下文
     * @param messageHandler 应用消息处理器
     * @param <T>            集成事件类型
     * @return 首次消费返回 {@code true}，已成功消费的重复消息返回 {@code false}
     * @throws RuntimeException 业务处理失败时抛出并回滚幂等登记
     */
    @Transactional(rollbackFor = Exception.class)
    public <T extends IntegrationEvent> boolean consume(
            MessageEnvelope<T> envelope,
            MessageContext context,
            MessageHandler<T> messageHandler
    ) {
        if (!(envelope.payload() instanceof BusinessIdempotentIntegrationEvent)) {
            LocalDateTime occurredAt = LocalDateTime.ofInstant(envelope.occurredAt(), ZoneId.systemDefault());
            if (!processedMessageRepository.tryRecord(envelope.messageId(), envelope.eventType(), occurredAt)) {
                return false;
            }
        }
        messageHandler.handle(envelope.payload(), context);
        return true;
    }
}
