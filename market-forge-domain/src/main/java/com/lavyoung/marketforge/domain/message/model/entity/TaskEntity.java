package com.lavyoung.marketforge.domain.message.model.entity;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 本地消息任务。
 *
 * @param userId        用户标识，同时作为任务表分片键
 * @param topic         消息交换机
 * @param eventId       事件唯一标识
 * @param eventType     事件类型或路由键
 * @param messageBody   序列化后的事件内容
 * @param occurredAt    事件发生时间
 * @param state         任务投递状态
 * @param retryCount    已失败重试次数
 * @param nextRetryTime 下一次允许重试时间
 * @param lastError     最近一次投递失败原因
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Builder
public record TaskEntity(
        String userId,
        String topic,
        String eventId,
        String eventType,
        String messageBody,
        LocalDateTime occurredAt,
        String state,
        Integer retryCount,
        LocalDateTime nextRetryTime,
        String lastError
) {
}
