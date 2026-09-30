package com.lavyoung.marketforge.infrastructure.persistent.dao.mq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.ProcessedMessagePO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 已消费消息数据访问接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper
public interface IProcessedMessageDao extends BaseMapper<ProcessedMessagePO> {

    /**
     * 尝试登记消息，重复消息由唯一索引忽略。
     *
     * @param messageId  消息唯一标识
     * @param eventType  事件类型
     * @param occurredAt 事件发生时间
     * @return 首次登记返回 {@code 1}，消息已登记返回 {@code 0}
     */
    @Insert("""
            INSERT IGNORE INTO processed_message
                (message_id, event_type, occurred_at, create_time, update_time)
            VALUES
                (#{messageId}, #{eventType}, #{occurredAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    int insertIgnore(
            @Param("messageId") String messageId,
            @Param("eventType") String eventType,
            @Param("occurredAt") LocalDateTime occurredAt
    );
}
