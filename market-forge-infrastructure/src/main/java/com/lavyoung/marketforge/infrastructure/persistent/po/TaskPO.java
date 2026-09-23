package com.lavyoung.marketforge.infrastructure.persistent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息任务持久化对象。
 * <p>
 * 记录待发布 MQ 消息及其投递状态，用于异步可靠投递和失败补偿。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("task")
public class TaskPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息主题或交换机。
     */
    private String topic;

    /**
     * 事件唯一标识，用于幂等发布。
     */
    private String eventId;

    /**
     * 事件类型或消息路由键。
     */
    private String eventType;

    /**
     * 序列化后的消息体。
     */
    private String messageBody;

    /**
     * 事件发生时间。
     */
    private LocalDateTime occurredAt;

    /**
     * 消息任务状态。
     */
    private String state;
}
