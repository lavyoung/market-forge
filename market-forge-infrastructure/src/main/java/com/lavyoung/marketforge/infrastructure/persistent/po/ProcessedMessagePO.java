package com.lavyoung.marketforge.infrastructure.persistent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 已消费消息持久化对象。
 * <p>
 * 通过消息标识唯一约束记录成功消费的集成事件，并与业务处理共享数据库事务。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("processed_message")
public class ProcessedMessagePO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String eventType;

    private LocalDateTime occurredAt;
}
