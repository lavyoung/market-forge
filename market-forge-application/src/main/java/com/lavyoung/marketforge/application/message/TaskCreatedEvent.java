package com.lavyoung.marketforge.application.message;

/**
 * 本地消息任务创建事件。
 *
 * @param userId  用户标识，同时作为任务表分片键
 * @param eventId 消息事件唯一标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
public record TaskCreatedEvent(String userId, String eventId) {
}
