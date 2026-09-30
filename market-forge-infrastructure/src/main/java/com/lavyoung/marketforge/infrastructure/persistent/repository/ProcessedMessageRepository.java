package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.IProcessedMessageDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 已消费消息仓储。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class ProcessedMessageRepository {

    private final IProcessedMessageDao processedMessageDao;

    /**
     * 尝试登记一条待消费消息。
     *
     * @param messageId  消息唯一标识
     * @param eventType  事件类型
     * @param occurredAt 事件发生时间
     * @return 首次登记返回 {@code true}，重复消息返回 {@code false}
     */
    public boolean tryRecord(String messageId, String eventType, LocalDateTime occurredAt) {
        return processedMessageDao.insertIgnore(messageId, eventType, occurredAt) == 1;
    }
}
