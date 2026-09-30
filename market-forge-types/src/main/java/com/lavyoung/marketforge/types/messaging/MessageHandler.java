package com.lavyoung.marketforge.types.messaging;

/**
 * 集成消息处理器端口。
 * <p>
 * 每一种集成事件由应用层提供对应处理器，消息适配器只负责反序列化、幂等和上下文构建，
 * 不承载具体业务语义。
 *
 * @param <T> 集成事件类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface MessageHandler<T extends IntegrationEvent> {

    /**
     * 处理一条已通过适配器解码的集成事件。
     *
     * @param event   集成事件
     * @param context 消息上下文
     * @throws RuntimeException 业务处理失败时抛出，使消息事务回滚并触发重试
     */
    void handle(T event, MessageContext context);
}
