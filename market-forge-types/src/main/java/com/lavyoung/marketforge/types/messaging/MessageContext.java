package com.lavyoung.marketforge.types.messaging;

import java.util.Map;

/**
 * 消息消费上下文。
 * <p>
 * 描述 MQ 入站消息的传输层元数据，供应用处理器记录日志、做幂等诊断或读取消息头。
 *
 * @param messageId   消息唯一标识
 * @param traceId     链路追踪标识
 * @param redelivered 是否为 broker 重新投递的消息
 * @param headers     MQ 消息头
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public record MessageContext(
        String messageId,
        String traceId,
        boolean redelivered,
        Map<String, Object> headers
) {

}
